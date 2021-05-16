from peewee import FloatField, TextField
from Store.base_model import BaseModel
class SavedDinner(BaseModel):
    full_name = TextField()
    rating = FloatField()
