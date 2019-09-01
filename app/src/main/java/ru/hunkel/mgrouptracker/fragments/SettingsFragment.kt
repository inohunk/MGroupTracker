package ru.hunkel.mgrouptracker.fragments


import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import ru.hunkel.mgrouptracker.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}
