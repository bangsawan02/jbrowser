package fulguris.settings.compose

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import fulguris.R
import fulguris.activity.SettingsActivity
import fulguris.activity.FRAGMENT_CLASS_NAME
import fulguris.extensions.applyWindowInsets

@AndroidEntryPoint
class SettingsComposeActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure edge-to-edge
        applyWindowInsets()

        setContent {
            MaterialTheme(
                colorScheme = dynamicColorScheme(this) ?: MaterialTheme.colorScheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(stringResource(R.string.settings)) },
                                navigationIcon = {
                                    IconButton(onClick = { finish() }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back"
                                        )
                                    }
                                }
                            )
                        }
                    ) { paddingValues ->
                        SettingsList(
                            modifier = Modifier.padding(paddingValues),
                            onNavigateToFragment = { fragmentClass ->
                                val intent = Intent(this@SettingsComposeActivity, SettingsActivity::class.java).apply {
                                    putExtra(FRAGMENT_CLASS_NAME, fragmentClass)
                                }
                                startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}

// Temporary dynamic color fallback
@Composable
fun dynamicColorScheme(context: android.content.Context): ColorScheme? {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
        if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        null
    }
}

data class SettingsItem(
    val titleRes: Int,
    val summaryRes: Int?,
    val iconRes: Int, // Just map directly to R.drawable for now, or ImageVector if needed
    val fragmentClass: String
)

val rootSettingsItems = listOf(
    SettingsItem(R.string.pref_title_appearance, R.string.pref_summary_appearance, R.drawable.ic_palette_outline, "fulguris.settings.fragment.DisplaySettingsFragment"),
    SettingsItem(R.string.pref_title_browser, R.string.pref_summary_browser, R.drawable.ic_web, "fulguris.settings.fragment.GeneralSettingsFragment"),
    SettingsItem(R.string.settings_privacy, R.string.pref_summary_privacy, R.drawable.ic_shield_person_outline, "fulguris.settings.fragment.PrivacySettingsFragment"),
    SettingsItem(R.string.pref_title_domains, R.string.pref_summary_domains, R.drawable.ic_domain, "fulguris.settings.fragment.DomainsSettingsFragment"),
    SettingsItem(R.string.settings_adblock, R.string.pref_summary_adblock, R.drawable.ic_block, "fulguris.settings.fragment.AdBlockSettingsFragment"),
    SettingsItem(R.string.pref_title_extensions, R.string.pref_summary_extensions, R.drawable.ic_extension_outline, "fulguris.settings.fragment.ExtensionsSettingsFragment"),
    SettingsItem(R.string.settings_backup, R.string.pref_summary_backup, R.drawable.ic_backup_outline, "fulguris.settings.fragment.BackupSettingsFragment"),
    SettingsItem(R.string.settings_contribute, R.string.pref_summary_contribute, R.drawable.ic_giftcard, "fulguris.settings.fragment.SponsorshipSettingsFragment"),
    SettingsItem(R.string.settings_about, R.string.pref_summary_about, R.drawable.ic_info, "fulguris.settings.fragment.AboutSettingsFragment")
)

@Composable
fun SettingsList(modifier: Modifier = Modifier, onNavigateToFragment: (String) -> Unit) {
    LazyColumn(modifier = modifier) {
        items(rootSettingsItems) { item ->
            ListItem(
                modifier = Modifier.clickable { onNavigateToFragment(item.fragmentClass) },
                headlineContent = { 
                    Text(
                        text = stringResource(item.titleRes), 
                        style = MaterialTheme.typography.titleMedium
                    ) 
                },
                supportingContent = item.summaryRes?.let {
                    {
                        Text(
                            text = stringResource(it),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                leadingContent = {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = item.iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    }
}
