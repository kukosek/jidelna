package org.slavicin.jidelna.activities.main

import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import org.slavicin.jidelna.R
import org.slavicin.jidelna.activities.login.LoginActivity


class webview : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true);
        val myWebView: WebView = findViewById(R.id.webview)
        myWebView.loadUrl("https://jidelna.techbrick.cz/podminky.html")

    }

    override fun onBackPressed() {
        super.onBackPressed()
        val i = Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(i)
        finish()
    }


}