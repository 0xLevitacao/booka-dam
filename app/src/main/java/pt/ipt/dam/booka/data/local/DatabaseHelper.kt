package pt.ipt.dam.booka.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "biblioteca.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_BOOKS = "livros"
        const val COLUMN_ID = "id"
        const val COLUMN_ISBN = "isbn"
        const val COLUMN_TITULO = "titulo"
        const val COLUMN_AUTOR = "autor"
        const val COLUMN_ANO = "ano"
        const val COLUMN_CAPA_URL = "capa_url"
        const val COLUMN_DESCRICAO = "descricao"
        const val COLUMN_DATA_ADICIONADO = "data_adicionado"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTable = """
            CREATE TABLE $TABLE_BOOKS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ISBN TEXT,
                $COLUMN_TITULO TEXT NOT NULL,
                $COLUMN_AUTOR TEXT,
                $COLUMN_ANO TEXT,
                $COLUMN_CAPA_URL TEXT,
                $COLUMN_DESCRICAO TEXT,
                $COLUMN_DATA_ADICIONADO INTEGER
            )
        """.trimIndent()

        db?.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_BOOKS")
        onCreate(db)
    }

    // Adicionar livro
    fun addBook(book: Book): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ISBN, book.isbn)
            put(COLUMN_TITULO, book.titulo)
            put(COLUMN_AUTOR, book.autor)
            put(COLUMN_ANO, book.ano)
            put(COLUMN_CAPA_URL, book.capaUrl)
            put(COLUMN_DESCRICAO, book.descricao)
            put(COLUMN_DATA_ADICIONADO, book.dataAdicionado)
        }
        return db.insert(TABLE_BOOKS, null, values)
    }

    // Obter todos os livros
    fun getAllBooks(): List<Book> {
        val books = mutableListOf<Book>()
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_BOOKS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_DATA_ADICIONADO DESC"
        )

        with(cursor) {
            while (moveToNext()) {
                val book = Book(
                    id = getInt(getColumnIndexOrThrow(COLUMN_ID)),
                    isbn = getString(getColumnIndexOrThrow(COLUMN_ISBN)),
                    titulo = getString(getColumnIndexOrThrow(COLUMN_TITULO)),
                    autor = getString(getColumnIndexOrThrow(COLUMN_AUTOR)),
                    ano = getString(getColumnIndexOrThrow(COLUMN_ANO)),
                    capaUrl = getString(getColumnIndexOrThrow(COLUMN_CAPA_URL)),
                    descricao = getString(getColumnIndexOrThrow(COLUMN_DESCRICAO)),
                    dataAdicionado = getLong(getColumnIndexOrThrow(COLUMN_DATA_ADICIONADO))
                )
                books.add(book)
            }
        }
        cursor.close()
        return books
    }

    // Apagar livro
    fun deleteBook(id: Int): Int {
        val db = writableDatabase
        return db.delete(TABLE_BOOKS, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    // Verificar se livro já existe (por ISBN)
    fun bookExists(isbn: String?): Boolean {
        if (isbn == null) return false
        val db = readableDatabase
        val cursor = db.query(
            TABLE_BOOKS,
            arrayOf(COLUMN_ID),
            "$COLUMN_ISBN = ?",
            arrayOf(isbn),
            null,
            null,
            null
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }
}