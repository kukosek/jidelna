import json
import datetime
from Work.orderable_dinner import OrderableDinner
from Work.daymenu import DayMenu
from typing import List


class WeekMenu:
    def __init__(self, daymenus: List[DayMenu], credit_left: float):
        self.daymenus: List[DayMenu]= daymenus
        self.credit_left: float = credit_left

    def to_dict(self):
        daymenu_array = []
        for daymenu in self.daymenus:
            daymenu_array.append(daymenu.to_dict())
        return {
            "daymenus": daymenu_array,
            "creditLeft": self.credit_left
        }

    def to_string(self):
        daymenu_array = []
        for daymenu in self.daymenus:
            daymenu_dict = daymenu.to_dict()
            daymenu_dict["date"] = daymenu_dict["date"].isoformat()
            daymenu_array.append(daymenu_dict)
        return json.dumps({
            "daymenus": daymenu_array,
            "creditLeft": self.credit_left
        })

