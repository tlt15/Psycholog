package com.example.psycholog

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ArticleDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article_detail)

        val title = intent.getStringExtra("title") ?: ""
        val content = intent.getStringExtra("content") ?: ""

        findViewById<TextView>(R.id.articleTitle).text = title
        findViewById<TextView>(R.id.articleContent).text = content
    }
}