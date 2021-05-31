from peewee import TextField, IntegerField, DateTimeField
from Store.base_model import BaseModel

class LoginAttempts(BaseModel):
    ip = TextField()
    attempts = IntegerField()
    last_attempt = DateTimeField()
