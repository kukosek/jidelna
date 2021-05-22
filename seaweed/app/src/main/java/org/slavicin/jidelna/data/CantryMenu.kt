package org.slavicin.jidelna.data

data class Dinner(
    val type: String,
    val menuNumber: Int,
    val name: String,
    val allergens: List<Int>,
    var status: String,
    val dinnerid: Int,
    val numOfReviews: Int
)
data class CantryMenu(
    val date: String, val menus: List<Dinner>
)

data class WeekMenu(
    val daymenus: List<CantryMenu>,
    val creditLeft: Int
)