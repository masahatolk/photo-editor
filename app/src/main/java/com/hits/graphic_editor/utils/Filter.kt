package com.hits.graphic_editor.utils

import android.view.LayoutInflater
import com.hits.graphic_editor.databinding.ActivityNewProjectBinding

interface Filter {
    val binding: ActivityNewProjectBinding
    val layoutInflater: LayoutInflater
    val processedImage: ProcessedImage
    fun onClose(onSave: Boolean)
    fun onStart()
}