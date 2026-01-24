package pt.ipt.dam.booka.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import coil.load
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam.booka.R
import pt.ipt.dam.booka.data.api.RetrofitInstance
import pt.ipt.dam.booka.data.local.DatabaseHelper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
class ScannerFragment : Fragment() {

    private lateinit var previewView: PreviewView
    private lateinit var textResult: TextView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var dbHelper: DatabaseHelper
    private var isProcessing = false

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_scanner, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        previewView = view.findViewById(R.id.previewView)
        textResult = view.findViewById(R.id.textResult)

        cameraExecutor = Executors.newSingleThreadExecutor()
        dbHelper = DatabaseHelper(requireContext())

        if (checkCameraPermission()) {
            startCamera()
        } else {
            requestCameraPermission()
        }
    }

    //Funções para permissão da camara
    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        requestPermissions(
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(context, "Permissão da câmara necessária", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao iniciar câmara", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    //Leitor ISBN
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && !isProcessing) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            val scanner = BarcodeScanning.getClient()

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        when (barcode.valueType) {
                            Barcode.TYPE_ISBN -> {
                                val isbn = barcode.rawValue
                                if (isbn != null) {
                                    fetchBookInfo(isbn)
                                }
                            }
                            Barcode.TYPE_TEXT,
                            Barcode.TYPE_PRODUCT -> {
                                val code = barcode.rawValue
                                if (code != null && (code.length == 10 || code.length == 13)) {
                                    fetchBookInfo(code)
                                }
                            }
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    //Retorna informação livro scanneado
    private fun fetchBookInfo(isbn: String) {
        isProcessing = true

        activity?.runOnUiThread {
            textResult.text = "A procurar livro com ISBN: $isbn..."
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitInstance.api.searchBookByISBN("isbn:$isbn")

                withContext(Dispatchers.Main) {
                    if (response.items != null && response.items.isNotEmpty()) {
                        val book = response.items[0].volumeInfo
                        showBookDialog(book)
                    } else {
                        textResult.text = "Livro não encontrado para ISBN: $isbn"
                        isProcessing = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    textResult.text = "Erro ao procurar livro: ${e.message}"
                    isProcessing = false
                }
            }
        }
    }

    //mostrar popup com informações do livro
    private fun showBookDialog(book: pt.ipt.dam.booka.data.api.VolumeInfo) {
        val dialog = android.app.AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_book_info, null)

        val ivBookCover = dialogView.findViewById<android.widget.ImageView>(R.id.ivBookCover)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvTitle)
        val tvAuthor = dialogView.findViewById<TextView>(R.id.tvAuthor)
        val tvYear = dialogView.findViewById<TextView>(R.id.tvYear)
        val tvDescription = dialogView.findViewById<TextView>(R.id.tvDescription)
        val btnCancel = dialogView.findViewById<android.widget.Button>(R.id.btnCancel)
        val btnAddToFavorites = dialogView.findViewById<android.widget.Button>(R.id.btnAddToFavorites)

        ivBookCover.load(book.imageLinks?.thumbnail?.replace("http://", "https://"))

        val titulo = book.title ?: "Título desconhecido"
        val autor = book.authors?.joinToString(", ") ?: "Autor desconhecido"
        val ano = book.publishedDate ?: "Ano desconhecido"
        val descricao = book.description ?: "Sem descrição disponível"
        val isbn = book.industryIdentifiers?.firstOrNull()?.identifier
        val capaUrl = book.imageLinks?.thumbnail?.replace("http://", "https://")

        tvTitle.text = titulo
        tvAuthor.text = autor
        tvYear.text = ano
        tvDescription.text = descricao

        val alertDialog = dialog.setView(dialogView).create()

        btnCancel.setOnClickListener {
            alertDialog.dismiss()
            isProcessing = false
            textResult.text = "Aponte para um código de barras ISBN"
        }

        btnAddToFavorites.setOnClickListener {
            // Verificar se já existe
            if (dbHelper.bookExists(isbn)) {
                Toast.makeText(context, "Este livro já está nos favoritos!", Toast.LENGTH_SHORT).show()
            } else {
                // Guardar na base de dados
                val novoLivro = pt.ipt.dam.booka.data.local.Book(
                    isbn = isbn,
                    titulo = titulo,
                    autor = autor,
                    ano = ano,
                    capaUrl = capaUrl,
                    descricao = descricao
                )
                dbHelper.addBook(novoLivro)
                Toast.makeText(context, "Livro adicionado aos favoritos!", Toast.LENGTH_SHORT).show()
            }

            alertDialog.dismiss()
            isProcessing = false
            textResult.text = "Aponte para um código de barras ISBN"
        }

        alertDialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}