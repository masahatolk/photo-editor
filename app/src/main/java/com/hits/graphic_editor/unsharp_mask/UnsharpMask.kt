package com.hits.graphic_editor.unsharp_mask

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.hits.graphic_editor.custom_api.alpha
import com.hits.graphic_editor.custom_api.argbToInt
import com.hits.graphic_editor.custom_api.blue
import com.hits.graphic_editor.custom_api.getBitMap
import com.hits.graphic_editor.custom_api.getSimpleImage
import com.hits.graphic_editor.custom_api.getTruncatedChannel
import com.hits.graphic_editor.custom_api.green
import com.hits.graphic_editor.custom_api.red
import com.hits.graphic_editor.databinding.ActivityNewProjectBinding
import com.hits.graphic_editor.databinding.SplineMenuButtonBinding
import com.hits.graphic_editor.databinding.UnsharpmaskBottomMenuBinding
import com.hits.graphic_editor.spline.addUnsharpMaskMenuButton
import com.hits.graphic_editor.spline.removeUnsharpMaskMenuButton
import com.hits.graphic_editor.utils.Filter
import com.hits.graphic_editor.utils.ProcessedImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class UnsharpMask(
    override val binding: ActivityNewProjectBinding,
    override val layoutInflater: LayoutInflater,
    override val processedImage: ProcessedImage

) : Filter {
    private var lastProcessedBitmap: Bitmap? = null
    var blurRadius: Float = 10f
    var amount: Float = 1.0f
    var threshold: Int = 0
    private val dialog: BottomSheetDialog = BottomSheetDialog(binding.root.context)

    private val unsharpMaskBinding: UnsharpmaskBottomMenuBinding by lazy {
        UnsharpmaskBottomMenuBinding.inflate(layoutInflater)
    }
    private val menuButton: SplineMenuButtonBinding by lazy {
        SplineMenuButtonBinding.inflate(layoutInflater)
    }

    override fun onClose(onSave: Boolean) {
        dialog.dismiss()
        removeUnsharpMaskMenuButton(binding, menuButton)
    }

    override fun onStart() {
        addUnsharpMaskMenuButton(binding, menuButton)
        dialog.setContentView(unsharpMaskBinding.root)
        setListeners()
    }

    private fun setListeners() {
        menuButton.button.setOnClickListener { dialog.show() }

        setupIconListeners()
        setupSeekBarListeners()
        setupApplyButtonListener()
    }

    private fun applyUnsharpMasking() {
        val inputBitmap = lastProcessedBitmap ?: getBitMap(processedImage.getSimpleImage())
        val resultBitmap = unsharpMask(inputBitmap, blurRadius, amount, threshold)
        lastProcessedBitmap = resultBitmap

        processedImage.addToLocalStackAndSetImageToView(getSimpleImage(resultBitmap))
    }

    private fun unsharpMask(
        inputBitmap: Bitmap,
        radius: Float,
        amount: Float,
        threshold: Int
    ): Bitmap {
        val mask = createMask(inputBitmap, radius, amount, threshold)
        val thresholdedMask = applyThreshold(mask, threshold)
        return applyMask(inputBitmap, thresholdedMask)
    }

    private fun applyGaussianBlurToImage(inputBitmap: Bitmap, radius: Float): Bitmap {
        val width = inputBitmap.width
        val height = inputBitmap.height

        val blurredBitmap = Bitmap.createBitmap(width, height, inputBitmap.config)
        val radiusInt = radius.toInt()

        runBlocking {
            val horizontalBlurJob =
                async { boxBlur(inputBitmap, blurredBitmap, width, height, radiusInt, true) }
            horizontalBlurJob.await()
            val verticalBlurJob =
                async { boxBlur(blurredBitmap, blurredBitmap, width, height, radiusInt, false) }
            verticalBlurJob.await()
        }

        return blurredBitmap
    }

    private suspend fun boxBlur(
        src: Bitmap,
        dst: Bitmap,
        width: Int,
        height: Int,
        radius: Int,
        horizontal: Boolean
    ) = withContext(Dispatchers.Default) {
        val div = 2 * radius + 1
        val pixels = IntArray(if (horizontal) width else height)
        val resultPixels = IntArray(pixels.size)

        for (i in 0 until if (horizontal) height else width) {
            if (horizontal) src.getPixels(pixels, 0, width, 0, i, width, 1)
            else src.getPixels(pixels, 0, 1, i, 0, 1, height)

            var r = 0
            var g = 0
            var b = 0

            for (j in -radius..radius) {
                val pixel = pixels[min(max(j, 0), pixels.size - 1)]
                r += pixel.red()
                g += pixel.green()
                b += pixel.blue()
            }

            for (j in 0 until pixels.size) {
                val pixelOut = pixels[min(j + radius, pixels.size - 1)]
                val pixelIn = pixels[max(j - radius - 1, 0)]

                r += pixelOut.red() - pixelIn.red()
                g += pixelOut.green() - pixelIn.green()
                b += pixelOut.blue() - pixelIn.blue()

                resultPixels[j] = argbToInt(255, r / div, g / div, b / div)
            }

            if (horizontal) dst.setPixels(resultPixels, 0, width, 0, i, width, 1)
            else dst.setPixels(resultPixels, 0, 1, i, 0, 1, height)
        }
    }

    private fun createMask(
        inputBitmap: Bitmap,
        radius: Float,
        amount: Float,
        threshold: Int
    ): Bitmap {
        val blurredBitmap = applyGaussianBlurToImage(inputBitmap, radius)
        val width = inputBitmap.width
        val height = inputBitmap.height

        val originalPixels = IntArray(width * height)
        val blurredPixels = IntArray(width * height)
        val resultPixels = IntArray(width * height)

        inputBitmap.getPixels(originalPixels, 0, width, 0, 0, width, height)
        blurredBitmap.getPixels(blurredPixels, 0, width, 0, 0, width, height)

        for (i in originalPixels.indices) {
            val originalPixel = originalPixels[i]
            val blurredPixel = blurredPixels[i]

            val diffR = originalPixel.red() - blurredPixel.red()
            val diffG = originalPixel.green() - blurredPixel.green()
            val diffB = originalPixel.blue() - blurredPixel.blue()

            val maskR = if (abs(diffR) >= threshold) diffR else 0
            val maskG = if (abs(diffG) >= threshold) diffG else 0
            val maskB = if (abs(diffB) >= threshold) diffB else 0

            resultPixels[i] = argbToInt(
                originalPixel.alpha(),
                getTruncatedChannel(originalPixel.red() + (amount * maskR).toInt()),
                getTruncatedChannel(originalPixel.green() + (amount * maskG).toInt()),
                getTruncatedChannel(originalPixel.blue() + (amount * maskB).toInt())
            )
        }

        val maskBitmap = Bitmap.createBitmap(width, height, inputBitmap.config)
        maskBitmap.setPixels(resultPixels, 0, width, 0, 0, width, height)
        return maskBitmap
    }

    private fun applyThreshold(mask: Bitmap, threshold: Int): Bitmap {
        val width = mask.width
        val height = mask.height
        val resultPixels = IntArray(width * height)
        val maskPixels = IntArray(width * height)

        mask.getPixels(maskPixels, 0, width, 0, 0, width, height)

        for (i in maskPixels.indices) {
            val pixel = maskPixels[i]
            val r = pixel.red()
            val g = pixel.green()
            val b = pixel.blue()

            resultPixels[i] = argbToInt(
                pixel.alpha(),
                if (abs(r) >= threshold) r else 0,
                if (abs(g) >= threshold) g else 0,
                if (abs(b) >= threshold) b else 0
            )
        }

        val thresholdedBitmap = Bitmap.createBitmap(width, height, mask.config)
        thresholdedBitmap.setPixels(resultPixels, 0, width, 0, 0, width, height)
        return thresholdedBitmap
    }

    private fun applyMask(inputBitmap: Bitmap, mask: Bitmap): Bitmap {
        val width = inputBitmap.width
        val height = inputBitmap.height
        val resultBitmap = Bitmap.createBitmap(width, height, inputBitmap.config)

        runBlocking {
            val originalPixels = IntArray(width * height)
            val maskPixels = IntArray(width * height)
            val resultPixels = IntArray(width * height)

            inputBitmap.getPixels(originalPixels, 0, width, 0, 0, width, height)
            mask.getPixels(maskPixels, 0, width, 0, 0, width, height)

            val jobs = (0 until height).map { y ->
                async {
                    for (x in 0 until width) {
                        val i = y * width + x
                        val originalPixel = originalPixels[i]
                        val maskPixel = maskPixels[i]

                        resultPixels[i] = argbToInt(
                            originalPixel.alpha(),
                            getTruncatedChannel(originalPixel.red() + maskPixel.red()),
                            getTruncatedChannel(originalPixel.green() + maskPixel.green()),
                            getTruncatedChannel(originalPixel.blue() + maskPixel.blue())
                        )
                    }
                }
            }
            jobs.awaitAll()

            resultBitmap.setPixels(resultPixels, 0, width, 0, 0, width, height)
        }

        return resultBitmap
    }

    private fun setupIconListeners() {
        unsharpMaskBinding.radiusIcon.setOnClickListener {
            unsharpMaskBinding.radiusSeekBar.visibility = View.VISIBLE
            unsharpMaskBinding.amountSeekBar.visibility = View.GONE
            unsharpMaskBinding.thresholdSeekBar.visibility = View.GONE
            unsharpMaskBinding.radiusValue.visibility = View.VISIBLE
            unsharpMaskBinding.amountValue.visibility = View.GONE
            unsharpMaskBinding.thresholdValue.visibility = View.GONE
        }

        unsharpMaskBinding.amountIcon.setOnClickListener {
            unsharpMaskBinding.radiusSeekBar.visibility = View.GONE
            unsharpMaskBinding.amountSeekBar.visibility = View.VISIBLE
            unsharpMaskBinding.thresholdSeekBar.visibility = View.GONE
            unsharpMaskBinding.radiusValue.visibility = View.GONE
            unsharpMaskBinding.amountValue.visibility = View.VISIBLE
            unsharpMaskBinding.thresholdValue.visibility = View.GONE
        }

        unsharpMaskBinding.thresholdIcon.setOnClickListener {
            unsharpMaskBinding.radiusSeekBar.visibility = View.GONE
            unsharpMaskBinding.amountSeekBar.visibility = View.GONE
            unsharpMaskBinding.thresholdSeekBar.visibility = View.VISIBLE
            unsharpMaskBinding.radiusValue.visibility = View.GONE
            unsharpMaskBinding.amountValue.visibility = View.GONE
            unsharpMaskBinding.thresholdValue.visibility = View.VISIBLE
        }
    }

    private fun setupSeekBarListeners() {
        unsharpMaskBinding.radiusSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                blurRadius = progress.toFloat()
                unsharpMaskBinding.radiusValue.text = "Blur radius: $progress"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        unsharpMaskBinding.amountSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                amount = progress / 100f
                unsharpMaskBinding.amountValue.text = "Gain: ${amount}"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        unsharpMaskBinding.thresholdSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                threshold = progress
                unsharpMaskBinding.thresholdValue.text = "Threshold: $progress"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupApplyButtonListener() {
        unsharpMaskBinding.applyButton.setOnClickListener {
            applyUnsharpMasking()
        }
    }
}