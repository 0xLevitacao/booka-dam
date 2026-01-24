package pt.ipt.dam.booka.data.local

data class Book(
    val id: Int = 0,
    val isbn: String?,
    val titulo: String,
    val autor: String?,
    val ano: String?,
    val capaUrl: String?,
    val descricao: String?,
    val dataAdicionado: Long = System.currentTimeMillis()
)