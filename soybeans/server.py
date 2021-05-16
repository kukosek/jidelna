import cherrypy
import cherrypy_cors

import threading

from Automatic.dinner_ranker import DinnerRanker
from Work.browser_work_distributor import BrowserWorkDistributor
from Work.job import Job
from Work.jobs import Jobs
from Work.exceptions import *

from Automatic.automatic_order import AutomaticOrderManager

from Store.get_db import get_db
from Store.user import User
from Store.saved_dinner import SavedDinner
from Store.login_attempts import LoginAttempts
from Store.review import Review
from Store.dinner_serving_dates import DinnerServingDates
from Store.autoorder_cancel_dates import AutoorderCancelDate

import Security.login_guard as login_guard

from datetime import datetime

import logging

import json
import random
import string

import time
import os
from dotenv import load_dotenv
load_dotenv()

import schedule


#RFC date for expiring cookies
from wsgiref.handlers import format_date_time
from datetime import datetime
from time import mktime

from peewee import DoesNotExist, PostgresqlDatabase

def CORS():
    cherrypy.response.headers["Access-Control-Allow-Credentials"] = "true"

if __name__ == '__main__':
    db: PostgresqlDatabase = get_db()
    db.connect()

    db.create_tables([User, LoginAttempts, SavedDinner, Review, AutoorderCancelDate])

    try:
        num_of_workers = os.getenv('NUM_OF_WORKERS')
        if num_of_workers == None:
            raise Exception
        else:
            num_of_workers = int(num_of_workers)
    except Exception:
        print("No NUM_OF_WORKERS env variable, defaulting to 1.")
        num_of_workers = 1
    distributor = BrowserWorkDistributor(num_of_workers)
    autoorder_manager = AutomaticOrderManager(distributor, db)
    schedule.every().day.at("06:19").do(autoorder_manager.do_automatic_orders)

def get_user_by_authid(authid) -> User:
    try:
        return User.get(User.authid == authid)
    except DoesNotExist:
        print('does not exist')
        cherrypy.response.cookie["authid"] = authid
        cherrypy.response.cookie["authid"]["expires"] = 0
        raise cherrypy.HTTPError(status=401, message="Bad authid")

class JidelnaSuperstructureServer(object):
    def get_request_origin_ip(self):
        if "X-ORIGINAL-FORWARDED-FOR" in cherrypy.request.headers:
            return cherrypy.request.headers["X-ORIGINAL-FORWARDED-FOR"]
        else:
            raddr = cherrypy.request.headers["Remote-Addr"]
            if "127.0.0.1" in raddr:
                if "X-Forwarded-For" in cherrypy.request.headers:
                    return cherrypy.request.headers["X-Forwarded-For"]
                else:
                    raise cherrypy.HTTPError(status=500, message="Could not identify your IP.")
            else:
                return raddr

    def get_request_params(self):
        try:
            return json.loads(cherrypy.request.body.read(int(cherrypy.request.headers['Content-Length'])))
        except ValueError:
            raise cherrypy.HTTPError(400)

    def login_exception_check(self, possible_exception):
        if isinstance(possible_exception, Exception):
            if "Could not login" in str(possible_exception):
                cherrypy.log.error("/login Login probe doesn't work, cant reach dayorder")
                raise cherrypy.HTTPError(status=502, message="Jidelna login probe doesn't work, cant reach dayorder.")
            elif "Incorrect credentials" in str(possible_exception):
                login_guard.notify_login_failed(self.get_request_origin_ip())
                raise cherrypy.HTTPError(status=401, message="Incorrect credentials")
            elif "Reached error page: about:neterror" in str(possible_exception):
                raise cherrypy.HTTPError(status=504, message="Jidelna server unreachable")
        else:
            login_guard.notify_login_success(self.get_request_origin_ip())

    def get_authid(self):
        try:
            return cherrypy.request.cookie["authid"].value
        except KeyError:
            raise cherrypy.HTTPError(status=401, message="Please authorize using authid cookie")

    @cherrypy.expose
    def index(self):
        return open("index.html", "r").read()

    @cherrypy.expose
    def login(self):
        if cherrypy.request.method == "POST":
            if not login_guard.is_ip_malicious(self.get_request_origin_ip()):
                if "authid" in cherrypy.request.cookie:
                    authid = self.get_authid()
                    user = get_user_by_authid(authid)

                    result = distributor.distribute(Job(Jobs.LOGIN, user))
                    if isinstance(result, Exception):
                        if "Could not login" in str(result):
                            cherrypy.log.error("/login Login probe doesn't work, cant reach dayorder")
                            raise cherrypy.HTTPError(status=502,
                                                     message="Jidelna login probe doesn't work, cant reach dayorder.")
                        elif "Reached error page: about:neterror" in str(result):
                            cherrypy.log.error("/login Reached error page: about:neterror")
                            raise cherrypy.HTTPError(status=504, message="Jidelna server unreachable")
                        else:
                            cherrypy.log.error("/login Unknown exception:\n" + str(result))
                            raise cherrypy.HTTPError(status=500)
                    else:
                        return "ok"
                request_params = self.get_request_params()
                if "username" not in request_params or "password" not in request_params:
                    raise cherrypy.HTTPError(status=400,
                                             message="Bad request: you must specify username and password in request body as json")
                user = User(
                        username=request_params["username"],
                        password=request_params["password"],
                        authid="",
                        autoorder_enable=False,
                        autoorder_settings="{}",
                        autoorder_request_settings="{}",
                        register_datetime=datetime.now()
                    )
                result = distributor.distribute(Job(Jobs.LOGIN, user))
                self.login_exception_check(result)

                try:
                    possibly_existing_user: User = User.get(User.username == request_params["username"])
                    user = possibly_existing_user
                    user.password = request_params["password"]
                except DoesNotExist:
                    authid = ''.join([random.choice(string.ascii_letters + string.digits) for n in range(128)])
                    user.authid = authid

                print(user.username)
                user.save()

                cherrypy.response.headers['Set-Cookie'] = 'authid='+str(user.authid)+'; SameSite=None; Secure'
                return "ok"
            else:
                raise cherrypy.HTTPError(status=403, message="you messed up. contact me")
        else:
            raise cherrypy.HTTPError(status=400)

    @cherrypy.expose
    def logout(self, **params):
        authid = self.get_authid()
        user = get_user_by_authid(authid)

        now = datetime.now()
        stamp = mktime(now.timetuple())
        expires = format_date_time(stamp) #--> Wed, 22 Oct 2008 10:52:40 GMT

        cherrypy.response.headers['Set-Cookie'] = 'authid='+str(user.authid)+'; SameSite=None; Secure; expires='+expires
        if "delete" in params:
            if params["delete"] == "true":
                user.delete_instance()
                return "logged_out_and_deleted"
        return "logged_out"

    @cherrypy.expose
    def menu(self, **params):
        authid = self.get_authid()
        user : User = get_user_by_authid(authid)
        print("menu: Jmeno:", user.username, "Heslo: ", user.password)
        print(user.username[0])

        if cherrypy.request.method == "GET":
            cherrypy.response.headers['Content-Type'] = 'application/json'
            desired_date = None
            if "date" in params:
                try:
                    desired_date = datetime.strptime(params["date"],
                                                     "%Y-%m-%d").date()
                except Exception:
                    raise cherrypy.HTTPError(status=400, message="Bad date format")
            if desired_date is None:
                daymenus = distributor.distribute(Job(Jobs.GET_MENU, user))
            else:
                daymenus = distributor.distribute(Job(Jobs.GET_DAYMENU, user, desired_date))
            if isinstance(daymenus, Exception):
                self.login_exception_check(daymenus)
                cherrypy.log.error("/menu unknown exception:" + str(daymenus))
                raise cherrypy.HTTPError(status=500)
            else:
                if user.autoorder_enable:
                    def daymenu_correction(daymenu_to_correct):
                        will_autoorder = True

                        try:
                            AutoorderCancelDate.get(user==user, cancel_date==daymenu_to_correct.date)
                            will_autoorder = False
                        except DoesNotExist:
                            pass
                        unavailable_menus = 0
                        for menu in daymenu_to_correct.menus:
                            if menu.status == "ordered" or menu.status == "ordering" or menu.status == "ordered closed":
                                will_autoorder = False
                            elif menu.status == "unavailable":
                                unavailable_menus += 1
                        if len(daymenu_to_correct.menus) == unavailable_menus:
                            will_autoorder = False
                        if will_autoorder:
                            menu_num_to_order = DinnerRanker(
                                user.autoorder_settings).get_best_dinner_number(
                                daymenu_to_correct.menus)
                            for menu in daymenu_to_correct.menus:
                                if menu.menu_number == menu_num_to_order:
                                    menu.status = "autoordered"

                    if isinstance(daymenus, list):
                        for daymenu in daymenus:
                            daymenu_correction(daymenu)
                    else:
                        daymenu_correction(daymenus)
                if desired_date is None:
                    daymenus_dict = daymenus
                    for i in range(len(daymenus_dict)):
                        daymenus_dict[i] = daymenus_dict[i].to_dict()
                        daymenus_dict[i]["date"] = daymenus_dict[i]["date"].isoformat()
                    return json.dumps(daymenus_dict).encode('utf8')
                else:
                    return daymenus.to_string().encode('utf8')
        elif cherrypy.request.method == "POST":
            request_params = self.get_request_params()
            try:
                action = request_params["action"]
                order_date = datetime.strptime(request_params["date"], "%Y-%m-%d").date()
                result = None
                if action == "order":
                    result = distributor.distribute(
                        Job(Jobs.ORDER_MENU, user, order_date, request_params["menuNumber"]))
                    try:
                        cancel_date = AutoorderCancelDate.get(AutoorderCancelDate.user==user,AutoorderCancelDate.cancel_date==order_date)
                        cancel_date.delete_instance()
                    except DoesNotExist:
                        pass

                elif action == "cancel":
                    menus_cancel_day = distributor.distribute(Job(Jobs.GET_DAYMENU, user, order_date))
                    if isinstance(menus_cancel_day, Exception):
                        self.login_exception_check(menus_cancel_day)
                        raise cherrypy.HTTPError(status=500)
                    menus_cancel_day = menus_cancel_day.menus
                    something_is_ordered = False
                    for dinner in menus_cancel_day:
                        if dinner.status != "available" and dinner.status != "cancelling":
                            something_is_ordered = True
                    if something_is_ordered:
                        result = distributor.distribute(
                            Job(Jobs.CANCEL_ORDER, user, order_date, request_params["menuNumber"]))
                    else:
                        try:
                            AutoorderCancelDate.get(AutoorderCancelDate.user == user, AutoorderCancelDate.cancel_date == order_date)
                        except DoesNotExist:
                            if user.autoorder_enable:
                                cancel_date = AutoorderCancelDate(AutoorderCancelDate.user==user, AutoorderCancelDate.cancel_date==order_date)
                                cancel_date.save()
                if isinstance(result, Exception):
                    self.login_exception_check(result)
                    if isinstance(result, ValueError):
                        raise cherrypy.HTTPError(status=400, message="Menu number " + str(
                            request_params["menuNumber"]) + " not available")
                    elif isinstance(result, DinnerOrderingClosedException):
                        raise cherrypy.HTTPError(status=404, message="Menu number " + str(
                            request_params["menuNumber"]) + " not available - ordering was closed.")
                    else:
                        cherrypy.log.error(
                            "/menu " + request_params["action"] + " request unknown exception:" + str(result))
                        raise cherrypy.HTTPError(status=500)
                else:
                    return "ok"
            except KeyError:
                raise cherrypy.HTTPError(status=400)

    @cherrypy.expose
    def settings(self):
        authid = self.get_authid()
        user = get_user_by_authid(authid)

        if cherrypy.request.method == "GET":
            cherrypy.response.headers['Content-Type'] = 'application/json'

            user_settings = {"autoorder": {"enable": user.autoorder_enable, "settings": user.autoorder_settings,
                                           "requestSettings": user.autoorder_request_settings}}
            return json.dumps(user_settings).encode('utf8')
        elif cherrypy.request.method == "POST":
            settings = self.get_request_params()
            try:
                for key, value in settings.items():
                    if key == "autoorder":
                        user.autoorder_enable = value["enable"]
                        user.autoorder_settings = value["settings"]
                        user.autoorder_request_settings = value["requestSettings"]
                        user.save()
                    else:
                        raise cherrypy.HTTPError(status=400, message="Unknown setting: " + key)
                return "ok"
            except KeyError as e:
                cherrypy.HTTPError(status=400, message=str(e))
            except ValueError as e:
                cherrypy.HTTPError(status=400, message=str(e))


class RunScheduler:
    def __init__(self):
        self.running = True

    def rs(self):
        while self.running:
            schedule.run_pending()
            time.sleep(1)


run_scheduler = RunScheduler()


class ThreadController(cherrypy.process.plugins.SimplePlugin):
    def start(self):
        scheduler_thread = threading.Thread(target=run_scheduler.rs)
        scheduler_thread.start()

    def stop(self):
        run_scheduler.running = False
        try:
            distributor.close_all()
        except Exception:
            pass


if __name__ == '__main__':
    def finish():
        db.close()
        logging.info("PostgreSQL connection is closed")
        try:
            distributor.close_all()  # test
        except Exception:
            pass


    thread_controller = ThreadController(cherrypy.engine)
    thread_controller.subscribe()

    config = {}
    try:
        config['/']= {'tools.CORS.on': True }
        config['tools.CORS.on'] = True
        config['cors.expose.on'] = True
        config['server.socket_host'] = os.getenv("HOST")
        config['server.socker_port'] = os.getenv("PORT")
        config['request.show_tracebacks'] = os.getenv("REQUEST_SHOW_ERRORS").lower() == "true"
        config['log.screen'] = True
    except Exception as e:
        print("Error while reading server config env variables")
        finish()
    try:
        cherrypy.config.update(config)
        cherrypy_cors.install()
        cherrypy.tools.CORS = cherrypy.Tool("before_finalize", CORS)
        cherrypy.quickstart(JidelnaSuperstructureServer())
    except Exception:
        finish()
    finish()
