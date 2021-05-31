from peewee import DoesNotExist
from Store.login_attempts import LoginAttempts
from datetime import datetime, timedelta


max_attempts = 10
ban_expire_days = 1
try:
    with open("Security/trustworthy_ips.txt", 'r') as file:
        ip_whitelist = file.readlines()
except ValueError:
    print("warning couldnt open trustworthy ip list")
    ip_whitelist = []


# Function notify login failed(ip) - increments number of bad attempts in the database for the IP
def notify_login_failed(ip: str):
    if ip not in ip_whitelist:
        try:
            attempts: LoginAttempts = LoginAttempts.get(LoginAttempts.ip == ip)
            attempts.attempts += 1
            attempts.last_attempt = datetime.now()
        except DoesNotExist:
            # This is the ip's first bad attempt to login
            # Add a record to the db
            attempts: LoginAttempts = LoginAttempts(ip=ip, attempts=1, last_attempt=datetime.now())
        attempts.save()

# function notify login success(ip) - if ip previously failed to auth, we will delete its failed records (make it
# clean)
def notify_login_success(ip):
    try:
        attempts: LoginAttempts = LoginAttempts.get(LoginAttempts.ip == ip)
        attempts.delete_instance()
    except DoesNotExist:
        pass

# function is ip mailicious - returns true if the number of bad attempts in the DB
def is_ip_malicious(ip):
    try:
        attempts: LoginAttempts = LoginAttempts.get(LoginAttempts.ip == ip)
        attempt_delta: timedelta = datetime.now() - attempts.last_attempt
        if attempt_delta.days >= ban_expire_days:
            notify_login_success(ip)
            return False
        else:
            return attempts.attempts >= max_attempts
    except DoesNotExist:
        return False
