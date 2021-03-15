package xyz.pokkst.pokket.cash.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import xyz.pokkst.pokket.cash.R

class TokenHeaderAdapter(private val text: String) :
    RecyclerView.Adapter<TokenHeaderAdapter.TokenHeaderViewHolder>() {

    var isVisible: Boolean = true
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): TokenHeaderViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.token_header_layout, viewGroup, false)
        return TokenHeaderViewHolder(view, text)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: TokenHeaderViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.bind()
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = if(isVisible) 1 else 0

    class TokenHeaderViewHolder(itemView: View, private val text: String) : RecyclerView.ViewHolder(itemView) {

        fun bind() {
            itemView.findViewById<TextView>(R.id.header_text)?.text = text
        }
    }
}
