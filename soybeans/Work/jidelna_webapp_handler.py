from datetime import datetime, date
import locale
from selenium.common.exceptions import StaleElementReferenceException


locale.setlocale(locale.LC_TIME, "cs_CZ.UTF-8")


class DayOrder:
    # A object about a day in the cantry
    # The menu list is stored in .menu attribute
    # You can order() or cancel_order() a dinner
    # You must initiate it when the browser (Selenium.webdriver) is already logged into the cantry's webapp
    # mDate is the date of the menu you want to initialize
    def __init__(self, mDate, browser):
        self.mDate = mDate
        self.browser = browser

        if "Jídelníček" not in self.browser.page_source:
            raise Exception("Day order request object created without logged in")

        # Select the date by interacting with the weird month/date selector on the webapp
        monthFinded = False
        displayedOrderDate = None

        def getDisplayedOrderDateElem():
            return browser.find_element_by_id("clnBillDate").find_elements_by_tag_name("tbody")[0].find_elements_by_tag_name(
                    "tr")[0].find_elements_by_tag_name("td")[0].find_elements_by_tag_name("table")[
                    0].find_elements_by_tag_name("tbody")[0].find_elements_by_tag_name("tr")[0].find_elements_by_tag_name(
                    "td")[1]
        def click_next_month():
            self.browser.find_element_by_xpath("//a[@title='Přejít na další měsíc']").click()
        def click_prev_month():
            self.browser.find_element_by_xpath("//a[@title='Přejít na předchozí měsíc']").click()
        while not monthFinded:
            try:
                displayedOrderDateElem = getDisplayedOrderDateElem()
            except StaleElementReferenceException:
                self.browser.get('http://5.104.18.31/jidelna/PersonDayPerOrderRequest.aspx')
                displayedOrderDateElem = getDisplayedOrderDateElem()
            czech_months = ["leden", "únor", "březen", "duben", "květen", "červen", "červenec", "srpen", "září",
                            "říjen", "listopad", "prosinec"]
            month, year = displayedOrderDateElem.text.split(' ')
            month = czech_months.index(month) + 1
            year = int(year)
            displayedOrderDate = date(year, month, 1)

            if displayedOrderDate.month == mDate.month:
                monthFinded = True
            else:
                if displayedOrderDate < mDate:
                    try:
                        click_next_month()
                    except StaleElementReferenceException:
                        self.browser.get('http://5.104.18.31/jidelna/PersonDayPerOrderRequest.aspx')
                        click_next_month()
                else:
                    try:
                        click_prev_month()
                    except StaleElementReferenceException:
                        self.browser.get('http://5.104.18.31/jidelna/PersonDayPerOrderRequest.aspx')
                        click_prev_month()
        def getDayClickable():
            return self.browser.find_element_by_xpath("//a[@title='" + datetime.strftime(mDate, "%d %B") + "']")
        try:
            getDayClickable().click()
        except StaleElementReferenceException:
            self.browser.get('http://5.104.18.31/jidelna/PersonDayPerOrderRequest.aspx')
            getDayClickable()

        def get_menu_table():
            return browser.find_element_by_id("dgBill").find_elements_by_tag_name("tbody")[0]
        try:
            menu_table_elem = get_menu_table()
        except StaleElementReferenceException:
            self.browser.get('http://5.104.18.31/jidelna/PersonDayPerOrderRequest.aspx')
            menu_table_elem = get_menu_table()
        self.menu = [] # This will be the list of dinners(dict)

        # For every menu(dinner) on the page, parse the allergens into a list of int,
        #   take the basic info about the dinner, determine the status by checking the image icon,
        #   create a dict with all this, and add to the dinner list (.menu)
        for menuElem in menu_table_elem.find_elements_by_tag_name("tr")[1:]:
            menuInfo = menuElem.find_elements_by_tag_name("td")
            allergens = menuInfo[6].text.split(',')
            removed = 0
            for a in range(len(allergens)):
                i = a - removed
                if allergens[i] == '':
                    removed += 1
                    allergens.pop(i)
                else:
                    allergens[i] = int(''.join(i for i in allergens[i].split('.')[0] if i.isdigit()))
            def append_dinner():
                self.menu.append({"type": menuInfo[3].text, "menuNumber": int(menuInfo[4].text), "name": menuInfo[5].text,
                              "allergens": allergens})
            try:
                append_dinner()
            except StaleElementReferenceException:
                self.browser.get('http://5.104.18.31/jidelna/PersonDayPerOrderRequest.aspx')
                append_dinner()
            statusImageName = menuInfo[2].find_elements_by_tag_name("input")[0].get_attribute("src")
            if statusImageName == "http://5.104.18.31/jidelna/image/objst_order.jpg":
                if mDate == date.today() and datetime.now() > datetime.now().replace(hour=14, minute=0, second=0):
                    self.menu[-1]["status"] = "ordered closed"
                else:
                    self.menu[-1]["status"] = "ordered"
            elif statusImageName == "http://5.104.18.31/jidelna/image/objst_order_request.jpg":
                self.menu[-1]["status"] = "ordering"
            elif statusImageName == "http://5.104.18.31/jidelna/image/objst_del_request.jpg":
                self.menu[-1]["status"] = "cancelling order"
            elif statusImageName == "http://5.104.18.31/jidelna/image/objst_no.jpg":
                if mDate == date.today() and datetime.now() > datetime.now().replace(hour=8, minute=0, second=0):
                    self.menu[-1]["status"] = "unavailable"
                else:
                    self.menu[-1]["status"] = "available"
            elif statusImageName == "http://5.104.18.31/jidelna/image/objst_inbourse.jpg":
                if mDate == date.today() and datetime.now() > datetime.now().replace(hour=14, minute=0, second=0):
                    self.menu[-1]["status"] = "unavailable"
                else:
                    self.menu[-1]["status"] = "available"
            else:
                self.menu[-1]["status"] = "none"

    def order(self, menuNumber):

        # Find the position (index) of dinner with specified menu number
        menuIndex = None
        for i in range(len(self.menu)):
            if int(self.menu[i]["menuNumber"]) == menuNumber:
                menuIndex = i
        if menuIndex == None:
            raise ValueError("Menu " + menuNumber + " not available")
        else:
            # Order the specified dinner
            self.browser.find_element_by_id("dgBill").find_elements_by_tag_name("tbody")[0].find_elements_by_tag_name(
                "tr")[menuIndex + 1].find_elements_by_tag_name("td")[0].find_elements_by_tag_name("input")[0].click()

    def cancel_order(self, menuNumber):

        # Find the position (index) of dinner with specified menu number
        menuIndex = None
        for i in range(len(self.menu)):
            if int(self.menu[i]["menuNumber"]) == menuNumber:
                menuIndex = i
        if menuIndex == None:
            raise ValueError("Menu " + menuNumber + " not available")
        else:
            # Cancel the specified dinner
            self.browser.find_element_by_id("dgBill").find_elements_by_tag_name("tbody")[0].find_elements_by_tag_name(
                "tr")[menuIndex + 1].find_elements_by_tag_name("td")[1].find_elements_by_tag_name("input")[0].click()


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
        self.browser.get('http://5.104.18.31/jidelna/jidelna.aspx')
        self.browser.find_element_by_id('txbName').send_keys(username)
        self.browser.find_element_by_id('txbPWD').send_keys(password)
        self.browser.find_element_by_id('btnLogin').click()

        if "Jídelníček" in self.browser.page_source:
            self.logged_in = True
        elif "Neplatné přihlášení" in self.browser.page_source or "Neplatné pøihlášení" in self.browser.page_source:
            self.logged_in = False
            raise ValueError("Incorrect credentials")
        else:
            self.logged_in = False
            raise Exception("Could not login")

    def logout(self):
        self.browser.find_element_by_id('imbLogOff').click()
        self.logged_in = False

    def select_date(self, mDate):
        self.dayorder = DayOrder(mDate, self.browser)

    def get_menu(self):
        return self.dayorder.menu

    def order_menu(self, menuNumber):
        self.dayorder.order(menuNumber)

    def cancel_order(self, menuNumber):
        self.dayorder.cancel_order(menuNumber)
