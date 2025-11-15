package com.noreelinsta.settings

import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.noreelinsta.R

class CustomizationSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        bindUtilityActions()
    }

    private fun bindUtilityActions() {
        findPreference<Preference>("pref_clear_data")?.setOnPreferenceClickListener {
            clearWebViewData()
            Toast.makeText(requireContext(), R.string.data_cleared_message, Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun clearWebViewData() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
        WebView(requireContext()).apply {
            clearCache(true)
            clearHistory()
            destroy()
        }
    }
}
