package com.mitsukova.audible.ui.toc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mitsukova.audible.databinding.ActivityTableOfContentsBinding
import nl.siegmann.epublib.epub.EpubReader
import java.io.File
import java.io.FileInputStream

class TableOfContentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTableOfContentsBinding
    private lateinit var recyclerViewTOC: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTableOfContentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        recyclerViewTOC = binding.recyclerViewTableOfContents

        val filePath = intent.getStringExtra("filePath")

        if (filePath != null) {
            val file = File(filePath)
            val inputStream = FileInputStream(file)
            val book = EpubReader().readEpub(inputStream)

            recyclerViewTOC.layoutManager = LinearLayoutManager(this)

            val tocList = book.tableOfContents.tocReferences
            val adapter = TableOfContentsAdapter(tocList) { tocReference ->
                val resultIntent = Intent()
                resultIntent.putExtra("tocReferenceHref", tocReference.resource.href)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }

            recyclerViewTOC.adapter = adapter
        }
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

