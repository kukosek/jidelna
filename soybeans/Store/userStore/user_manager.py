from typing import List

from Store.userStore.user import User
import json

from datetime import datetime

def db_row_to_user(row):
    user_id, username, password, authid, autoorder_enable, autoorder_settings, autoorder_request_settings, autoorder_cancellation_dates, register_datetime = row
    if autoorder_settings is None:
        autoorder_settings = {}
    else:
        autoorder_settings = json.loads(autoorder_settings)
    if autoorder_cancellation_dates is None:
        autoorder_cancellation_dates = []
    if autoorder_request_settings is None:
        autoorder_request_settings = {}
    else:
        autoorder_request_settings = json.loads(autoorder_request_settings)
    return User(user_id, username, password, authid, autoorder_enable, autoorder_settings, autoorder_request_settings, autoorder_cancellation_dates, register_datetime)


class UserManager:
    # Class for communicating with database storage of users
    def __init__(self, cur, conn):
        self.register_datetime_format = "%Y-%m-%d %H:%M:%S"
        self.conn = conn
        self.cur = cur
        self.table_name = "users"
        cur.execute(
            """CREATE TABLE IF NOT EXISTS """ + self.table_name + """
            (
                id serial PRIMARY KEY,
                username varchar,
                password varchar,
                authid varchar,
                autoorder_enable boolean,
                autoorder_settings varchar,
                autoorder_request_settings varchar,
                autoorder_cancellation_dates date[],
                register_datetime timestamp
            );""")
        conn.commit()

    def get_user_by_authid(self, authid: str) -> User or None:
        # Retrieves an user from database. Returns None if no such authid
        self.cur.execute(
            """SELECT * FROM users WHERE 
            authid = %(authid)s""",
            {'authid': authid})
        rows = self.cur.fetchall()
        if len(rows) > 0:
            user = db_row_to_user(rows[0])
            return user
        else:
            return None

    def get_user_by_username(self, username: str) -> User or None:
        # Retrieves an user from database. Returns None if no such authid
        self.cur.execute(
            """SELECT * FROM users WHERE 
            username = %(username)s""",
            {'username': username})
        rows = self.cur.fetchall()
        if len(rows) > 0:
            user = db_row_to_user(rows[0])
            return user
        else:
            return None

    def get_autoorder_users(self) -> List[User]:
        # Returns a list of users that have autoordering enabled
        self.cur.execute(
            "SELECT * FROM " + self.table_name + " WHERE autoorder_enable = true")
        autoorder_users = self.cur.fetchall()
        for i in range(len(autoorder_users)):
            autoorder_users[i] = db_row_to_user(autoorder_users[i])
        return autoorder_users

    def add_or_update_user(self, user: User):
        # Saves the specified user to DB

        # Check if user already exists
        self.cur.execute("""SELECT * FROM """ + self.table_name + """ WHERE username = %(username)s""", {
            'username': user.username})
        query = self.cur.fetchall()
        if len(query) > 0:  # If user exists
            # Update user record
            existing_user = db_row_to_user(query[0])
            self.cur.execute("""UPDATE """ + self.table_name + """ 
                                SET username = %(username)s,
                                    password = %(password)s,
                                    authid = %(authid)s,
                                    autoorder_enable = %(autoorder_enable)s,
                                    autoorder_settings = %(autoorder_settings)s,
                                    autoorder_request_settings = %(autoorder_request_settings)s,
                                    autoorder_cancellation_dates = %(autoorder_cancellation_dates)s,
                                    register_datetime = %(register_datetime)s
                                WHERE id = %(id)s
                                """, {
                "username": user.username,
                "password": user.password,
                "authid": user.authid,
                "autoorder_enable": user.autoorder_enable,
                "autoorder_settings": json.dumps(user.autoorder_settings),
                "autoorder_request_settings": json.dumps(user.autoorder_request_settings),
                "autoorder_cancellation_dates": user.autoorder_cancellation_dates,
                "id": existing_user.id,
                "register_datetime": user.register_datetime
            })
        else:  # No such user
            # Add new user
            self.cur.execute(
                """INSERT INTO """ + self.table_name + """ (id, username, password, authid, register_datetime) VALUES (DEFAULT, %(username)s, %(password)s, %(authid)s, %(register_datetime)s)""",
                {
                    "username": user.username,
                    "password": user.password,
                    "authid": user.authid,
                    "register_datetime": datetime.now().strftime(self.register_datetime_format)
                })
        self.conn.commit()

    def delete_user(self, user: User):
        self.cur.execute(
            "DELETE FROM " + self.table_name + " WHERE username = %(username)s",
            {"username": user.username})
        self.conn.commit()
