package pt.ipt.dam.booka.data.api

import com.google.gson.annotations.SerializedName

data class GoogleBooksResponse(
    val items: List<BookItem>?
)

data class BookItem(
    val volumeInfo: VolumeInfo
)

data class VolumeInfo(
    val title: String?,
    val authors: List<String>?,
    val publisher: String?,
    val publishedDate: String?,
    val description: String?,
    val imageLinks: ImageLinks?,
    val industryIdentifiers: List<IndustryIdentifier>?
)

data class ImageLinks(
    val thumbnail: String?
)

data class IndustryIdentifier(
    val type: String?,
    val identifier: String?
)