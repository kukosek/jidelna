from peewee import BooleanField, DateTimeField, Model, CharField, TextField
from Store.base_model import BaseModel

class User(BaseModel):
    username = CharField(unique=True),
    password = TextField()
    authid = TextField()
    autoorder_enable = BooleanField()
    autoorder_settings = TextField()
    autoorder_request_settings = TextField()
    autoorder_cancellation_dates = TextField()
    register_datetime = DateTimeField()
