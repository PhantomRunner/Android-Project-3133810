package com.example.cyberpunktcg.ui.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.ExifInterface
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannerPhotoDecoderTest {
    private var photo: File? = null

    @After
    fun deletePhoto() {
        photo?.delete()
    }

    @Test
    fun rotatesJpegAccordingToExif() {
        val file = createPhoto()
        ExifInterface(file.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }

        val image = decodeScannerPhoto(file, maxSide = 1600)
        try {
            assertEquals(512, image.width)
            assertEquals(1024, image.height)
            val top = image.getPixel(256, 256)
            val bottom = image.getPixel(256, 768)
            assertTrue(Color.red(top) > Color.blue(top))
            assertTrue(Color.blue(bottom) > Color.red(bottom))
        } finally {
            image.recycle()
        }
    }

    @Test
    fun downsamplesToFitTheMaximumSide() {
        val image = decodeScannerPhoto(createPhoto(), maxSide = 256)
        try {
            assertEquals(256, image.width)
            assertEquals(128, image.height)
        } finally {
            image.recycle()
        }
    }

    private fun createPhoto(): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("scanner-decoder-", ".jpg", context.cacheDir).also { photo = it }
        val bitmap = Bitmap.createBitmap(1024, 512, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.BLUE)
            canvas.drawRect(0f, 0f, 512f, 512f, Paint().apply { color = Color.RED })
            file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
        } finally {
            bitmap.recycle()
        }
        return file
    }
}
