package com.noreelinsta

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.color.MaterialColors
import com.noreelinsta.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.settingsToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val iconColor = MaterialColors.getColor(
            binding.settingsToolbar,
            com.google.android.material.R.attr.colorOnSurface
        )
        AppCompatResources.getDrawable(this, R.drawable.ic_arrow_back)?.apply {
            setTint(iconColor)
            binding.settingsToolbar.navigationIcon = this
        }
        binding.settingsToolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}
