import cherrypy

from Work.job import Job
from Work.jobs import Jobs
from Automatic.dinner_ranker import DinnerRanker
from Automatic.dinner_to_order import DinnerToOrder

from datetime import date

import json
import random


class AutomaticOrderManager:
    def __init__(self, distributor, user_manager):
        self.user_manager = user_manager
        self.distributor = distributor

    # Automatically orders something for every user that has autoorder enabled and didn't already order something
    def do_automatic_orders(self):
        cherrypy.log("Starting autoorder")
        autoorder_users = self.user_manager.get_autoorder_users()
        num_of_users = len(autoorder_users)
        num_of_errors = 0

        if num_of_users > 0:
            for user in autoorder_users:
                if user.autoorder_cancellation_dates is None or date.today() not in user.autoorder_cancellation_dates:
                    menu = self.distributor.distribute(Job(Jobs.GET_DAYMENU, user, date.today()))
                    if isinstance(menu, Exception):
                        if "Bad credentials" in str(menu):
                            cherrypy.log.error("Credentials of user " + user.username + " are incorrect.")
                            continue
                        else:
                            cherrypy.log.error("Couldn't get today's menu for auto ordering. Error: " + str(menu))
                            break
                    if len(menu) == 0:
                        cherrypy.log("Autoorder: no menu for today")
                        break
                    dinner_already_ordered = False
                    for dinner in menu:
                        if dinner["status"] == "ordered" or dinner["status"] == "ordering":
                            dinner_already_ordered = True
                            break
                    if not dinner_already_ordered:
                        settings = user.autoorder_settings
                        menu_to_order = DinnerToOrder(None, date.today())
                        if "random" in settings and settings["random"]:
                            menu_to_order.number = random.randint(1, len(menu))
                        else:
                            menu_to_order.number = DinnerRanker(settings).get_best_dinner_number(menu)
                        if menu_to_order.number is not None:
                            result = self.distributor.distribute(
                                Job(Jobs.ORDER_MENU, user, menu_to_order.date, menu_to_order.number))
                            if isinstance(result, Exception):
                                cherrypy.log.error("Autoorder error, user " + user.username + ": " + str(result))
                                num_of_errors += 1
                else:
                    user.autoorder_cancellation_dates.pop(user.autoorder_cancellation_dates.indexOf(date.today()))
                    self.user_manager.add_or_update_user(user)
        cherrypy.log(
            "Autoorder finished, fulfilled " + str(num_of_users - num_of_errors) + "/" + str(num_of_users) + " requests")
