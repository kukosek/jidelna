import os
from playhouse.pool import PooledPostgresqlExtDatabase

def get_db() -> PooledPostgresqlExtDatabase:
    return PooledPostgresqlExtDatabase(
        os.getenv('DB_NAME'),

        max_connections=8,
        stale_timeout=5,
        timeout=5,

        user=os.getenv('DB_USER'),
        password=os.getenv('DB_PASSWORD'),
        host=os.getenv('DB_HOST'),
        port=os.getenv('DB_PORT')
    )

