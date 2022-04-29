package com.markbowen.placebook.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.DialogFragment

class PhotoOptionDialogFragment : DialogFragment() {
    //  defines an interface that must be implemented by the parent Activity
    interface PhotoOptionDialogListener {
        fun onCaptureClick()
        fun onPickClick()
    }
    // property is defined to hold an instance of PhotoOptionDialogListener
    private lateinit var listener: PhotoOptionDialogListener
    // This is the standard onCreateDialog method for a DialogFragment
    override fun onCreateDialog(savedInstanceState: Bundle?):
            Dialog {
        // The listener property is set to the parent Activity
        listener = activity as PhotoOptionDialogListener
        // The two possible option indices are initialized to -1.
        var captureSelectIdx = -1
        var pickSelectIdx = -1
        // An options ArrayList is defined to hold the AlertDialog options
        val options = ArrayList<String>()
// 7    //You’ll use the activity property of
//the AlertDialog() class as the context.
        val context = activity as Context
        // If the device has a camera capable of capturing images, then a Camera option is
        //added to the options array. The captureSelectIdx variable is set to 0 to indicate
        //the Camera option will be at position 0 in the option list
        if (canCapture(context)) {
            options.add("Camera")
            captureSelectIdx = 0
        }
        // If the device can pick an image from a gallery, then a Gallery option is added to
        //the options array
        if (canPick(context)) {
            options.add("Gallery")
            pickSelectIdx = if (captureSelectIdx == 0) 1 else 0
        }
        // The AlertDialog is built using the options list, and an onClickListener is
        //provided to respond to the user selection.
        return AlertDialog.Builder(context)
            .setTitle("Photo Option")
            .setItems(options.toTypedArray<CharSequence>()) { _,
                                                              which ->
                if (which == captureSelectIdx) {
                    // . If the Camera option was selected, then onCaptureClick() is called on
                    //listener
                    listener.onCaptureClick()
                } else if (which == pickSelectIdx) {
                    // If the Gallery option was selected, then onPickClick() is called on listener.
                    listener.onPickClick()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
    }
    companion object {
        // canPick() determines if the device can pick an image from a gallery.
        fun canPick(context: Context) : Boolean {
            val pickIntent = Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            return (pickIntent.resolveActivity(
                context.packageManager) != null)
        }
        // canCapture() determines if the device has a camera to capture a new image. It
        //uses the same technique as canPick()
        fun canCapture(context: Context) : Boolean {
            val captureIntent = Intent(
                MediaStore.ACTION_IMAGE_CAPTURE)
            return (captureIntent.resolveActivity(
                context.packageManager) != null)
        }
        // newInstance is a helper method intended to be used by the parent activity
        fun newInstance(context: Context) =
            // If the device can pick from a gallery or snap a new image, then the
            //PhotoOptionDialogFragment is created and returned
            if (canPick(context) || canCapture(context)) {
                PhotoOptionDialogFragment()
            } else {
                null
            }
    }
}