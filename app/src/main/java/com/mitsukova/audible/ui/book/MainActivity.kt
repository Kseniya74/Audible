package com.mitsukova.audible.ui.book

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.mitsukova.audible.R
import com.mitsukova.audible.data.model.CustomBook
import com.mitsukova.audible.data.repository.book.BookRepositoryImpl
import com.mitsukova.audible.data.repository.favourites.BookActionListener
import com.mitsukova.audible.databinding.ActivityMainBinding
import com.mitsukova.audible.ui.book.BookViewModel
import com.mitsukova.audible.ui.book.BookViewModelFactory
import com.mitsukova.audible.ui.favourites.BookAdapter
import com.mitsukova.audible.ui.favourites.FavouritesActivity
import com.mitsukova.audible.utils.ContextType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), BookActionListener {

    private lateinit var binding: ActivityMainBinding
    lateinit var bookViewModel: BookViewModel
    lateinit var bookAdapter: BookAdapter
    val bookList = mutableListOf<CustomBook>()

    private var isSortedAscending = true

    private companion object {
        private const val STORAGE_PERMISSION_CODE = 1
        private const val FILE_PICKER_REQUEST_CODE = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repository = BookRepositoryImpl(application)
        val factory = BookViewModelFactory(application, repository)
        bookViewModel = ViewModelProvider(this, factory).get(BookViewModel::class.java)

        bookAdapter = BookAdapter(ContextType.MAIN_ACTIVITY, bookList, this, bookViewModel, null)
        binding.recyclerViewEpubFiles.layoutManager = GridLayoutManager(this, 3)
        binding.recyclerViewEpubFiles.adapter = bookAdapter

        bookViewModel.bookData.observe(this as LifecycleOwner) { books ->
            bookList.clear()
            bookList.addAll(books)
            bookAdapter.notifyDataSetChanged()
        }

        binding.uploadButton.setOnClickListener {
            if (checkPermission()) {
                uploadBook()
            } else {
                requestPermission()
            }
        }

        binding.listButton.setOnClickListener {
            toggleListBooksView()
        }

        binding.favouritesButton.setOnClickListener {
            startActivity(Intent(this, FavouritesActivity::class.java))
        }

        binding.filterButton.setOnClickListener {
            if (isSortedAscending) {
                binding.filterButton.setImageResource(R.drawable.ic_sort_ascending)
                isSortedAscending = false
            } else {
                binding.filterButton.setImageResource(R.drawable.ic_sort_descending)
                isSortedAscending = true
            }
            bookAdapter.sortBooks()
        }
    }

    override fun onResume() {
        super.onResume()

        bookViewModel.refreshBooks()
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE
        )
    }

    private fun uploadBook() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/epub+zip"
        startActivityForResult(intent, FILE_PICKER_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { fileUri ->
                CoroutineScope(Dispatchers.IO).launch {
                    val filePath = bookViewModel.getFilePathFromUri(fileUri)
                    // Обработка файла
                    if (filePath != null) {
                        handlePickedFile(filePath)
                    }
                }
            }
        }
    }

    private fun handlePickedFile(filePath: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val book = bookViewModel.parseEpubFile(filePath)
            withContext(Dispatchers.Main) {
                if (book != null) {
                    val customBook = CustomBook(book, filePath, false, 0)
                    addBookToBookList(customBook, bookList)
                }
            }
        }
        bookViewModel.uploadBook(filePath)
    }

    private fun addBookToBookList(newBook: CustomBook, bookList: MutableList<CustomBook>) {
        val bookExists = bookList.any { it.filePath == newBook.filePath }

        if (!bookExists) {
            bookViewModel.refreshBooks()
            bookAdapter.notifyDataSetChanged()
        } else
            Toast.makeText(this, "Эта книга уже была загружена", Toast.LENGTH_LONG).show()
    }

    override fun addToFavourites(customBook: CustomBook) {
        CoroutineScope(Dispatchers.IO).launch {
            bookViewModel.addToFavourites(customBook)
            customBook.isFavourite = true
            withContext(Dispatchers.Main) {
                bookAdapter.notifyItemChanged(bookList.indexOf(customBook))
            }
        }
    }

    override fun deleteBook(customBook: CustomBook, position: Int) {
        if (position >= 0 && position < bookList.size) {
            CoroutineScope(Dispatchers.IO).launch {
                bookViewModel.deleteBook(customBook)
                withContext(Dispatchers.Main) {
                    bookList.removeAt(position)
                    bookAdapter.notifyItemRemoved(position)
                }
            }
        }
    }

    private fun toggleListBooksView() {
        val isGridView = (binding.recyclerViewEpubFiles.layoutManager is GridLayoutManager)
        val newLayoutManager = if (isGridView) {
            binding.listButton.setImageResource(R.drawable.grid)
            LinearLayoutManager(this)
        } else {
            binding.listButton.setImageResource(R.drawable.menu_icon)
            GridLayoutManager(this, 3)
        }

        binding.recyclerViewEpubFiles.layoutManager = newLayoutManager
        bookAdapter.notifyDataSetChanged()
    }

    override fun removeFromFavourites(customBook: CustomBook, position: Int) {
        bookViewModel.removeFromFavourites(customBook)
        bookAdapter.notifyDataSetChanged()
    }
}