package com.mitsukova.audible.ui.favourites
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.mitsukova.audible.R
import com.mitsukova.audible.data.model.CustomBook
import com.mitsukova.audible.data.repository.favourites.BookActionListener
import com.mitsukova.audible.ui.book.BookViewModel
import com.mitsukova.audible.ui.bookDetail.BookDetailActivity
import com.mitsukova.audible.utils.ContextType
import nl.siegmann.epublib.domain.Book
import java.io.ByteArrayInputStream

class BookAdapter(
    private val contextType: ContextType,
    private val bookDataClassList: MutableList<CustomBook>,
    private val listener: BookActionListener,
    private val bookViewModel: BookViewModel?,
    private val favouriteBookViewModel: FavouritesViewModel?
) : RecyclerView.Adapter<BookAdapter.ViewHolder>() {
    private val itemBook = R.layout.item_book
    private var showBookDetails: Boolean = true
    private var isSortedAscending = true

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cover: ImageView = itemView.findViewById(R.id.bookCoverImageView)
        val favouriteButton: ImageView = itemView.findViewById(R.id.favouriteButton)
        val deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)
        val unfavouriteButton: ImageView = itemView.findViewById(R.id.unfavouriteButton)
        val bookTitleTextView: TextView = itemView.findViewById(R.id.bookTitleTextView)
        val bookAuthorTextView: TextView = itemView.findViewById(R.id.bookAuthorTextView)
        val inFavouriteButton: ImageView = itemView.findViewById(R.id.inFavouriteButton)
        val readingSeekBar: SeekBar = itemView.findViewById(R.id.readingSeekBar)
        val progressPercentage: TextView = itemView.findViewById(R.id.progressPercentage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(itemBook, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val customBook = bookDataClassList[position]
        holder.cover.setImageBitmap(getCoverImage(customBook.epubBook))
        holder.cover.setOnClickListener {
            openBook(customBook.filePath, it)
        }

        bookViewModel?.loadReadingProgress(customBook.filePath)?.observe(
            holder.itemView.context as LifecycleOwner
        ) { progress ->
            holder.readingSeekBar.progress = progress ?: 0
            holder.readingSeekBar.isEnabled = false
            holder.progressPercentage.text = "${progress ?: 0}%"
        }

        favouriteBookViewModel?.loadReadingProgress(customBook.filePath)?.observe(
            holder.itemView.context as LifecycleOwner
        ) { progress ->
            holder.readingSeekBar.progress = progress ?: 0
            holder.readingSeekBar.isEnabled = false
            holder.progressPercentage.text = "${progress ?: 0}%"
        }

        if (customBook.isFavourite) {
            holder.favouriteButton.visibility = View.GONE
            holder.inFavouriteButton.visibility = View.VISIBLE
        } else {
            holder.favouriteButton.visibility = View.VISIBLE
            holder.inFavouriteButton.visibility = View.GONE
        }

        if (showBookDetails) {
            holder.bookTitleTextView.text = customBook.epubBook.title
            holder.bookAuthorTextView.text = customBook.epubBook.metadata.authors.joinToString("")
            holder.bookTitleTextView.visibility = View.VISIBLE
            holder.bookAuthorTextView.visibility = View.VISIBLE
        } else {
            holder.bookTitleTextView.visibility = View.GONE
            holder.bookAuthorTextView.visibility = View.GONE
        }

        when (contextType) {
            ContextType.MAIN_ACTIVITY -> {
                holder.favouriteButton.visibility = View.VISIBLE
                holder.deleteButton.visibility = View.VISIBLE
                holder.unfavouriteButton.visibility = View.GONE
            }
            ContextType.FAVOURITES_FRAGMENT -> {
                holder.inFavouriteButton.visibility = View.GONE
                holder.favouriteButton.visibility = View.GONE
                holder.deleteButton.visibility = View.GONE
                holder.unfavouriteButton.visibility = View.VISIBLE
            }
        }

        holder.favouriteButton.setOnClickListener {
            listener.addToFavourites(customBook)
        }

        holder.deleteButton.setOnClickListener {
            listener.deleteBook(customBook, position)
        }

        holder.unfavouriteButton.setOnClickListener {
            listener.removeFromFavourites(customBook, position)
        }
    }

    override fun getItemCount(): Int {
        return bookDataClassList.size
    }

    fun getCoverImage(epubBook: Book): Bitmap? {
        val coverPage = epubBook.spine.spineReferences.firstOrNull()?.resource

        return if (coverPage != null && epubBook.coverImage != null) {
            val coverImageBytes = epubBook.coverImage.data
            val coverImageStream = ByteArrayInputStream(coverImageBytes)
            BitmapFactory.decodeStream(coverImageStream)
        } else {
            null
        }
    }

    private fun openBook(bookFilePath: String, itemView: View) {
        val intent = Intent(itemView.context, BookDetailActivity::class.java)
        intent.putExtra("PATH", bookFilePath)
        itemView.context.startActivity(intent)
    }

    fun sortBooks() {
        if (isSortedAscending) {
            bookDataClassList.sortBy { it.epubBook.title }
            isSortedAscending = false
        } else {
            bookDataClassList.sortByDescending { it.epubBook.title }
            isSortedAscending = true
        }
        notifyDataSetChanged()
    }
}