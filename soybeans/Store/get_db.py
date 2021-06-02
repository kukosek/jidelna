import os
from peewee import PostgresqlDatabase

def get_db() -> PostgresqlDatabase:
    return PostgresqlDatabase(
        os.getenv('DB_NAME'),
        autoconnect=True,
        autorollback=True,

        #max_connections=20,
        #stale_timeout=5,

        user=os.getenv('DB_USER'),
        password=os.getenv('DB_PASSWORD'),
        host=os.getenv('DB_HOST'),
        port=os.getenv('DB_PORT')
    )

