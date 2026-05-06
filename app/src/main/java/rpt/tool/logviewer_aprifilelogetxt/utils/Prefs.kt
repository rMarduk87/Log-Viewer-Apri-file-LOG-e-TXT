package rpt.tool.logviewer_aprifilelogetxt.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri

object Prefs {
    private const val PREF_NAME = "log_viewer_prefs"
    private const val KEY_LAST_URI = "last_uri"
    private const val KEY_BOOKMARKS = "bookmarks"
    private const val KEY_ONBOARDING = "onboarding_done"

    fun saveLastUri(context: Context, uri: Uri) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit { putString(KEY_LAST_URI, uri.toString()) }
    }

    fun getLastUri(context: Context): Uri? {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LAST_URI, null)?.toUri()
    }

    fun saveBookmarks(context: Context, ids: Set<Int>) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit { putStringSet(KEY_BOOKMARKS, ids.map { it.toString() }.toSet()) }
    }

    fun loadBookmarks(context: Context): Set<Int> {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_BOOKMARKS, emptySet())
            ?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    fun setOnboardingDone(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_ONBOARDING, true) }
    }

    fun isOnboardingDone(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING, false)
    }
}