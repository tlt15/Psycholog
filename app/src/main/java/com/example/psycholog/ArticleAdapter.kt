package com.example.psycholog

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ArticleAdapter(private val articles: List<Article>) :
    RecyclerView.Adapter<ArticleAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleView: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val article = articles[position]
        holder.titleView.text = article.title
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ArticleDetailActivity::class.java).apply {
                putExtra("title", article.title)
                putExtra("content", article.content)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = articles.size
}