package com.mitsukova.audible.ui.favourites

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.mitsukova.audible.R
import com.mitsukova.audible.data.model.CustomBook
import com.mitsukova.audible.data.repository.book.BookRepositoryImpl
import com.mitsukova.audible.data.repository.favourites.BookActionListener
import com.mitsukova.audible.data.repository.favourites.FavouriteRepositoryImpl
import com.mitsukova.audible.databinding.ActivityFavouritesBinding
import com.mitsukova.audible.ui.book.BookViewModel
import com.mitsukova.audible.ui.book.BookViewModelFactory
import com.mitsukova.audible.utils.ContextType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.epub.EpubReader
import java.io.FileInputStream

class FavouritesActivity : AppCompatActivity(), BookActionListener {
    private lateinit var binding: ActivityFavouritesBinding
    private lateinit var favouritesAdapter: BookAdapter
    private val favouritesList = mutableListOf<CustomBook>()
    lateinit var bookViewModel: BookViewModel
    private lateinit var favouriteViewModel: FavouritesViewModel
    private lateinit var bookAdapter: BookAdapter

    private var isSortedAscending = true
    private var isGridView = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFavouritesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repository = FavouriteRepositoryImpl(application)
        val factory = FavouritesViewModelFactory(repository)
        favouriteViewModel = ViewModelProvider(this, factory).get(FavouritesViewModel::class.java)

        val bookRepository = BookRepositoryImpl(application)
        val bookFactory = BookViewModelFactory(application, bookRepository)
        bookViewModel = ViewModelProvider(this, bookFactory).get(BookViewModel::class.java)

        bookAdapter = BookAdapter(
            ContextType.MAIN_ACTIVITY,
            favouritesList,
            this,
            null,
            favouriteViewModel
        )

        val recyclerView = binding.recyclerViewFavourites
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        favouritesAdapter = BookAdapter(
            ContextType.FAVOURITES_FRAGMENT,
            favouritesList,
            this,
            null,
            favouriteViewModel
        )
        recyclerView.adapter = favouritesAdapter

        loadFavouriteBooksFromDatabase()

        binding.listButton.setOnClickListener {
            toggleListBooksView()
        }

        binding.homeButton.setOnClickListener {
            finish()
        }

        if (!isSortedAscending) {
            binding.filterButton.setImageResource(R.drawable.ic_sort_descending)
        }

        binding.filterButton.setOnClickListener {
            if (isSortedAscending) {
                binding.filterButton.setImageResource(R.drawable.ic_sort_ascending)
                isSortedAscending = false
            } else {
                binding.filterButton.setImageResource(R.drawable.ic_sort_descending)
                isSortedAscending = true
            }
            favouritesAdapter.sortBooks()
        }
    }

    private fun loadFavouriteBooksFromDatabase() {
        favouriteViewModel.favouriteBookData.observe(this) { favouriteBooks ->
            val customBooks = favouriteBooks.mapNotNull { favouriteBookEntity ->
                val book = parseEpubFile(favouriteBookEntity.filePath)
                book?.let {
                    CustomBook(it, favouriteBookEntity.filePath, true, 0)
                }
            }

            favouritesList.clear()
            favouritesList.addAll(customBooks)
            favouritesAdapter.notifyDataSetChanged()
            updateTextVisibility()
        }
    }

    override fun onResume() {
        super.onResume()
        updateProgressInAdapter()
        favouriteViewModel.refreshFavourites()
    }

    private fun updateProgressInAdapter() {
        favouritesList.forEach { customBook ->
            favouriteViewModel.loadReadingProgress(customBook.filePath).observe(
                this
            ) { progress ->
                customBook.progress = progress ?: 0
            }
        }

        bookAdapter.notifyDataSetChanged()
    }

//    private fun toggleListBooksView() {
//        isGridView = !isGridView
//
//        val layoutManager = if (isGridView) {
//            GridLayoutManager(this, 3)
//        } else {
//            LinearLayoutManager(this)
//        }
//
//        binding.recyclerViewFavourites.layoutManager = layoutManager
//        favouritesAdapter.notifyDataSetChanged()
//    }

    private fun toggleListBooksView() {
        val isGridView = (binding.recyclerViewFavourites.layoutManager is GridLayoutManager)
        val newLayoutManager = if (isGridView) {
            binding.listButton.setImageResource(R.drawable.grid)
            LinearLayoutManager(this)
        } else {
            binding.listButton.setImageResource(R.drawable.menu_icon)
            GridLayoutManager(this, 3)
        }

        binding.recyclerViewFavourites.layoutManager = newLayoutManager
        bookAdapter.notifyDataSetChanged()
    }

    private fun parseEpubFile(filePath: String): Book? {
        return try {
            EpubReader().readEpub(FileInputStream(filePath))
        } catch (e: Exception) {
            Log.e("FavouritesActivity", "Error parsing EPUB file: ${e.message}")
            null
        }
    }

    override fun addToFavourites(customBook: CustomBook) {}

    override fun deleteBook(customBook: CustomBook, position: Int) {}

    override fun removeFromFavourites(customBook: CustomBook, position: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            favouriteViewModel.removeFromFavourites(customBook.filePath)

            withContext(Dispatchers.Main) {
                customBook.isFavourite = false
                favouritesList.removeAt(position)
                favouritesAdapter.notifyItemRemoved(position)
                favouritesAdapter.notifyDataSetChanged()
                bookViewModel.refreshBooks()
            }
        }
    }

    private fun updateTextVisibility() {
        val textView = binding.textFavourites
        textView.visibility = if (favouritesList.isEmpty()) View.VISIBLE else View.GONE
    }
}