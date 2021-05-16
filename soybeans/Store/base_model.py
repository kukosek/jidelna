from peewee import Model
from Store.get_db import get_db
db = get_db()
class BaseModel(Model):
    class Meta:
        database = db
