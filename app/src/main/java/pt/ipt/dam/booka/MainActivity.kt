package pt.ipt.dam.booka

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam.booka.data.api.RetrofitInstance
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import coil.load
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var textResult: TextView
    private lateinit var cameraExecutor: ExecutorService
    private var isProcessing = false

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        textResult = findViewById(R.id.textResult)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (checkCameraPermission()) {
            startCamera()
        } else {
            requestCameraPermission()
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
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
                Toast.makeText(this, "Permissão da câmara necessária", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

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
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Erro ao iniciar câmara", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

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

    private fun fetchBookInfo(isbn: String) {
        isProcessing = true

        runOnUiThread {
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

    private fun showBookDialog(book: pt.ipt.dam.booka.data.api.VolumeInfo) {
        val dialog = android.app.AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_book_info, null)

        val ivBookCover = dialogView.findViewById<android.widget.ImageView>(R.id.ivBookCover)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvTitle)
        val tvAuthor = dialogView.findViewById<TextView>(R.id.tvAuthor)
        val tvYear = dialogView.findViewById<TextView>(R.id.tvYear)
        val tvDescription = dialogView.findViewById<TextView>(R.id.tvDescription)
        val btnCancel = dialogView.findViewById<android.widget.Button>(R.id.btnCancel)
        val btnAddToFavorites = dialogView.findViewById<android.widget.Button>(R.id.btnAddToFavorites)

        // test
        ivBookCover.load(book.imageLinks?.thumbnail?.replace("http://", "https://"))

        tvTitle.text = book.title ?: "Título desconhecido"
        tvAuthor.text = book.authors?.joinToString(", ") ?: "Autor desconhecido"
        tvYear.text = book.publishedDate ?: "Ano desconhecido"
        tvDescription.text = book.description ?: "Sem descrição disponível"

        val alertDialog = dialog.setView(dialogView).create()

        btnCancel.setOnClickListener {
            alertDialog.dismiss()
            isProcessing = false
            textResult.text = "Aponte para um código de barras ISBN"
        }

        btnAddToFavorites.setOnClickListener {
            // TODO: Guardar na base de dados
            Toast.makeText(this, "Livro adicionado aos favoritos!", Toast.LENGTH_SHORT).show()
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