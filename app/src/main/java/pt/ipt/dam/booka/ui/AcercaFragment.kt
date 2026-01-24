package pt.ipt.dam.booka.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class AcercaFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Layout pai
        val layout = FrameLayout(requireContext())

        // TextView com o conteúdo
        val textView = TextView(requireContext()).apply {
            text = """
            Desenvolvimento de Aplicações Móveis
            Ano Letivo 2025/26
            
            Autores:
            - Diogo Costa - 25307
            - José Nuno Sousa - 20488
            
            Bibliotecas usadas:
            - CameraX
            - ML Kit Barcode Scanning
            - Retrofit
            - Coil
        """.trimIndent()

            textSize = 16f
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(32, 32, 32, 32)
        }

        // Adiciona o TextView centrado
        layout.addView(
            textView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )

        return layout
    }

}