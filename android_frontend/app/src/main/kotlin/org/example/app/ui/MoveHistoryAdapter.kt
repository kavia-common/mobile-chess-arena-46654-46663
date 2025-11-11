package org.example.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.example.app.R

data class MoveRow(val number: Int, val white: String?, val black: String?)

/**
 * PUBLIC_INTERFACE
 * Adapter for move history showing move number and SAN for white/black.
 */
class MoveHistoryAdapter :
    ListAdapter<MoveRow, MoveHistoryAdapter.VH>(DIFF) {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val num: TextView = itemView.findViewById(R.id.tv_move_number)
        val white: TextView = itemView.findViewById(R.id.tv_white_san)
        val black: TextView = itemView.findViewById(R.id.tv_black_san)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_move, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = getItem(position)
        holder.num.text = "${row.number}."
        holder.white.text = row.white ?: ""
        holder.black.text = row.black ?: ""
        holder.itemView.contentDescription = "Move ${row.number}. White ${row.white ?: "no move"}, Black ${row.black ?: "no move"}"
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<MoveRow>() {
            override fun areItemsTheSame(oldItem: MoveRow, newItem: MoveRow): Boolean =
                oldItem.number == newItem.number

            override fun areContentsTheSame(oldItem: MoveRow, newItem: MoveRow): Boolean =
                oldItem == newItem
        }
    }
}
