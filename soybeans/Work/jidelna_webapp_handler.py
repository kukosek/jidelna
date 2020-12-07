from datetime import datetime, date
import locale
from selenium.common.exceptions import StaleElementReferenceException
from Work.exceptions import *
from Work.orderable_dinner import OrderableDinner

locale.setlocale(locale.LC_TIME, "cs_CZ.UTF-8")

jidelna_webroot = 'http://5.104.18.31/jidelna/'


class DayOrder:
    # A object about a day in the cantry
    # The menu list is stored in .menu attribute
    # You can order() or cancel_order() a dinner
    # You must initiate it when the browser (Selenium.webdriver) is already logged into the cantry's webapp
    # mDate is the date of the menu you want to initialize
    def __init__(self, desired_date, browser):
        self.mDate = desired_date
        self.browser = browser

        if "Jídelníček" not in self.browser.page_source:
            raise BrowserNotLoggedInException("Day order request object created without logged in")

        # Select the date by interacting with the weird month/date selector on the webapp
        month_finded = False
        displayed_order_date = None

        def get_displayed_order_date_elem():
            return \
                browser.find_element_by_id("clnBillDate").find_elements_by_tag_name("tbody")[
                    0].find_elements_by_tag_name(
                    "tr")[0].find_elements_by_tag_name("td")[0].find_elements_by_tag_name("table")[
                    0].find_elements_by_tag_name("tbody")[0].find_elements_by_tag_name("tr")[
                    0].find_elements_by_tag_name(
                    "td")[1]

        def click_next_month():
            self.browser.find_element_by_xpath("//a[@title='Přejít na další měsíc']").click()

        def click_prev_month():
            self.browser.find_element_by_xpath("//a[@title='Přejít na předchozí měsíc']").click()

        while not month_finded:
            try:
                displayed_order_date_elem = get_displayed_order_date_elem()
            except StaleElementReferenceException:
                self.browser.get('%sPersonDayPerOrderRequest.aspx' % jidelna_webroot)
                displayed_order_date_elem = get_displayed_order_date_elem()
            czech_months = ["leden", "únor", "březen", "duben", "květen", "červen", "červenec", "srpen", "září",
                            "říjen", "listopad", "prosinec"]
            month, year = displayed_order_date_elem.text.split(' ')
            month = czech_months.index(month) + 1
            year = int(year)
            displayed_order_date = date(year, month, 1)

            if displayed_order_date.month == desired_date.month:
                month_finded = True
            else:
                if displayed_order_date < desired_date:
                    try:
                        click_next_month()
                    except StaleElementReferenceException:
                        self.browser.get('%sPersonDayPerOrderRequest.aspx' % jidelna_webroot)
                        click_next_month()
                else:
                    try:
                        click_prev_month()
                    except StaleElementReferenceException:
                        self.browser.get('%sPersonDayPerOrderRequest.aspx' % jidelna_webroot)
                        click_prev_month()

        def get_day_clickable():
            return self.browser.find_element_by_xpath("//a[@title='" + datetime.strftime(desired_date, "%d %B") + "']")

        try:
            get_day_clickable().click()
        except StaleElementReferenceException:
            self.browser.get('%sPersonDayPerOrderRequest.aspx' % jidelna_webroot)
            get_day_clickable()

        def get_menu_table():
            return browser.find_element_by_id("dgBill").find_elements_by_tag_name("tbody")[0]

        try:
            menu_table_elem = get_menu_table()
        except StaleElementReferenceException:
            self.browser.get('%sPersonDayPerOrderRequest.aspx' % jidelna_webroot)
            menu_table_elem = get_menu_table()
        self.menu = []  # This will be the list of dinners(dict)

        # For every menu(dinner) on the page, parse the allergens into a list of int,
        #   take the basic info about the dinner, determine the status by checking the image icon,
        #   create a dict with all this, and add to the dinner list (.menu)
        for menuElem in menu_table_elem.find_elements_by_tag_name("tr")[1:]:
            menu_info = menuElem.find_elements_by_tag_name("td")
            allergens = menu_info[6].text.split(',')
            removed = 0
            for a in range(len(allergens)):
                i = a - removed
                if allergens[i] == '':
                    removed += 1
                    allergens.pop(i)
                else:
                    allergens[i] = int(''.join(i for i in allergens[i].split('.')[0] if i.isdigit()))

            def append_dinner():
                self.menu.append(
                    OrderableDinner(
                        dinner_type=menu_info[3].text,
                        menu_number= int(menu_info[4].text),
                        name=menu_info[5].text,
                        allergens=allergens
                    )
                )


            try:
                append_dinner()
            except StaleElementReferenceException:
                self.browser.get('%sPersonDayPerOrderRequest.aspx' % jidelna_webroot)
                append_dinner()
            status_image_name = menu_info[2].find_elements_by_tag_name("input")[0].get_attribute("src")
            if status_image_name == ("%simage/objst_order.jpg" % jidelna_webroot):
                if desired_date == date.today() and datetime.now() > datetime.now().replace(hour=14, minute=0,
                                                                                            second=0):
                    self.menu[-1].status = "ordered closed"
                else:
                    self.menu[-1].status = "ordered"
            elif status_image_name == ("%simage/objst_order_request.jpg" % jidelna_webroot):
                self.menu[-1].status = "ordering"
            elif status_image_name == ("%simage/objst_del_request.jpg" % jidelna_webroot):
                self.menu[-1].status = "cancelling order"
            elif status_image_name == ("%simage/objst_no.jpg" % jidelna_webroot):
                if desired_date == date.today() and datetime.now() > datetime.now().replace(hour=8, minute=0, second=0):
                    self.menu[-1].status = "unavailable"
                else:
                    self.menu[-1].status = "available"
            elif status_image_name == ("%simage/objst_inbourse.jpg" % jidelna_webroot):
                if desired_date == date.today() and datetime.now() > datetime.now().replace(hour=14, minute=0,
                                                                                            second=0):
                    self.menu[-1].status = "unavailable"
                else:
                    self.menu[-1].status = "available"
            else:
                self.menu[-1].status = "none"

    def order(self, menu_number):

        # Find the position (index) of dinner with specified menu number
        menu_index = None
        for i in range(len(self.menu)):
            if int(self.menu[i].menu_number) == menu_number:
                menu_index = i
        if menu_index is None:
            raise ValueError("Menu " + menu_number + " not available")
        else:
            # Order the specified dinner
            self.browser.find_element_by_id("dgBill").find_elements_by_tag_name("tbody")[0].find_elements_by_tag_name(
                "tr")[menu_index + 1].find_elements_by_tag_name("td")[0].find_elements_by_tag_name("input")[0].click()
            if "Uzavřeno" in self.browser.page_source:
                if "max" in self.browser.page_source:
                    raise DinnerOrderingClosedException("Dinner sold out")
                raise DinnerOrderingClosedException("Too late. Not accepting orders now")

    def cancel_order(self, menu_number):

        # Find the position (index) of dinner with specified menu number
        menu_index = None
        for i in range(len(self.menu)):
            if int(self.menu[i].menu_number) == menu_number:
                menu_index = i
        if menu_index is None:
            raise ValueError("Menu " + menu_number + " not available")
        else:
            # Cancel the specified dinner
            self.browser.find_element_by_id("dgBill").find_elements_by_tag_name("tbody")[0].find_elements_by_tag_name(
                "tr")[menu_index + 1].find_elements_by_tag_name("td")[1].find_elements_by_tag_name("input")[0].click()
            if "Uzavřeno" in self.browser.page_source:
                if "max" in self.browser.page_source:
                    raise DinnerOrderingClosedException("Dinner sold out")
                raise DinnerOrderingClosedException("Too late. Not accepting orders now")


class JidelnaWebappHandler:
    # A selenium handler to interact with the Cantry's webapp
    def __init__(self, browser):
        self.browser = browser
        self.logged_in = False
        self.dayorder = None

    def login(self, username, password):
        if self.logged_in:
            try:
                self.logout()
            except Exception:
                pass
        def login_basic():
            self.browser.get('%sjidelna.aspx' % jidelna_webroot)
            self.browser.find_element_by_id('txbName').send_keys(username)
            self.browser.find_element_by_id('txbPWD').send_keys(password)
            self.browser.find_element_by_id('btnLogin').click()
        try:
            login_basic()
        except StaleElementReferenceException:
            login_basic()
        if "Jídelníček" in self.browser.page_source:
            self.logged_in = True
        elif "Neplatné přihlášení" in self.browser.page_source or "Neplatné pøihlášení" in self.browser.page_source:
            self.logged_in = False
            raise IncorrectCredentialsException("Incorrect credentials")
        else:
            self.logged_in = False
            raise LoginException("Could not login")

    def logout(self):
        self.browser.find_element_by_id('imbLogOff').click()
        self.logged_in = False

    def select_date(self, desired_date):
        self.dayorder = DayOrder(desired_date, self.browser)

    def get_menu(self):
        return self.dayorder.menu

    def order_menu(self, menu_number):
        self.dayorder.order(menu_number)

    def cancel_order(self, menu_number):
        self.dayorder.cancel_order(menu_number)
