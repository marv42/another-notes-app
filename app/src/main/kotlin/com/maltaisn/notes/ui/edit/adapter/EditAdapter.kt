/*
 * Copyright 2020 Nicolas Maltais
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.maltaisn.notes.ui.edit.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.maltaisn.notes.databinding.ItemEditContentBinding
import com.maltaisn.notes.databinding.ItemEditItemAddBinding
import com.maltaisn.notes.databinding.ItemEditItemBinding
import com.maltaisn.notes.databinding.ItemEditTitleBinding
import com.maltaisn.notes.hideKeyboard
import com.maltaisn.notes.ui.edit.EditViewModel


class EditAdapter(val context: Context, val callback: Callback) :
        ListAdapter<EditListItem, RecyclerView.ViewHolder>(EditDiffCallback()) {

    private var recyclerView: RecyclerView? = null

    private var pendingFocusChange: EditViewModel.FocusChange? = null

    private val itemTouchHelper = ItemTouchHelper(DragTouchHelperCallback(context) { from, to ->
        // submitList is not used here, since it results in a very unresponsive design.
        // Adapter and dataset are updated manually.
        notifyItemMoved(from, to)
        callback.onNoteItemSwapped(from, to)
    })

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_TITLE -> EditTitleViewHolder(ItemEditTitleBinding.inflate(
                    inflater, parent, false), callback)
            VIEW_TYPE_CONTENT -> EditContentViewHolder(ItemEditContentBinding.inflate(
                    inflater, parent, false), callback)
            VIEW_TYPE_ITEM_ADD -> EditItemAddViewHolder(ItemEditItemAddBinding.inflate(
                    inflater, parent, false), callback)
            VIEW_TYPE_ITEM -> {
                val viewHolder = EditItemViewHolder(ItemEditItemBinding.inflate(
                        inflater, parent, false), callback)
                viewHolder.binding.dragImv.setOnTouchListener { view, event ->
                    if (event.action == MotionEvent.ACTION_DOWN && callback.isNoteDragEnabled) {
                        // Drag handle was touched. Hide keyboard and start dragging.
                        viewHolder.clearFocus()
                        view.hideKeyboard()
                        itemTouchHelper.startDrag(viewHolder)
                    }
                    false
                }
                viewHolder
            }
            else -> error("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is EditTitleViewHolder -> holder.bind(item as EditTitleItem)
            is EditContentViewHolder -> holder.bind(item as EditContentItem)
            is EditItemViewHolder -> holder.bind(item as EditItemItem)
        }
        if (holder is EditFocusableViewHolder && position == pendingFocusChange?.itemPos) {
            // Apply pending focus change event.
            holder.setFocus(pendingFocusChange!!.pos)
            pendingFocusChange = null
        }
    }

    override fun getItemViewType(position: Int) = getItem(position).type


    fun setItemFocus(focus: EditViewModel.FocusChange) {
        val rcv = recyclerView ?: return

        // If item to focus on doesn't exist yet, save it for later.
        if (!focus.itemExists) {
            pendingFocusChange = focus
            return
        }

        val viewHolder = rcv.findViewHolderForAdapterPosition(focus.itemPos)
        if (viewHolder is EditFocusableViewHolder) {
            viewHolder.setFocus(focus.pos)
        } else {
            // No item view holder for that position.
            // Not supposed to happen, but if it does, just save it for later.
            pendingFocusChange = focus
        }
    }

    interface Callback {
        fun onNoteItemChanged(item: EditItemItem, pos: Int, isPaste: Boolean)
        fun onNoteItemBackspacePressed(item: EditItemItem, pos: Int)
        fun onNoteItemDeleteClicked(pos: Int)

        fun onNoteItemAddClicked()

        fun onNoteClickedToEdit()

        val isNoteDragEnabled: Boolean
        fun onNoteItemSwapped(from: Int, to: Int)
    }

    companion object {
        const val VIEW_TYPE_TITLE = 0
        const val VIEW_TYPE_CONTENT = 1
        const val VIEW_TYPE_ITEM = 2
        const val VIEW_TYPE_ITEM_ADD = 3
    }
}