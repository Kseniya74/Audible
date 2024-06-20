package com.mitsukova.audible.ui.notes

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mitsukova.audible.R
import com.mitsukova.audible.data.database.AudibleDatabase
import com.mitsukova.audible.data.repository.notes.NotesRepositoryImpl
import kotlinx.coroutines.launch

class NotesActivity : AppCompatActivity() {
    private lateinit var notesViewModel: NotesViewModel
    private lateinit var emptyMessageTextView: TextView
    private lateinit var notesRecyclerView: RecyclerView
    private var filePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notes)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        filePath = intent.getStringExtra("filePath")

        notesRecyclerView = findViewById(R.id.recyclerViewNotes)
        notesRecyclerView.layoutManager = LinearLayoutManager(this)

        emptyMessageTextView = findViewById(R.id.emptyMessageTextView)

        val notesRepository = NotesRepositoryImpl(AudibleDatabase.getDatabase(this))
        val factory = NotesViewModelFactory(notesRepository)
        notesViewModel = ViewModelProvider(this, factory).get(NotesViewModel::class.java)

        lifecycleScope.launch {
            if (filePath != null) {
                notesViewModel.getNotesByBookId(filePath!!)
            }
        }

        notesViewModel.notes.observe(this) { notes ->
            if (notes != null) {
                if (notes.isEmpty()) {
                    emptyMessageTextView.visibility = View.VISIBLE
                    emptyMessageTextView.text = "Здесь будут отображаться сохраненные цитаты"
                    notesRecyclerView.visibility = View.GONE
                } else {
                    emptyMessageTextView.visibility = View.GONE
                    notesRecyclerView.visibility = View.VISIBLE
                    notesRecyclerView.adapter = NotesAdapter(notes.toMutableList()) { notes ->
                        lifecycleScope.launch {
                            notesViewModel.deleteNote(notes)
                        }
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_note, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}