from Store.review import Review
from Store.dinner_serving_dates import DinnerServingDates
from peewee import DoesNotExist
from Store.saved_dinner import SavedDinner
from Work.orderable_dinner import OrderableDinner
from datetime import date


def orderable_save_and_complete(
        dinner: OrderableDinner,
        serving_date: date
    ) -> OrderableDinner:
    try:
        saved_dinner: SavedDinner = SavedDinner.get(SavedDinner.full_name == dinner.name)
    except DoesNotExist:
        saved_dinner: SavedDinner = SavedDinner.create(
            full_name=dinner.name,
            rating=0.0
                )
        saved_dinner.save()
    dinner.dinner_id = saved_dinner.id

    dinner.num_of_reviews = Review.select().where(Review.dinner == dinner.dinner_id).count()

    try:
        DinnerServingDates.get(
                DinnerServingDates.dinner==saved_dinner,
                DinnerServingDates.serving_date==serving_date
            )
    except DoesNotExist:
        DinnerServingDates.create(
            dinner=saved_dinner,
            serving_date=serving_date
        ).save()



    return dinner
