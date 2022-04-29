package com.markbowen.placebook.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import com.markbowen.placebook.R
import com.markbowen.placebook.databinding.ActivityBookmarkDetailsBinding
import com.markbowen.placebook.viewModel.BookmarkDetailsViewModel
import androidx.lifecycle.Observer
import com.markbowen.placebook.util.ImageUtils
import java.io.File
import java.net.URLEncoder

//sets the content view with the
//DataBindingUtil helper class that will create our binding class

//calls the built-in setSupportActionBar() to make the
//Toolbar act as the ActionBar for this Activity

class BookmarkDetailsActivity : AppCompatActivity(),
    PhotoOptionDialogFragment.PhotoOptionDialogListener {

    private lateinit var databinding: ActivityBookmarkDetailsBinding
    private var photoFile: File? = null

    private val bookmarkDetailsViewModel by viewModels<BookmarkDetailsViewModel>()
    private var bookmarkDetailsView: BookmarkDetailsViewModel.BookmarkDetailsView? = null

    override fun onCreate(savedInstanceState: android.os.Bundle?)
    {
        super.onCreate(savedInstanceState)
        databinding = DataBindingUtil.setContentView(this,
            R.layout.activity_bookmark_details)
        setupToolbar()
        getIntentData()
        setupFab()
    }

    private fun getIntentData() {
        // 1
        val bookmarkId = intent.getLongExtra(
            MapsActivity.Companion.EXTRA_BOOKMARK_ID, 0
        )
        // 2

        bookmarkDetailsViewModel.getBookmark(bookmarkId)?.observe(this
        ) {
            it?.let {
                bookmarkDetailsView = it
                // 4
                databinding.bookmarkDetailsView = it
                populateImageView()
                populateCategoryList()
            }
        }
    }

    private fun populateCategoryList() {
        // The method returns immediately if bookmarkDetailsView is null.
        val bookmarkView = bookmarkDetailsView ?: return
        // You retrieve the category icon resourceId from the view model.
        val resourceId =
            bookmarkDetailsViewModel.getCategoryResourceId(bookmarkView.category)
        //If the resourceId is not null, you update imageViewCategory to the category
        //icon.
        resourceId?.let { databinding.imageViewCategory.setImageResource(it) }

        val categories = bookmarkDetailsViewModel.getCategories()

        val adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_item, categories)

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        databinding.spinnerCategory.adapter = adapter

        val placeCategory = bookmarkView.category

        databinding.spinnerCategory.setSelection(adapter.getPosition(placeCategory))


        databinding.spinnerCategory.post {
            // You assign the spinnerCategory onItemSelectedListener property to an
            //instance of the onItemSelectedListener class
            databinding.spinnerCategory.onItemSelectedListener = object :
                AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view:
                View, position: Int, id: Long) {
                    // When the user selects a new category, you call onItemSelected().
                    val category = parent.getItemAtPosition(position) as
                            String
                    val resourceId =
                        bookmarkDetailsViewModel.getCategoryResourceId(category)
                    resourceId?.let {
                        databinding.imageViewCategory.setImageResource(it) }
                }
                override fun onNothingSelected(parent: AdapterView<*>) {
                    // NOTE: This method is required but not used.
                }
            }
        }
    }

    private fun sharePlace() {
        // An early return is taken if bookmarkView is null.
        val bookmarkView = bookmarkDetailsView ?: return

        var mapUrl = ""
        if (bookmarkView.placeId == null) {
            // A string with the latitude/longitude separated by a comma is constructed
            val location = URLEncoder.encode("${bookmarkView.latitude},"
                    + "${bookmarkView.longitude}", "utf-8")
            mapUrl = "https://www.google.com/maps/dir/?api=1" +
                    "&destination=$location"
        } else {
            // For the option with the place ID available, the destination contains the place
            //name.
            val name = URLEncoder.encode(bookmarkView.name, "utf-8")
            mapUrl = "https://www.google.com/maps/dir/?api=1" +
                    "&destination=$name&destination_place_id=" +
                    "${bookmarkView.placeId}"
        }
        // You create the sharing Activity Intent and set the action to ACTION_SEND.
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        // The app that receives the
        //Intent can choose which of the data items to use and which to ignore
        sendIntent.putExtra(Intent.EXTRA_TEXT,
            "Check out ${bookmarkView.name} at:\n$mapUrl")
        sendIntent.putExtra(Intent.EXTRA_SUBJECT,
            "Sharing ${bookmarkView.name}")
        // The Intent type is set to a MIME type of “text/plain”. This instructs Android that
        //you intend to share plain text data
        sendIntent.type = "text/plain"
        // Finally, the sharing Activity is started.
        startActivity(sendIntent)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int,
                                  data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // resultCode is checked to make sure the user didn’t cancel the photo
        //capture
        if (resultCode == android.app.Activity.RESULT_OK) {
            // The requestCode is checked to see which call is returning a result
            when (requestCode) {
                // If the requestCode matches REQUEST_CAPTURE_IMAGE, then processing
                //continues
                REQUEST_CAPTURE_IMAGE -> {
                    // You return early from the method if there is no photoFile defined.
                    val photoFile = photoFile ?: return
                    // The permissions you set before are now revoked since they’re no longer needed
                    val uri = FileProvider.getUriForFile(this,
                        "com.markbowen.placebook.fileprovider",
                        photoFile)
                    revokeUriPermission(uri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    // getImageWithPath() is called to get the image from the new photo path, and
                    //updateImage()
                    val image = getImageWithPath(photoFile.absolutePath)
                    val bitmap = ImageUtils.rotateImageIfRequired(this,
                        image , uri)
                    updateImage(bitmap)


                }

                //If the Activity result is from selecting a gallery image, and the data returned is valid,
                //then getImageWithAuthority() is called to load the selected image
                REQUEST_GALLERY_IMAGE -> if (data != null && data.data != null)
                {
                    val imageUri = data.data as Uri
                    val image = getImageWithAuthority(imageUri)
                    image?.let {
                        val bitmap = ImageUtils.rotateImageIfRequired(this, it,
                            imageUri)
                        updateImage(bitmap)
                    }
                }
            }
        }
    }

//This method uses the new decodeFileSize method to load the downsampled image
//and return it.
    private fun getImageWithPath(filePath: String) =
        ImageUtils.decodeFileToSize(
            filePath,
            resources.getDimensionPixelSize(R.dimen.default_image_width),
            resources.getDimensionPixelSize(R.dimen.default_image_height)
        )

    //You override onCreateOptionsMenu and provide items for the Toolbar by loading in
    //the menu_bookmark_details menu

    override fun onCreateOptionsMenu(menu: android.view.Menu):
            Boolean {
        menuInflater.inflate(R.menu.menu_bookmark_details, menu)
        return true
    }
//When the user taps on the bookmark image, you call replaceImage(). This attempts
//to create the PhotoOptionDialogFragment fragment
    private fun replaceImage() {
        val newFragment = PhotoOptionDialogFragment.newInstance(this)
        newFragment?.show(supportFragmentManager, "photoOptionDialog")
    }

    override fun onCaptureClick() {
        // . Any previously assigned photoFile is cleared.
        photoFile = null
        try {
            //   You call createUniqueImageFile() to create a uniquely named image File and
            //assign it to photoFile
            photoFile = ImageUtils.createUniqueImageFile(this)
        } catch (ex: java.io.IOException) {
            // If an exception is thrown, the method returns without doing anything
            return
        }
// You use the ?.let to make sure photoFile is not null before continuing with
//the rest of the method
        photoFile?.let { photoFile ->
            // FileProvider.getUriForFile() is called to get a Uri for the temporary photo
            //file
            val photoUri = FileProvider.getUriForFile(
                this,
                "com.markbowen.placebook.fileprovider",
                photoFile
            )
            // A new Intent is created with the ACTION_IMAGE_CAPTURE action.
            val captureIntent =
                Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            // The photoUri is added as an extra on the Intent, so the Intent knows where to
            //save the full-size image captured by the user.

            captureIntent.putExtra(
                android.provider.MediaStore.EXTRA_OUTPUT,
                photoUri
            )
            // Temporary write permissions on the photoUri are given to the Intent.
            val intentActivities = packageManager.queryIntentActivities(
                captureIntent, PackageManager.MATCH_DEFAULT_ONLY
            )
            intentActivities.map { it.activityInfo.packageName }
                .forEach {
                    grantUriPermission(
                        it, photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
            // The Intent is invoked, and the request code REQUEST_CAPTURE_IMAGE is passed
            //in
            startActivityForResult(captureIntent, REQUEST_CAPTURE_IMAGE)
        }
    }
    override fun onPickClick() {
        val pickIntent = Intent(Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(pickIntent, REQUEST_GALLERY_IMAGE)
    }

    private fun setupFab() {
        databinding.fab.setOnClickListener { sharePlace() }
    }

    private fun setupToolbar() {
        setSupportActionBar(databinding.toolbar)
    }
//This method assigns an image to the imageViewPlace and saves it to the bookmark
//image file
    private fun updateImage(image: Bitmap) {
        bookmarkDetailsView?.let {
            databinding.imageViewPlace.setImageBitmap(image)
            it.setImage(this, image)
        }
    }
//This method uses the new decodeUriStreamToSize method to load the
//downsampled image and return it.
    private fun getImageWithAuthority(uri: Uri) =
        ImageUtils.decodeUriStreamToSize(
            uri,
            resources.getDimensionPixelSize(R.dimen.default_image_width),
            resources.getDimensionPixelSize(R.dimen.default_image_height),
            this
        )

    //This method loads the image from bookmarkView and then uses it to set the
    //imageViewPlace ..
    private fun populateImageView() {
        bookmarkDetailsView?.let { bookmarkView ->

            val placeImage = bookmarkView.getImage(this)
            placeImage?.let {
                databinding.imageViewPlace.setImageBitmap(placeImage)
                databinding.imageViewPlace.setOnClickListener {
                    replaceImage()
                }
            }
        }
    }

    private fun deleteBookmark()
    {
        val bookmarkView = bookmarkDetailsView ?: return
        AlertDialog.Builder(this)
            .setMessage("Delete?")
            .setPositiveButton("Ok") { _, _ ->
                bookmarkDetailsViewModel.deleteBookmark(bookmarkView)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .create().show()
    }

 //This method takes the current changes from the text fields and updates the
 //bookmark
    private fun saveChanges() {
        val name = databinding.editTextName.text.toString()
        if (name.isEmpty()) {
            return
        }
        bookmarkDetailsView?.let { bookmarkView ->
            bookmarkView.name = databinding.editTextName.text.toString()
            bookmarkView.notes =
                databinding.editTextNotes.text.toString()
            bookmarkView.address =
                databinding.editTextAddress.text.toString()
            bookmarkView.phone =
                databinding.editTextPhone.text.toString()

            bookmarkView.category = databinding.spinnerCategory.selectedItem
                    as String

            bookmarkDetailsViewModel.updateBookmark(bookmarkView)
        }
        finish()
    }
    //This defines the request code to use when processing the camera capture Intent.
    companion object {
        private const val REQUEST_GALLERY_IMAGE = 2

        private const val REQUEST_CAPTURE_IMAGE = 1
    }

        //This method is called when the user selects a Toolbar checkmark item
    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {

            R.id.action_save -> {
                saveChanges()
                true
            }
            R.id.action_delete -> {
                deleteBookmark()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }


}