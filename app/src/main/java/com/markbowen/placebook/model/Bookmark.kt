package com.markbowen.placebook.model


import android.content.Context
import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.markbowen.placebook.util.FileUtils
import com.markbowen.placebook.util.ImageUtils



// . The @Entity annotation tells Room that this is a database entity class
@Entity
// constructor is defined using arguments for all properties with default values defined

//The id property is defined using the @PrimaryKey annotation. There must be at...
//...least one of these per Entity class.

data class Bookmark(
    @PrimaryKey(autoGenerate = true) var id: Long? = null,
    var placeId: String? = null,
    var name: String = "",
    var address: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var phone: String = "",
    var notes: String = "",
    var category: String = ""

)
{
    // . setImage() provides the public interface for saving an image for a Bookmark
    fun setImage(image: Bitmap, context: Context) {
        // If the bookmark has an id, then the image gets saved to a file.
        id?.let {
            ImageUtils.saveBitmapToFile(context, image,
                generateImageFilename(it))
        }
    }
    //This allows another object to load an image without having to
    //load the bookmark from the database
    companion object {
        fun generateImageFilename(id: Long): String {
            //  returns a filename based on a Bookmark ID
            return "bookmark$id.png"
        }
    }
    fun deleteImage(context: Context) {
        id?.let {
            FileUtils.deleteFile(context, generateImageFilename(it))
        }
    }
}