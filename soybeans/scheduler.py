import time
from playhouse.pool import PooledPostgresqlExtDatabase
import schedule
import os
from peewee import PostgresqlDatabase
from Store.get_db import get_db
from Work.browser_work_distributor import BrowserWorkDistributor
from Automatic.automatic_order import AutomaticOrderManager

try:
    num_of_workers = os.getenv('NUM_OF_WORKERS')
    if num_of_workers == None:
        raise Exception
    else:
        num_of_workers = int(num_of_workers)
except Exception:
    print("No NUM_OF_WORKERS env variable, defaulting to 1.")
    num_of_workers = 1
distributor = BrowserWorkDistributor(num_of_workers)

db: PooledPostgresqlExtDatabase = get_db()
db.connect()

autoorder_manager = AutomaticOrderManager(distributor, db)
schedule.every().day.at("04:19").do(autoorder_manager.do_automatic_orders)
autoorder_manager.do_automatic_orders()

while True:
    schedule.run_pending()
    time.sleep(1)
