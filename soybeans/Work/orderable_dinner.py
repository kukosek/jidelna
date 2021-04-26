import json


class OrderableDinner:
    def __init__(self, dinner_type=None, menu_number=None, name=None, allergens=None, status=None):
        self.type = dinner_type
        self.menu_number = menu_number
        self.name = name
        self.allergens = allergens
        self.status = status

    def from_dict(self, dictionary):
        self.type = dictionary["type"]
        self.menu_number = dictionary["menuNumber"]
        self.name = dictionary["name"]
        self.allergens = dictionary["allergens"]
        self.status = dictionary["status"]

    def to_dict(self):
        return {
            "type": self.type,
            "menuNumber": self.menu_number,
            "name": self.name,
            "allergens": self.allergens,
            "status": self.status
        }

    def from_string(self, json_string):
        self.from_dict(json.loads(json_string))

    def to_string(self):
        return json.dumps(self.to_dict())