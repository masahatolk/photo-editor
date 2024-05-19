package com.hits.graphic_editor.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.hits.graphic_editor.NewProjectActivity
import com.hits.graphic_editor.affine_transform.AffineTransform
import com.hits.graphic_editor.custom_api.getBitMap
import com.hits.graphic_editor.databinding.ActivityNewProjectBinding
import com.hits.graphic_editor.databinding.BottomMenuBinding
import com.hits.graphic_editor.databinding.ExtraTopMenuBinding
import com.hits.graphic_editor.databinding.TopMenuBinding
import com.hits.graphic_editor.face_detection.FaceDetection
import com.hits.graphic_editor.rotation.Rotation
import com.hits.graphic_editor.scaling.Scaling
import com.hits.graphic_editor.color_correction.ColorCorrection
import com.hits.graphic_editor.cube_3d.Cube3D
import com.hits.graphic_editor.utils.ProcessedImage
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


fun setListenersToExtraTopMenu(
    binding: ActivityNewProjectBinding,
    topMenu: TopMenuBinding,
    bottomMenu: BottomMenuBinding,
    extraTopMenu: ExtraTopMenuBinding,
    processedImage: ProcessedImage,
    scaling: Scaling,
    rotation: Rotation,
    filter: ColorCorrection,
    faceDetection: FaceDetection,
    affine: AffineTransform,
    cube: Cube3D
) {
    extraTopMenu.close.setOnClickListener {

        processedImage.switchStackMode(false)
        binding.imageView.setImageBitmap(getBitMap(processedImage.getSimpleImage()))

        removeExtraTopMenu(binding, extraTopMenu)
        scaling.removeAllMenus()
        rotation.removeAllMenus()
        filter.removeAllMenus()
        faceDetection.removeAllMenus()
        affine.removeAllMenus()
        cube.removeAllMenus()
        //...

        addTopMenu(binding, topMenu)
        addBottomMenu(binding, bottomMenu)
    }

    extraTopMenu.save.setOnClickListener {

        processedImage.switchStackMode(true)
        binding.imageView.setImageBitmap(getBitMap(processedImage.getSimpleImage()))

        removeExtraTopMenu(binding, extraTopMenu)
        scaling.removeAllMenus()
        rotation.removeAllMenus()
        filter.removeAllMenus()
        faceDetection.removeAllMenus()
        affine.removeAllMenus()
        cube.removeAllMenus()
        //...

        addTopMenu(binding, topMenu)
        addBottomMenu(binding, bottomMenu)
    }
    /*
    extraTopMenu.undo.setOnClickListener() {
        processedImage.undoAndSetImageToView()
    }

    extraTopMenu.redo.setOnClickListener(){
        processedImage.redoAndSetImageToView()
    }*/
}

fun setListenersToTopMenu(
    activity: NewProjectActivity,
    binding: ActivityNewProjectBinding,
    context: Context,
    topMenu: TopMenuBinding,
    processedImage: ProcessedImage

) {
    topMenu.close.setOnClickListener() {
        activity.finish()
    }

    topMenu.undo.setOnClickListener() {
        processedImage.undoAndSetImageToView()
    }

    topMenu.redo.setOnClickListener(){
        processedImage.redoAndSetImageToView()
    }

    topMenu.download.setOnClickListener() {
        val bitmap = (binding.imageView.drawable as BitmapDrawable).bitmap
        val fileName = "${System.currentTimeMillis()}" + ".png"
        var fos: OutputStream? = null

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver?.also { resolver ->

                val contentValues = ContentValues().apply {

                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                val imageUri: Uri? =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
        }
        else {
            val root = Environment.getExternalStorageDirectory()
            val directory = File("$root/DemoApps")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, fileName)
            fos = FileOutputStream(file)
        }
        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            Toast.makeText(context, "image saved to the gallery", Toast.LENGTH_SHORT).show()
        }
    }

    topMenu.share.setOnClickListener() {
        val bitmap = (binding.imageView.drawable as BitmapDrawable).bitmap
        val path = MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, "Image", null)
        val uri: Uri = Uri.parse(path)

        val intent = Intent(Intent.ACTION_SEND)
        intent.type="image/png"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        context.startActivity(Intent.createChooser(intent,"Share using"))
    }
}