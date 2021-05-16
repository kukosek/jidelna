from peewee import DateField, ForeignKeyField
from Store.base_model import BaseModel
from Store.user import User
class AutoorderCancelDate(BaseModel):
    user = ForeignKeyField(User)
    cancel_date = DateField()
