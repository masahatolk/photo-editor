package com.hits.graphic_editor.affine_transform

import android.view.LayoutInflater
import com.hits.graphic_editor.databinding.ActivityNewProjectBinding
import com.hits.graphic_editor.utils.Filter
import com.hits.graphic_editor.utils.ProcessedImage

class AffineTransform(
    override val binding: ActivityNewProjectBinding,
    override val layoutInflater: LayoutInflater,
    override val processedImage: ProcessedImage
):Filter {
    override fun showBottomMenu() {
    }

    override fun removeAllMenus() {
    }
}