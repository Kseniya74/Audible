package com.mitsukova.audible.ui.notes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mitsukova.audible.R
import com.mitsukova.audible.data.model.NotesEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotesAdapter(private val notes: MutableList<NotesEntity>,
                   private val deleteNote: suspend (NotesEntity) -> Unit
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewNote: TextView = itemView.findViewById(R.id.textViewNote)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteNote)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.textViewNote.text = note.text

        holder.deleteButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                deleteNote(note)

                val index = notes.indexOf(note)

                if (index != -1) {
                    notes.removeAt(index)
                    CoroutineScope(Dispatchers.Main).launch {
                        notifyItemRemoved(index)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return notes.size
    }
}