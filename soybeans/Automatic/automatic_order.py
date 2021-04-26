import cherrypy

from Work.job import Job
from Work.jobs import Jobs
from Work.exceptions import *
from Automatic.dinner_ranker import DinnerRanker
from Automatic.dinner_to_order import DinnerToOrder

from datetime import date, timedelta, datetime

import json
import random

result_skip_user = 1
result_stop_autoorder = 2
result_error = 3


class AutomaticOrderManager:
    def __init__(self, distributor, user_manager):
        self.user_manager = user_manager
        self.distributor = distributor

    def automatic_orders_for_user_and_day(self, user, desired_date):
        if user.autoorder_cancellation_dates is None or desired_date not in user.autoorder_cancellation_dates:
            menu = self.distributor.distribute(Job(Jobs.GET_DAYMENU, user, desired_date))
            if isinstance(menu, Exception):
                if "Bad credentials" in str(menu):
                    cherrypy.log.error("Credentials of user " + user.username + " are incorrect.")
                    return result_skip_user
                else:
                    cherrypy.log.error("Couldn't get today's menu for auto ordering. Error: " + str(menu))
                    return result_stop_autoorder
            menu = menu.menus
            if len(menu) == 0:
                cherrypy.log("Autoorder: no menu for today")
                return result_stop_autoorder
            dinner_already_ordered = False
            for dinner in menu:
                if dinner.status == "ordered" or dinner.status == "ordering":
                    dinner_already_ordered = True
                    break
            if not dinner_already_ordered:
                settings = user.autoorder_settings
                menu_to_order = DinnerToOrder(None, desired_date)
                menu_successfully_ordered = False
                while menu_successfully_ordered is False and len(menu) > 0:
                    if "random" in settings and settings["random"]:
                        menu_to_order.number = random.randint(1, len(menu))
                    else:
                        menu_to_order.number = DinnerRanker(settings).get_best_dinner_number(menu)
                    if menu_to_order.number is not None:
                        result = self.distributor.distribute(
                            Job(Jobs.ORDER_MENU, user, menu_to_order.date, menu_to_order.number))
                        if isinstance(result, DinnerOrderingClosedException):
                            # Can't order this dinner, try to order some other one

                            # Remove the unavailable dinner from our menu
                            for dinner in menu:
                                if dinner.menu_number == menu_to_order.number:
                                    menu.remove(dinner)
                            # skip to next loop
                        elif isinstance(result, Exception):
                            cherrypy.log.error("Autoorder error, user " + user.username + ": " + str(result))
                            return result_error
                        else:
                            menu_successfully_ordered = True
                    else:
                        menu_successfully_ordered = True
        else:
            user.autoorder_cancellation_dates.pop(user.autoorder_cancellation_dates.indexOf(desired_date))
            self.user_manager.add_or_update_user(user)

    # Automatically orders something for every user that has autoorder enabled and didn't already order something
    def do_automatic_orders(self):
        cherrypy.log("Starting autoorder")
        autoorder_users = self.user_manager.get_autoorder_users()
        num_of_users = len(autoorder_users)
        num_of_errors = 0

        if num_of_users > 0:
            for user in autoorder_users:
                if "orderAll" in user.autoorder_request_settings and user.autoorder_request_settings["orderAll"]:
                    menus = self.distributor.distribute(Job(Jobs.GET_MENU, user))
                    if isinstance(menus, Exception):
                        if "Bad credentials" in str(menus):
                            cherrypy.log.error("Credentials of user " + user.username + " are incorrect.")
                            num_of_errors += 1
                        else:
                            cherrypy.log.error("Couldn't get today's menu for auto ordering. Error: " + str(menus))
                            num_of_errors += 1
                            break
                    else:
                        for menu in menus:
                            result = self.automatic_orders_for_user_and_day(user, menu.date)
                            if result == result_stop_autoorder:
                                break
                            elif result == result_skip_user:
                                continue
                            elif result == result_error:
                                num_of_errors += 1
                else:
                    add_days = 0
                    if "orderDaysInAdvance" in user.autoorder_request_settings:
                        try:
                            add_days = int(user.autoorder_request_settings["orderDaysInAdvance"])
                        except ValueError:
                            add_days = 0
                    result = self.automatic_orders_for_user_and_day(user, date.today() + timedelta(days=add_days))
                    if result == result_stop_autoorder:
                        break
                    elif result == result_skip_user:
                        continue
                    elif result == result_error:
                        num_of_errors += 1
        cherrypy.log(
            "Autoorder finished, fulfilled " + str(num_of_users - num_of_errors) + "/" + str(
                num_of_users) + " requests")
