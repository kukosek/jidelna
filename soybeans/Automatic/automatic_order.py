from Store.autoorder_cancel_dates import AutoorderCancelDate
import cherrypy
from typing import List
from peewee import DoesNotExist, PostgresqlDatabase
from Work.weekmenu import WeekMenu

from Work.job import Job
from Work.jobs import Jobs
from Work.exceptions import *
from Automatic.dinner_ranker import DinnerRanker
from Automatic.dinner_to_order import DinnerToOrder

from Store.user import User

from datetime import date, timedelta, datetime

import json
import random

result_skip_user = 1
result_stop_autoorder = 2
result_error = 3


class AutomaticOrderManager:
    def __init__(self, distributor, db: PostgresqlDatabase):
        self.db = db
        self.distributor = distributor

    def automatic_orders_for_user_and_day(self, user, desired_date):
        try:
            cancel_date : AutoorderCancelDate= AutoorderCancelDate.get(
                    AutoorderCancelDate.user == user,
                    AutoorderCancelDate.cancel_date == desired_date
                )
            cancel_date.delete_instance()

        except DoesNotExist:
            menu: WeekMenu = self.distributor.distribute(Job(Jobs.GET_DAYMENU, user, desired_date))
            if isinstance(menu, Exception):
                if "Bad credentials" in str(menu):
                    cherrypy.log.error("Credentials of user " + user.username + " are incorrect.")
                    return result_skip_user
                else:
                    cherrypy.log.error("Couldn't get today's menu for auto ordering. Error: " + str(menu))
                    return result_stop_autoorder
            today_menu = menu.daymenus[0].menus
            if len(today_menu) == 0:
                cherrypy.log("Autoorder: no menu for today")
                return result_stop_autoorder
            dinner_already_ordered = False
            for dinner in today_menu:
                if dinner.status == "ordered" or dinner.status == "ordering":
                    dinner_already_ordered = True
                    break
            if not dinner_already_ordered:
                settings = json.loads(user.autoorder_settings)
                menu_to_order = DinnerToOrder(-1, desired_date)
                menu_successfully_ordered = False

                while menu_successfully_ordered is False and len(today_menu) > 0:
                    if "random" in settings and settings["random"]:
                        menu_to_order.menuNumber = random.randint(1, len(today_menu))
                    else:
                        menu_to_order.menuNumber = DinnerRanker(settings).get_best_dinner_number(today_menu)

                    if menu_to_order.menuNumber != -1:
                        result = self.distributor.distribute(
                            Job(Jobs.ORDER_MENU, user, menu_to_order.date, menu_to_order.menuNumber))

                        if isinstance(result, DinnerOrderingClosedException):
                            # Can't order this dinner, try to order some other one

                            # Remove the unavailable dinner from our menu
                            for dinner in today_menu:
                                if dinner.menu_number == menu_to_order.menuNumber:
                                    today_menu.remove(dinner)
                            # skip to next loop
                        elif isinstance(result, Exception):
                            cherrypy.log.error("Autoorder error, user " + user.username + ": " + str(result))
                            return result_error
                        else:
                            menu_successfully_ordered = True
                    else:
                        menu_successfully_ordered = True


    # Automatically orders something for every user that has autoorder enabled and didn't already order something
    def do_automatic_orders(self):
        self.db.connect()
        cherrypy.log("Starting autoorder")
        autoorder_users: List[User] = User.select().where(User.autoorder_enable == True )
        num_of_users = len(autoorder_users)
        num_of_errors = 0

        if num_of_users > 0:
            for user in autoorder_users:
                user_au_req_settings = json.loads(user.autoorder_request_settings)
                if "orderAll" in user_au_req_settings and user_au_req_settings["orderAll"]:
                    menus: WeekMenu = self.distributor.distribute(Job(Jobs.GET_MENU, user))
                    if isinstance(menus, Exception):
                        if "Bad credentials" in str(menus):
                            cherrypy.log.error("Credentials of user " + user.username + " are incorrect.")
                            num_of_errors += 1
                        else:
                            cherrypy.log.error("Couldn't get today's menu for auto ordering. Error: " + str(menus))
                            num_of_errors += 1
                            break
                    else:
                        for menu in menus.daymenus:
                            result = self.automatic_orders_for_user_and_day(user, menu.date)
                            if result == result_stop_autoorder:
                                break
                            elif result == result_skip_user:
                                continue
                            elif result == result_error:
                                num_of_errors += 1
                else:
                    add_days = 0
                    if "orderDaysInAdvance" in user_au_req_settings:
                        try:
                            add_days = int(user_au_req_settings["orderDaysInAdvance"])
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
