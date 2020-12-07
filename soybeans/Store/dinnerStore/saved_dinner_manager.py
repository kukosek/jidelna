from Store.dinnerStore.saved_dinner import SavedDinner


def db_row_to_saved_dinner(row):
    dinner_id, full_name, main_component_name, side_dish_name, serving_history_name, rating, reviews, image_ids = row
    return SavedDinner(
        dinner_id, full_name, main_component_name, side_dish_name, serving_history_name, rating, reviews, image_ids
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
                serving_history_dates date[],
                rating float,
                reviews varchar[],
                image_ids varchar[]
            );""")
        conn.commit()

    def get_saved_dinner_by_orderable_dinner(self, orderable_dinner):
        self.cur.execute(
            "SELECT * FROM " + self.table_name + " WHERE full_name = %(full_name)s",
            {"full_name": orderable_dinner.full_name}
        )
        rows = self.cur.fetchall()
        if len(rows) == 0:
            return None
        else:
            return db_row_to_saved_dinner(rows[0])

    def update_saved_dinner(self, saved_dinner):
        self.cur.execute(
            "SELECT * FROM " + self.table_name + " WHERE id = %(id)s",
            {"id": saved_dinner.id}
        )
        rows = self.cur.fetchall()
        if len(rows) == 0:
            raise ValueError("This saved dinner doesnt exist. Cant update")
        else:
            return db_row_to_saved_dinner(rows[0])