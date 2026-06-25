package com.gavthan.manager.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Saves an exported file to cache and opens the Android share sheet. */
object Exporter {
    const val XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    const val CSV_MIME = "text/csv"

    fun share(ctx: Context, fileName: String, bytes: ByteArray, mime: String) {
        val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeBytes(bytes)
        val uri: Uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(
            Intent.createChooser(send, "Export $fileName").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun readUri(ctx: Context, uri: Uri): ByteArray? =
        ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
}
