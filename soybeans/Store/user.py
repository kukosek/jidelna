from peewee import BooleanField, DateTimeField, TextField
from Store.base_model import BaseModel

class User(BaseModel):
    password = TextField()
    username = TextField()
    authid = TextField()
    autoorder_enable = BooleanField()
    autoorder_settings = TextField()
    autoorder_request_settings = TextField()
    register_datetime = DateTimeField()
    class Meta:
        table_name = 'user_profile'
