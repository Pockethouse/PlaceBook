package com.markbowen.placebook.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.markbowen.placebook.R
import com.markbowen.placebook.databinding.BookmarkItemBinding
import com.markbowen.placebook.ui.MapsActivity
import com.markbowen.placebook.viewModel.MapsViewModel

// The Adapter constructor takes two arguments: a list of BookmarkView items and
//a reference to the MapsActivity
class BookmarkListAdapter(
    private var bookmarkData: List<MapsViewModel.BookmarkView>?,
    private val mapsActivity: MapsActivity
) : RecyclerView.Adapter<BookmarkListAdapter.ViewHolder>() {
    // A ViewHolder class is defined to hold the view widgets.
    class ViewHolder(
        val binding: BookmarkItemBinding,
        private val mapsActivity: MapsActivity
    ) : RecyclerView.ViewHolder(binding.root) {init {
        binding.root.setOnClickListener {
            val bookmarkView = itemView.tag as MapsViewModel.BookmarkView
            mapsActivity.moveToBookmark(bookmarkView)
        }
    }}


    // setBookmarkData is designed to be called when the bookmark data changes
    //assigns bookmarks to the new BookmarkView List and refreshes the
    //RecyclerView by calling notifyDataSetChanged().
    fun setBookmarkData(bookmarks: List<MapsViewModel.BookmarkView>) {
        this.bookmarkData = bookmarks
        notifyDataSetChanged()
    }
    // used to create a ViewHolder by inflating
    //the bookmark_item layout and passing in the mapsActivity property.
    override fun onCreateViewHolder(parent: ViewGroup, viewType:
    Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = BookmarkItemBinding.inflate(layoutInflater,
            parent, false)
        return ViewHolder(binding, mapsActivity)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // You make sure bookmarkData is not null before doing the binding.
        bookmarkData?.let { list-> val bookmarkViewData = list[position]
            // A reference to the bookmarkViewData is assigned to the holder’s itemView.tag,
            holder.binding.root.tag = bookmarkViewData
            holder.binding.bookmarkData = bookmarkViewData

            bookmarkViewData.categoryResourceId?.let {
                holder.binding.bookmarkIcon.setImageResource(it)
            }

        }
    }

    // getItemCount() is overridden to return the number of items in the
    //bookmarkData list
    override fun getItemCount() = bookmarkData?.size ?: 0
}