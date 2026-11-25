package com.example.cyberpunktcg.ui.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.File
import java.io.IOException

internal fun decodeScannerPhoto(photo: File, maxSide: Int): Bitmap {
    require(maxSide > 0)
    val path = photo.absolutePath
    // Read the size first so large photos can be sampled down during decoding.
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, options)
    if (options.outWidth <= 0 || options.outHeight <= 0) throw IOException("Unreadable photograph")

    val longestSide = maxOf(options.outWidth, options.outHeight)
    var sampleSize = 1
    while (longestSide > maxSide.toLong() * sampleSize) sampleSize *= 2
    options.inJustDecodeBounds = false
    options.inSampleSize = sampleSize

    // Use the photo's EXIF metadata to apply its saved rotation and mirroring.
    val orientation = ExifInterface(path).getAttributeInt(
        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL,
    )
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_TRANSPOSE -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180, ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270, ExifInterface.ORIENTATION_TRANSVERSE -> matrix.postRotate(270f)
    }
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL, ExifInterface.ORIENTATION_FLIP_VERTICAL,
        ExifInterface.ORIENTATION_TRANSPOSE, ExifInterface.ORIENTATION_TRANSVERSE -> matrix.postScale(-1f, 1f)
    }

    val bitmap = BitmapFactory.decodeFile(path, options) ?: throw IOException("Unable to decode photograph")
    if (matrix.isIdentity) return bitmap
    return try {
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } finally {
        bitmap.recycle()
    }
}
