from typing import List

from Store.userStore.user import User
import json


def db_row_to_user(row):
    user_id, username, password, authid, autoorder_enable, autoorder_settings, autoorder_cancellation_dates = row
    return User(user_id, username, password, authid, autoorder_enable, autoorder_settings, autoorder_cancellation_dates)


class UserManager:
    # Class for communicating with database storage of users
    def __init__(self, cur, conn):
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
                autoorder_cancellation_dates date[]
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
            if user.autoorder_settings is None:
                user.autoorder_settings = {}
            else:
                user.autoorder_settings = json.loads(user.autoorder_settings)
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
                                    autoorder_cancellation_dates = %(autoorder_cancellation_dates)s
                                WHERE id = %(id)s
                                """, {
                "username": user.username,
                "password": user.password,
                "authid": user.authid,
                "autoorder_enable": user.autoorder_enable,
                "autoorder_settings": user.autoorder_settings,
                "autoorder_cancellation_dates": user.autoorder_cancellation_dates,
                "id": existing_user.id
            })
        else:  # No such user
            # Add new user
            self.cur.execute(
                """INSERT INTO """ + self.table_name + """ VALUES (DEFAULT, %(username)s, %(password)s, %(authid)s)""",
                {
                    "username": user.username,
                    "password": user.password,
                    "authid": user.authid
                })
        self.conn.commit()
