package com.markbowen.placebook.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


//directly call the methods within ImageUtils without creating a new ImageUtils
//object each time

object ImageUtils {

    //saveBitmapToFile() takes in a Context, Bitmap and String for the filename,
    //and saves the Bitmap to permanent storage.

    fun saveBitmapToFile(context: Context, bitmap: Bitmap,
                         filename: String) {

        // ByteArrayOutputStream is created to hold the image data
        val stream = ByteArrayOutputStream()
        // You write the image bitmap to the stream object using the lossless PNG format
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        // The stream is converted into an array of bytes.
        val bytes = stream.toByteArray()
        // saveBytesToFile() is called to write the bytes to a file
        saveBytesToFile(context, bytes, filename)
    }
    // saveBytesToFile() takes in a Context & String 4 filename saves byte as file
    private fun saveBytesToFile(context: Context, bytes:
    ByteArray, filename: String) {
        val outputStream: FileOutputStream

        try {
            // openFileOutput is used to open a FileOutputStream using the given filename.
                //The Context.MODE_PRIVATE flag causes the file to be written in the private area
            outputStream = context.openFileOutput(filename,
                Context.MODE_PRIVATE)
            // . The bytes are written to the outputStream and then the stream is closed.
            outputStream.write(bytes)
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    //This method is passed a context and a filename and returns a Bitmap image by
    //loading the image from the specified filename.

    fun loadBitmapFromFile(context: Context, filename: String):
            Bitmap? {
        val filePath = File(context.filesDir, filename).absolutePath
        return BitmapFactory.decodeFile(filePath)
    }

    @Throws(IOException::class)
    fun createUniqueImageFile(context: Context): File {
        val timeStamp =
            SimpleDateFormat("yyyyMMddHHmmss").format(Date())
        val filename = "PlaceBook_" + timeStamp + "_"
        val filesDir =
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(filename, ".jpg", filesDir)
    }

    fun decodeUriStreamToSize(
        uri: Uri,
        width: Int,
        height: Int,
        context: Context
    ): Bitmap? {
        var inputStream: InputStream? = null
        try {
            val options: BitmapFactory.Options
            // inputStream is opened for the Uri.
            inputStream = context.contentResolver.openInputStream(uri)
            // If the inputStream is not null, then processing continues
            if (inputStream != null) {
                // The image size is determined
                options = BitmapFactory.Options()
                options.inJustDecodeBounds = false
                BitmapFactory.decodeStream(inputStream, null, options)
                // The input stream is closed and opened again, and checked for null
                inputStream.close()
                inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    // The image is loaded from the stream using the downsampling options and is
                    //returned to the caller
                    options.inSampleSize = calculateInSampleSize(
                        options.outWidth, options.outHeight,
                        width, height)
                    options.inJustDecodeBounds = false
                    val bitmap = BitmapFactory.decodeStream(
                        inputStream, null, options)
                    inputStream.close()
                    return bitmap
                }
            }
            return null
        } catch (e: Exception) {
            return null
        } finally {
            // You must close the inputStream once it’s opened, even if an exception is thrown
            inputStream?.close()
        }
    }

    //This method is used to calculate the optimum inSampleSize that can be used to
    //resize an image to a specified width and height. The inSampleSize must be specified
    //as a power of two.
    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight &&
                halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
    //a rotation method that will rotate the bitmap to look
    //correct if rotated incorrectly
    private fun rotateImage(img: Bitmap, degree: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(degree)
        val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width,
            img.height, matrix, true)
        img.recycle()
        return rotatedImg
    }

    //This method is called by BookmarkDetailsActivity to get the downsampled image
    //with a specific width and height from the captured photo file.
    fun decodeFileToSize(
        filePath: String,
        width: Int,
        height: Int
    ): Bitmap {
        // The size of the image is loaded using BitmapFactory.decodeFile()
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(filePath, options)
        // calculateInSampleSize() is called with the image width and height and the
        //requested width and height
        options.inSampleSize = calculateInSampleSize(
            options.outWidth, options.outHeight, width, height)
        // inJustDecodeBounds is set to false to load the full image this time
        options.inJustDecodeBounds = false
        // BitmapFactory.decodeFile() loads the downsampled image from the file
        //returns it
        return BitmapFactory.decodeFile(filePath, options)
    }
        //a method that will check the File meta data to check it's orientation
    //This method gets the orientation in the Exif tags of a JPEG file and calls
        //rotateImage if it is not already 0 degrees.
    @Throws(IOException::class)
    fun rotateImageIfRequired(context: Context, img: Bitmap,
                              selectedImage: Uri
    ): Bitmap {
        val input: InputStream? =
            context.contentResolver.openInputStream(selectedImage)
        val path = selectedImage.path
        val ei: ExifInterface = when {
            Build.VERSION.SDK_INT > 23 && input != null ->
                ExifInterface(input)
            path != null -> ExifInterface(path)
            else -> null
        } ?: return img
        return when (ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img,
                90.0f) ?: img
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img,
                180.0f) ?: img
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img,
                270.0f) ?: img
            else -> img
        }
    }
}