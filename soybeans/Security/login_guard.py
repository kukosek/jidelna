from datetime import datetime


class LoginGuard:
    # Class to detect login brute force attempts by checking how many times has a parteticular ip failed at login.

    def __init__(self, conn):
        self.max_attempts = 10
        self.ban_expire_days = 1
        self.conn = conn
        cur = self.conn.cursor()
        self.table_name = "login_attempts_by_ip"
        self.bad_attempt_timestamp_format = "%Y-%m-%d %H:%M:%S"
        cur.execute(
            """CREATE TABLE IF NOT EXISTS """ + self.table_name + """
                    (
                        id serial PRIMARY KEY,
                        ip varchar,
                        attempts int,
                        last_attempt timestamp,
                        tried_usernames varchar[]
                    );""")
        conn.commit()

    def get_timestamp(self):
        return datetime.now().strftime(self.bad_attempt_timestamp_format)

    def get_id_by_ip(self, ip):
        cur = self.conn.cursor()
        cur.execute(
            """SELECT id from """ + self.table_name + """ WHERE ip = %(ip)s;""",
            {"ip": ip})
        rows = cur.fetchall()
        if len(rows) == 0:
            return None
        else:
            return rows[0][0]

    # Function notify login failed(ip) - increments number of bad attempts in the database for the IP
    def notify_login_failed(self, ip):
        cur = self.conn.cursor()
        row_id = self.get_id_by_ip(ip)
        if row_id is None:
            # This is the ip's first bad attempt to login
            # Add a record to the db

            cur.execute(
                """INSERT INTO """ + self.table_name + """ VALUES (DEFAULT, %(ip)s, 1, %(timestamp)s);""",
                {"ip": ip,
                 "timestamp": self.get_timestamp()})
        else:
            # The ip already had a bad attempt to login
            # increment the num of attempts in the db
            cur.execute(
                """UPDATE """ + self.table_name + """ SET attempts = attempts + 1, last_attempt = %(timestamp)s 
                WHERE id = %(id)s;""",
                {"id": row_id,
                 "timestamp": self.get_timestamp()})
        self.conn.commit()

    # function notify login success(ip) - if ip previously failed to auth, we will delete its failed records (make it
    # clean)
    def notify_login_success(self, ip):
        cur = self.conn.cursor()
        row_id = self.get_id_by_ip(ip)
        if row_id is not None:
            cur.execute(
                """DELETE from """ + self.table_name + """ WHERE id = %(id)s;""",
                {"id": row_id})
            self.conn.commit()

    # function is ip mailicious - returns true if the number of bad attempts in the DB
    def is_ip_malicious(self, ip):
        cur = self.conn.cursor()
        row_id = self.get_id_by_ip(ip)
        if row_id is not None:
            cur.execute("""SELECT attempts, last_attempt from """ + self.table_name + """ WHERE id = %(id)s;""",
                             {"id": row_id})
            attempts, last_attempt_datetime = cur.fetchone()
            attempt_delta = datetime.now() - last_attempt_datetime
            if attempt_delta.days >= self.ban_expire_days:
                self.notify_login_success(ip)
                return False
            else:
                return attempts >= self.max_attempts
        else:
            return False
