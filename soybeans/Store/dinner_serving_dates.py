from peewee import DateField, ForeignKeyField
from Store.base_model import BaseModel
from Store.saved_dinner import SavedDinner

class DinnerServingDates(BaseModel):
    dinner = ForeignKeyField(SavedDinner)
    serving_date = DateField()
