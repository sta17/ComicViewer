package no.steven.comicviewer

data class Comic (
    val number: Int,
    val title: String,
    val altText: String,
    val imgUrl: String,
    var imgPath: String
)