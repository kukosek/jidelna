from selenium import webdriver
from Work import jidelna_webapp_handler
from Work.daymenu import DayMenu
from Work.weekmenu import WeekMenu
from Work.jobs import Jobs
from datetime import datetime
from datetime import date
from datetime import timedelta

import threading
import cherrypy

import os
from dotenv import load_dotenv
load_dotenv()

class Worker:
    # Worker is a thing you can assign tasks to.
    # It consumes the information about what it should do from the class Job.
    # If you want to assign a task to the worker, you must supply a Job to the function do_job
    # It has a queue, so you can call it from more places parallel
    def __init__(self):
        try:
            headless = os.getenv('HEADLESS').lower() == "true"
        except Exception:
            print("No HEADLESS env variable, defaulting to true.")
            headless = True

        options = webdriver.firefox.options.Options()
        if headless:
            options.add_argument('-headless')
        self.browser = webdriver.Firefox(options=options)
        self.handler = jidelna_webapp_handler.JidelnaWebappHandler(self.browser)
        self.callQueue = []
        self.active = False
        self.loggedUser = None
        self.lastUsedTime = datetime.now()

    def __perform_queue_jobs(self):
        # Runs in another thread
        # Does all the taks (jobs) it has in the queue
        self.active = True
        while len(self.callQueue) != 0:
            job = self.callQueue.pop(0)  # removes first job from array and returns it

            def login_for_job():
                self.handler.login(job.user.username, job.user.password)
                self.loggedUser = job.user

            # do the job
            try:
                if job.type == Jobs.LOGIN:
                    login_for_job()
                    job.result = self.handler.get_user_name()
                elif job.type == Jobs.LOGOUT:
                    self.handler.logout()
                    self.loggedUser = None
                else:
                    last_request_elapsed_seconds = (datetime.now() - self.lastUsedTime).total_seconds()
                    if self.loggedUser is None or job.user.username != self.loggedUser.username or last_request_elapsed_seconds > 60.0:
                        login_for_job()
                    if job.type == Jobs.SELECT_DATE:
                        self.handler.select_date(job.arguments[0])
                    elif job.type == Jobs.ORDER_MENU:
                        self.handler.select_date(job.arguments[0])
                        self.handler.order_menu(job.arguments[1])
                    elif job.type == Jobs.CANCEL_ORDER:
                        self.handler.select_date(job.arguments[0])
                        self.handler.cancel_order(job.arguments[1])
                    elif job.type == Jobs.GET_DAYMENU:
                        desired_date = job.arguments[0]
                        self.handler.select_date(desired_date)
                        job.result = WeekMenu([DayMenu(desired_date, self.handler.get_menu())], 0)
                    elif job.type == Jobs.GET_MENU:
                        daymenus = []
                        day_menu_available = True
                        day_menu_not_available_times = 0
                        date_iter = date.today()
                        if date_iter.month == 7 or date_iter.month == 8:
                            date_iter = date(date_iter.year, 9, 1)
                        while day_menu_available:
                            self.handler.select_date(date_iter)
                            daymenu = self.handler.get_menu()
                            weekday = date_iter.weekday()
                            if len(daymenu) == 0:
                                if weekday != 5 and weekday != 6:
                                    day_menu_not_available_times += 1
                                    if day_menu_not_available_times > 1:
                                        day_menu_available = False
                            else:
                                day_menu_not_available_times = 0
                                daymenus.append(DayMenu(date_iter, daymenu))
                            date_iter += timedelta(days=1)
                        job.result = WeekMenu(daymenus, self.handler.get_credit())
            except Exception as e:
                job.result = e
                cherrypy.log.error(traceback=True)
            # callback - send a signal that the job is finished.
            job.evt.set()  # the .wait() in do_job ends now
        self.lastUsedTime = datetime.now()
        self.active = False
        return

    def do_job(self, job, *args):
        # add to queue
        if len(args) == 0:
            self.callQueue.append(job)
        else:
            self.callQueue.insert(args[0], job)
        # start thread calling queue jobs if its not running
        if not self.active:
            threading.Thread(target=self.__perform_queue_jobs).start()
        job.evt.wait()

        # Can be an exception
        return job.result
