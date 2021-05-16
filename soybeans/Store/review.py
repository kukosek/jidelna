from peewee import DateTimeField, FloatField, TextField, ForeignKeyField
from Store.base_model import BaseModel
from Store.user import User
from Store.saved_dinner import SavedDinner
class Review(BaseModel):
    user = ForeignKeyField(User)
    dinner = ForeignKeyField(SavedDinner)
    date_posted = DateTimeField()
    rating = FloatField()
    message = TextField()
    score = FloatField()
