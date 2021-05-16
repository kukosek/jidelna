from peewee import Model
from Store.DbHolder import db
class BaseModel(Model):
    class Meta:
        database = db
