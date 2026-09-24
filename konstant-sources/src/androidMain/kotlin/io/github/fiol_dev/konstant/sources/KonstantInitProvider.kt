package io.github.fiol_dev.konstant.sources

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

/** Captures the application context at startup so bundled assets can be read without a Context. */
public class KonstantInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        context?.let(::initKonstantAndroid)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
