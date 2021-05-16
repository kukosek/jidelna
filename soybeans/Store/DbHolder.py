from dotenv import load_dotenv
load_dotenv()
import psycopg2
import os
from peewee import PostgresqlDatabase

db = PostgresqlDatabase(os.getenv('DB_NAME'),
        user=os.getenv('DB_USER'),
        password=os.getenv('DB_PASSWORD'),
        host=os.getenv('DB_HOST'),
        port=os.getenv('DB_PORT')
    )

class DbHolder:
    def __init__(self) -> None:
        db.connect()

    def get_cursor(self):
        # Try getting the cursor, if doesnt work try recreating the connection
        try:
            cursor = self.conn.cursor()
        except psycopg2.InterfaceError:
            #self.conn = get_conn()
            return self.conn.cursor()

        # Test if the cursor works, if doesnt try recreating the connection
        try:
            cursor.execute("SELECT 1")
        except psycopg2.OperationalError:
            self.conn = get_conn()
            cursor = self.conn.cursor()
        return cursor
