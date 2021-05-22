from datetime import datetime
import cherrypy
from peewee import DateTimeField, DoesNotExist, FloatField, TextField, ForeignKeyField
from Store.base_model import BaseModel
from Store.user import User
from Store.saved_dinner import SavedDinner
class Review(BaseModel):
    user = ForeignKeyField(User)
    dinner = ForeignKeyField(SavedDinner)
    date_posted = DateTimeField()
    rating = FloatField()
    message = TextField()
    score = FloatField()


    def to_dict(self, user: User) -> dict:
        user_score = 0
        try:
            user_score = ReviewScore.get(ReviewScore.user==user, ReviewScore.review==self).user_score
        except DoesNotExist:
            pass
        return {
            "id": self.id,
            "dinnerid": self.dinner.id,
            "dinnerName":self.dinner.full_name,
            "user":self.user.name,
            "rating":self.rating,
            "userScore":user_score,
            "message":self.message,
            "score":self.score,
            "datePosted":self.date_posted.isoformat()
            }

def review_from_dict(
        review_dict: dict,
        user: User, dinner: SavedDinner,
        date_posted: datetime
        ) -> Review:
    try:
        review = Review(
            user=user,
            dinner=dinner,
            rating=review_dict["rating"],
            message=review_dict["message"],
            score=0,
            date_posted=date_posted
            )
    except KeyError:
        raise cherrypy.HTTPError(400, "Bad format")
    return review

class ReviewScore(BaseModel):
    review = ForeignKeyField(Review)
    user = ForeignKeyField(User)
    user_score = FloatField()

def review_score_from_dict(
        userscore_dict: dict,
        user: User,
        review_id: int
    ) -> ReviewScore:
    try:
        review = Review.get_by_id(review_id)
    except DoesNotExist:
        raise cherrypy.HTTPError(400, message="Review does not exist")

    try:
        req_user_score = userscore_dict["userScore"]
    except KeyError:
        raise cherrypy.HTTPError(400, "Please provide 'userScore' in 'score' of request body")
    if abs(req_user_score) > 1.0:
        raise cherrypy.HTTPError(400, "Invalid score")


    old_score = 0
    try:
        review_score = ReviewScore.get(ReviewScore.user==user, ReviewScore.review==review)
        old_score = review_score.user_score
        review_score.user_score = req_user_score
    except DoesNotExist:
        review_score = ReviewScore.create(user=user, review=review, user_score=req_user_score)

    score_change = req_user_score - old_score

    review.score += score_change

    review.save()
    review_score.save()

    return review_score
