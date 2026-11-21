package com.example.cyberpunktcg.ui.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannerTextRecognizerTest {
    private val recognizer = ScannerTextRecognizer()
    private var photo: File? = null

    @After
    fun deletePhoto() {
        photo?.delete()
    }

    @Test
    fun recognizesTwoLinesOfTextFromAJpeg() = runBlocking {
        val file = createPhoto { canvas ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                typeface = Typeface.DEFAULT_BOLD
                textSize = 80f
            }
            canvas.drawText("CYBERPUNK", 100f, 210f, paint)
            canvas.drawText("SCAN TEST", 100f, 330f, paint)
        }

        assertEquals("CYBERPUNK\nSCAN TEST", recognizer.recognize(file))
    }

    @Test
    fun returnsEmptyTextForABlankJpeg() = runBlocking {
        assertTrue(recognizer.recognize(createPhoto()).isBlank())
    }

    private fun createPhoto(drawContent: (Canvas) -> Unit = {}): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("scanner-ocr-", ".jpg", context.cacheDir).also { photo = it }
        val bitmap = Bitmap.createBitmap(1024, 512, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            drawContent(canvas)
            file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
        } finally {
            bitmap.recycle()
        }
        return file
    }
}
