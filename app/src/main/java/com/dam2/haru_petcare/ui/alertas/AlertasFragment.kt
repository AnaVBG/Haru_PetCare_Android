package com.dam2.haru_petcare.ui.alertas

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment

class AlertasFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return TextView(requireContext()).apply {
            text = "Alertas — próximamente"
            gravity = android.view.Gravity.CENTER
            setTextColor(resources.getColor(com.dam2.haru_petcare.R.color.haru_teal_dark, null))
            textSize = 16f
        }
    }
}