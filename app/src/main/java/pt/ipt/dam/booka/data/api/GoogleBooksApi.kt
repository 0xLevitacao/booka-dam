package pt.ipt.dam.booka.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleBooksApi {

    @GET("volumes")
    suspend fun searchBookByISBN(
        @Query("q") query: String
    ): GoogleBooksResponse

    companion object {
        const val BASE_URL = "https://www.googleapis.com/books/v1/"
    }
}