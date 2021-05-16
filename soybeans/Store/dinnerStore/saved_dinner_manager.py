from Store.dinnerStore.saved_dinner import SavedDinner
from Work.orderable_dinner import OrderableDinner


def db_row_to_saved_dinner(row):
    dinner_id, full_name, main_component_name, side_dish_name, rating = row
    return SavedDinner(
        dinner_id, full_name, main_component_name, side_dish_name,  rating
    )


class SavedDinnerManager:
    # Class for communicating with database storage of users
    def __init__(self, cur, conn):
        self.conn = conn
        self.cur = cur
        self.table_name = "dinners"
        cur.execute(
            """CREATE TABLE IF NOT EXISTS """ + self.table_name + """
            (
                id serial PRIMARY KEY,
                full_name varchar,
                main_component_name varchar,
                side_dish_name varchar,
                rating float,
            );""")
        conn.commit()

    def get_saved_dinner_by_orderable_dinner(self, orderable_dinner: OrderableDinner):
        self.cur.execute(
            "SELECT * FROM " + self.table_name + " WHERE full_name = %(full_name)s",
            {"full_name": orderable_dinner.name}
        )
        rows = self.cur.fetchall()
        if len(rows) == 0:
            self.cur.execute(
                "INSERT INTO " + self.table_name + " VALUES(default, %(full_name)s, "", "", 0.0))",
                    {"full_name": orderable_dinner.name}
                )
            self.conn.commit()
        else:
            return db_row_to_saved_dinner(rows[0])

    def update_saved_dinner(self, saved_dinner: SavedDinner):
        self.cur.execute(
            "SELECT * FROM " + self.table_name + " WHERE full_name = %(full_name)s",
            {"full_name": saved_dinner.full_name}
        )
        rows = self.cur.fetchall()
        if len(rows) == 0:
            raise ValueError("This saved dinner doesnt exist. Cant update")
        else:
            return db_row_to_saved_dinner(rows[0])
