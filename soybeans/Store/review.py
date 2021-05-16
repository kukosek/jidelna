from peewee import FloatField, TextField, ForeignKeyField
from Store.base_model import BaseModel
from Store.user import User
class Review(BaseModel):
    user = ForeignKeyField(User)
    rating = FloatField()
