package com.jmonloop.copypath

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ShareReceiveActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleShareIntent(intent)
        finish()
    }

    private fun handleShareIntent(intent: Intent) {
        if (intent.action != Intent.ACTION_SEND) {
            showToast("Invalid action: ${intent.action}")
            return
        }

        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        if (uri == null) {
            showToast("No file shared")
            return
        }

        val path = PathResolver.resolve(this, uri)
        copyToClipboard(path)
        showToast("Copied: $path")
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("File Path", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
