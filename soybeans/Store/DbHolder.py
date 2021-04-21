from dotenv import load_dotenv
load_dotenv()
import psycopg2
import os

def get_conn():
    try:
        conn_string = os.getenv('DB_CONF')
    except Exception:
        print("Error reading DB_CONF env var")
        exit(1)
    return psycopg2.connect(conn_string)

class DbHolder:
    def __init__(self) -> None:
        self.conn = get_conn()

    def get_cursor(self):
        try:
            return self.conn.cursor()
        except psycopg2.InterfaceError:
            self.conn = get_conn()
            return self.conn.cursor()
