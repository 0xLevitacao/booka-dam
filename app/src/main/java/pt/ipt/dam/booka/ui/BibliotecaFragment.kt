package pt.ipt.dam.booka.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import pt.ipt.dam.booka.R
import pt.ipt.dam.booka.data.local.Book
import pt.ipt.dam.booka.data.local.DatabaseHelper

class BibliotecaFragment : Fragment() {

    private lateinit var searchView: SearchView
    private lateinit var rvBooks: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var bookAdapter: BookAdapter
    private var allBooks = listOf<Book>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_biblioteca, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchView = view.findViewById(R.id.searchView)
        rvBooks = view.findViewById(R.id.rvBooks)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        dbHelper = DatabaseHelper(requireContext())

        // Setup RecyclerView
        rvBooks.layoutManager = LinearLayoutManager(requireContext())
        bookAdapter = BookAdapter(emptyList()) { book ->
            showBookDetailsDialog(book)
        }
        rvBooks.adapter = bookAdapter

        // Setup SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterBooks(newText ?: "")
                return true
            }
        })

        loadBooks()
    }

    //carrega a lista de livros
    override fun onResume() {
        super.onResume()
        loadBooks()
    }

    private fun loadBooks() {
        allBooks = dbHelper.getAllBooks()
        filterBooks(searchView.query.toString())
    }

    //Função de pesquisa para a barra de navegação do ecrã Biblioteca
    private fun filterBooks(query: String) {
        //sem nada escrito
        val filteredBooks = if (query.isEmpty()) {
            allBooks
        } else {
            //introduz autor ou titulo
            allBooks.filter { book ->
                book.titulo.contains(query, ignoreCase = true) ||
                        book.autor?.contains(query, ignoreCase = true) == true
            }
        }

        if (filteredBooks.isEmpty()) {
            rvBooks.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = if (query.isEmpty()) {
                "Nenhum livro adicionado ainda.\nUse o Scanner para adicionar livros!"
            } else {
                "Nenhum livro encontrado com \"$query\""
            }
        } else {
            rvBooks.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
            bookAdapter.updateBooks(filteredBooks)
        }
    }

    //mostra informações detalhadas sobre um livro selecionado
    private fun showBookDetailsDialog(book: Book) {
        val dialog = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_book_details, null)

        val ivBookCover = dialogView.findViewById<android.widget.ImageView>(R.id.ivBookCover)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvTitle)
        val tvAuthor = dialogView.findViewById<TextView>(R.id.tvAuthor)
        val tvYear = dialogView.findViewById<TextView>(R.id.tvYear)
        val tvDescription = dialogView.findViewById<TextView>(R.id.tvDescription)
        val btnClose = dialogView.findViewById<android.widget.Button>(R.id.btnClose)
        val btnRemove = dialogView.findViewById<android.widget.Button>(R.id.btnRemove)

        ivBookCover.load(book.capaUrl)
        tvTitle.text = book.titulo
        tvAuthor.text = book.autor ?: "Autor desconhecido"
        tvYear.text = book.ano ?: "Ano desconhecido"
        tvDescription.text = book.descricao ?: "Sem descrição disponível"

        val alertDialog = dialog.setView(dialogView).create()

        btnClose.setOnClickListener {
            alertDialog.dismiss()
        }

        //remover o livro dos favoritos
        btnRemove.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Remover livro")
                .setMessage("Tens a certeza que queres remover \"${book.titulo}\" dos favoritos?")
                .setPositiveButton("Sim") { _, _ ->
                    dbHelper.deleteBook(book.id)
                    Toast.makeText(context, "Livro removido dos favoritos", Toast.LENGTH_SHORT).show()
                    alertDialog.dismiss()
                    loadBooks()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        alertDialog.show()
    }
}