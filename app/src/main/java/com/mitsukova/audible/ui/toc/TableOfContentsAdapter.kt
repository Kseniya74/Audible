package com.mitsukova.audible.ui.toc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mitsukova.audible.R
import nl.siegmann.epublib.domain.TOCReference

class TableOfContentsAdapter(
    private val tocList: List<TOCReference>,
    private val onItemClick: (TOCReference) -> Unit
) : RecyclerView.Adapter<TableOfContentsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.textViewTOC)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_table_of_contents, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tocReference = tocList[position]
        holder.textView.text = tocReference.title
        holder.itemView.setOnClickListener {
            onItemClick(tocReference)
        }
    }

    override fun getItemCount(): Int {
        return tocList.size
    }
}

