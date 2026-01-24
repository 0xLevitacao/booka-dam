package pt.ipt.dam.booka.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class AcercaFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = TextView(context)
        view.text = """
            Desenvolvimento de Aplicações Móveis
            Ano Letivo 2025/26
            
            Autores:
            - Diogo Costa - 25307
            - Fisgas - XXXXX
            
            Bibliotecas usadas:
            - CameraX
            - ML Kit Barcode Scanning
            - Retrofit
            - Coil
        """.trimIndent()
        view.textSize = 16f
        view.setPadding(32, 32, 32, 32)
        return view
    }
}