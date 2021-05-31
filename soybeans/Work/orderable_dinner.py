from Store.review import Review
import json
from typing import List


class OrderableDinner:
    def __init__(self, dinner_type, menu_number, name, allergens, status):
        self.type: str = dinner_type
        self.menu_number: int = menu_number
        self.name: str = name
        self.allergens: List[int] = allergens
        self.status: str = status
        self.dinner_id: int = 0
        self.num_of_reviews: int = 0
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
            "status": self.status,
            "dinnerid": self.dinner_id,
            "numOfReviews": self.num_of_reviews
        }

    def from_string(self, json_string):
        self.from_dict(json.loads(json_string))

    def to_string(self):
        return json.dumps(self.to_dict())
