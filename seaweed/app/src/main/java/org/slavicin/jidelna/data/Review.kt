package org.slavicin.jidelna.data

data class Review(
    val id: Int,
    val dinnerid: Int,
    val dinnerName: String,
    val user: String,
    val rating: Double,
    val message: String,
    var score: Double,
    val userScore: Double,
    val datePosted: String
)

data class ReviewList(
    val reviews: List<Review>
)
