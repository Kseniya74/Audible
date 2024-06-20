package com.mitsukova.audible.data.repository.bookDetail

import android.webkit.WebView

interface WebViewListener {
    fun onTextExtracted(text: String)
    fun onWebViewCreated(webView: WebView)
    fun onTextSelected(text: String)
}