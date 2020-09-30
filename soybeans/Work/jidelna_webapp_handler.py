from datetime import datetime, date
import locale
locale.setlocale(locale.LC_TIME, "cs_CZ.UTF-8")

class DayOrder:
    def __init__(self, mDate, browser):
        self.mDate = mDate
        self.browser = browser
        if "Jídelníček" not in self.browser.page_source:
            raise Exception("Day order request object created without logged in")
        
        monthFinded = False
        displayedOrderDate = None
        while not monthFinded:
            displayedOrderDateElem = browser.find_element_by_id("clnBillDate").find_elements_by_tag_name("tbody")[0].find_elements_by_tag_name("tr")[0].find_elements_by_tag_name("td")[0].find_elements_by_tag_name("table")[0].find_elements_by_tag_name("tbody")[0].find_elements_by_tag_name("tr")[0].find_elements_by_tag_name("td")[1]
            czech_months = ["leden", "únor", "březen", "duben", "květen", "červen", "červenec", "srpen", "září", "říjen", "listopad", "prosinec"]
            month, year = displayedOrderDateElem.text.split(' ')
            month = czech_months.index(month)+1
            year = int(year)
            displayedOrderDate = date(year, month, 1)
            
            if displayedOrderDate.month == mDate.month:
                monthFinded = True
            else:
                if displayedOrderDate < mDate:
                    self.browser.find_element_by_xpath("//a[@title='Přejít na další měsíc']").click()
                else:
                    self.browser.find_element_by_xpath("//a[@title='Přejít na předchozí měsíc']").click()
        self.browser.find_element_by_xpath("//a[@title='"+datetime.strftime(mDate, "%d %B")+"']").click()
        menu_table_elem = browser.find_element_by_id("dgBill").find_elements_by_tag_name("tbody")[0]
        self.menu = []
        for menuElem in menu_table_elem.find_elements_by_tag_name("tr")[1:]:
            menuInfo = menuElem.find_elements_by_tag_name("td")
            allergens = menuInfo[6].text.split(',')
            removed = 0
            for a in range(len(allergens)):
                i=a-removed
                if allergens[i] == '':
                    removed+= 1
                    allergens.pop(i)
                else:
                    allergens[i] = int(''.join(i for i in allergens[i].split('.')[0] if i.isdigit()))
            self.menu.append({"type":menuInfo[3].text, "menuNumber":int(menuInfo[4].text), "name":menuInfo[5].text, "allergens":allergens})
            statusImageName = menuInfo[2].find_elements_by_tag_name("input")[0].get_attribute("src")
            if statusImageName == "http://5.104.18.31/jidelna/image/objst_order.jpg":
                self.menu[-1]["status"] = "ordered"
            elif statusImageName == "http://5.104.18.31/jidelna/image/objst_order_request.jpg":
                self.menu[-1]["status"] = "ordering"
            elif statusImageName == "http://5.104.18.31/jidelna/image/objst_del_request.jpg":
                self.menu[-1]["status"] = "cancelling order"
            elif statusImageName == "http://5.104.18.31/jidelna/image/objst_no.jpg":
                self.menu[-1]["status"] = "available"
            else:
                self.menu[-1]["status"] = "none"
    def order(self, menuNumber):
        menuIndex = None
        for i in range(len(self.menu)):
            if int(self.menu[i]["menuNumber"]) == menuNumber:
                menuIndex = i
        if menuIndex == None:
            raise ValueError("Menu "+menuNumber+" not available")
        else:
            self.browser.find_element_by_id("dgBill").find_elements_by_tag_name("tbody")[0].find_elements_by_tag_name("tr")[menuIndex+1].find_elements_by_tag_name("td")[0].find_elements_by_tag_name("input")[0].click()
    def cancel_order(self, menuNumber):
        menuIndex = None
        for i in range(len(self.menu)):
            if int(self.menu[i]["menuNumber"]) == menuNumber:
                menuIndex = i
        if menuIndex == None:
            raise ValueError("Menu "+menuNumber+" not available")
        else:
            self.browser.find_element_by_id("dgBill").find_elements_by_tag_name("tbody")[0].find_elements_by_tag_name("tr")[menuIndex+1].find_elements_by_tag_name("td")[1].find_elements_by_tag_name("input")[0].click()
class Jidelna_webapp_handler:
    def __init__(self, browser):
        self.browser = browser
        self.logged_in = False
        self.dayorder = None
    def login(self, username, password):
        if self.logged_in:
            try:
                self.logout()
            except Exception: pass
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
