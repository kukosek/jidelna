class SavedDinner:
    def __init__(self, dinner_id, full_name, main_component_name, side_dish_name, serving_history_dates,rating, reviews, image_ids):
        self.id = dinner_id
        self.full_name = full_name
        self.main_component_name = main_component_name
        self.side_dish_name = side_dish_name
        self.serving_history_dates = serving_history_dates
        self.rating = rating
        self.reviews = reviews
        self.image_ids = image_ids
