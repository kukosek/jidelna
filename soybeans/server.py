from Work.weekmenu import WeekMenu
from Store.orderable_save_and_fill import orderable_save_and_complete
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
from Store.review import Review, ReviewScore, review_from_dict, review_score_from_dict
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

from typing import List

AUTHID_LENGTH = 128

def CORS():
    cherrypy.response.headers["Access-Control-Allow-Credentials"] = "true"

if __name__ == '__main__':
    db: PostgresqlDatabase = get_db()
    db.connect()

    db.create_tables([
        User,
        LoginAttempts,
        SavedDinner,
        DinnerServingDates,
        Review,
        ReviewScore,
        AutoorderCancelDate
    ])

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
                user_name = distributor.distribute(Job(Jobs.LOGIN, user))
                self.login_exception_check(result)

                try:
                    possibly_existing_user: User = User.get(User.username == request_params["username"])
                    user = possibly_existing_user
                    user.password = request_params["password"]
                except DoesNotExist:
                    authid = ''.join([random.choice(string.ascii_letters + string.digits) for _ in range(AUTHID_LENGTH)])
                    user.authid = authid
                    user_name

                user.save()

                secure_cookie = False

                cherrypy.response.headers['Set-Cookie'] = 'authid='+str(user.authid)
                if secure_cookie:
                    cherrypy.response.headers['Set-Cookie'] += '; SameSite=None; Secure'
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
                weekmenu: WeekMenu = distributor.distribute(Job(Jobs.GET_MENU, user))
            else:
                weekmenu: WeekMenu = distributor.distribute(Job(Jobs.GET_DAYMENU, user, desired_date))
            print(weekmenu)
            if isinstance(weekmenu, Exception):
                self.login_exception_check(weekmenu)
                cherrypy.log.error("/menu unknown exception:" + str(weekmenu))
                raise cherrypy.HTTPError(status=500)
            else:
                for daymenu in weekmenu.daymenus:
                    for menu in daymenu.menus:
                        menu = orderable_save_and_complete(menu, daymenu.date)


                    if user.autoorder_enable:
                        will_autoorder = True
                        try:
                            AutoorderCancelDate.get(
                                    AutoorderCancelDate.user==user,
                                    AutoorderCancelDate.cancel_date==daymenu.date)
                            will_autoorder = False
                        except DoesNotExist:
                            pass
                        unavailable_menus = 0
                        for menu in daymenu.menus:
                            if menu.status == "ordered" or menu.status == "ordering" or menu.status == "ordered closed":
                                will_autoorder = False
                            elif menu.status == "unavailable":
                                unavailable_menus += 1
                        if len(daymenu.menus) == unavailable_menus:
                            will_autoorder = False
                        if will_autoorder:
                            menu_num_to_order = DinnerRanker(
                                json.loads(user.autoorder_settings)).get_best_dinner_number(
                                daymenu.menus)
                            for menu in daymenu.menus:
                                if menu.menu_number == menu_num_to_order:
                                    menu.status = "autoordered"




                return weekmenu.to_string().encode('utf8')
        elif cherrypy.request.method == "POST":
            request_params = self.get_request_params()
            try:
                action = request_params["action"]
                order_date = datetime.strptime(request_params["date"], "%Y-%m-%d").date()
                result = None
                if action == "order":
                    print(request_params["menuNumber"])
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

            user_settings = {"autoorder": {
                "enable": user.autoorder_enable,
                "settings": json.loads(user.autoorder_settings),
                "requestSettings": json.loads(user.autoorder_request_settings)}}
            return json.dumps(user_settings).encode('utf8')
        elif cherrypy.request.method == "POST":
            settings = self.get_request_params()
            try:
                for key, value in settings.items():
                    if key == "autoorder":
                        user.autoorder_enable = value["enable"]
                        user.autoorder_settings = json.dumps(value["settings"])
                        user.autoorder_request_settings = json.dumps(value["requestSettings"])
                        user.save()
                    else:
                        raise cherrypy.HTTPError(status=400, message="Unknown setting: " + key)
                return "ok"
            except KeyError as e:
                cherrypy.HTTPError(status=400, message=str(e))
            except ValueError as e:
                cherrypy.HTTPError(status=400, message=str(e))

    def check_id_list(self, id_list):
        for rid in id_list:
            try:
                int(rid)
            except ValueError:
                raise cherrypy.HTTPError(400, "ID must be integer")



    @cherrypy.expose
    def reviews(self, **params):

        authid = self.get_authid()
        user : User = get_user_by_authid(authid)


        def get_dinnerids():
            try:
                dinnerids = params["dinnerid"]
                if not isinstance(dinnerids, list):
                    dinnerids = [dinnerids]
                self.check_id_list(dinnerids)
                return dinnerids
            except KeyError:
                raise cherrypy.HTTPError(400, "You must specify dinnerid url param for submitting review")
            except ValueError:
                raise cherrypy.HTTPError(400, "Couldn't parse dinnerid URL param")


        if cherrypy.request.method == "GET":

            dinnerids = get_dinnerids()
            self.check_id_list(dinnerids)
            cherrypy.response.headers['Content-Type'] = 'application/json'

            query = Review.select()
            if "me" in params:
                query = query.where(Review.dinner.in_(dinnerids), Review.user==user)
            else:
                query = query.where(Review.dinner.in_(dinnerids))

            reviews = []
            for review in query:
                reviews.append(review.to_dict(user))

            result_dict = {"reviews": reviews}
            return json.dumps(result_dict).encode('utf-8')
        elif cherrypy.request.method == "POST":

            request_params = self.get_request_params()

            if "score" in request_params:
                try:
                    reviewids = params["reviewid"]
                    if not isinstance(reviewids, list):
                        reviewids = [reviewids]
                except KeyError:
                    raise cherrypy.HTTPError(400, "You must specify dinnerid url param for submitting review")
                except ValueError:
                    raise cherrypy.HTTPError(400, "Couldn't parse dinnerid URL param")
                review_score_from_dict(request_params["score"], user, reviewids[0])

            if "review" in request_params:
                dinnerids = get_dinnerids()
                try:
                    saved_dinner = SavedDinner.get_by_id(dinnerids[0])
                except DoesNotExist:
                    raise cherrypy.HTTPError(400, "Dinner doesnt exist")

                review = review_from_dict(request_params["review"],
                        user,saved_dinner, datetime.today())
                try:
                    old_review: Review = Review.get(Review.user==review.user, Review.dinner==review.dinner)
                    old_review.rating = review.rating
                    old_review.date_posted = datetime.today()
                    old_review.score = 0
                    old_review.message = review.message
                    old_review.save()
                except DoesNotExist:
                    try:
                        serving_date = DinnerServingDates.select().where(
                                DinnerServingDates.dinner==review.dinner
                                ).order_by(DinnerServingDates.serving_date.desc()).get()
                    except DoesNotExist:
                        raise cherrypy.HTTPError(400, "This dinner hasn't been served")
                    weekmenu: WeekMenu = distributor.distribute(Job(Jobs.GET_DAYMENU, user, serving_date.serving_date))
                    if isinstance(weekmenu, Exception):
                        self.login_exception_check(weekmenu)
                        cherrypy.log.error("/review create unknown exception:" + str(weekmenu))
                        raise cherrypy.HTTPError(status=500)
                    daymenu = weekmenu.daymenus[0]
                    ordered_dinnerid: int = -1
                    for menu in daymenu.menus:
                        menu = orderable_save_and_complete(menu, daymenu.date)
                        if menu.status == 'ordered' or menu.status == 'ordered closed' or menu.status == 'ordering':
                            ordered_dinnerid = (menu.dinner_id)
                            print("ok")
                    print(ordered_dinnerid)
                    if ordered_dinnerid == -1:
                        raise cherrypy.HTTPError(400, "Please order the dinnner you want to rate")
                    elif not ordered_dinnerid in dinnerids:
                        raise cherrypy.HTTPError(400, "You have ordered an other dinner than the one you want to rate.")

                    review.save()
        return "ok".encode('utf-8')

def _db_connect():
    if not db.is_connection_usable():
        db.connect()

def _db_close():
    if not db.is_closed():
        db.close()


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
        _db_close()
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
        cherrypy.engine.subscribe('before_request', _db_connect)
        cherrypy.quickstart(JidelnaSuperstructureServer())
    except Exception:
        finish()
    finish()
