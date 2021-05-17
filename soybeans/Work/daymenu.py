import json
import datetime
from Work.orderable_dinner import OrderableDinner


class DayMenu:
    def __init__(self, date=None, menus=None):
        self.date = date
        self.menus = menus

    def from_dict(self, dictionary):
        print("cs")
        if isinstance(dictionary["date"], datetime.date):
            self.date = dictionary["date"]
        else:
            self.date = datetime.datetime.strptime(dictionary["date"], "%Y-%m-%d")
        self.menus = []
        for menu in dictionary["menus"]:
            self.menus.append(OrderableDinner().from_dict(menu))

    def to_dict(self):
        menu_array = []
        for menu in self.menus:
            menu_array.append(menu.__dict__)
        return {
            "date": self.date,
            "menus": menu_array
        }

    def from_string(self, json_string):
        self.from_dict(json.loads(json_string))

    def to_string(self):
        daymenu_dict = self.to_dict()
        daymenu_dict["date"] = daymenu_dict["date"].isoformat()
        return json.dumps(daymenu_dict)
