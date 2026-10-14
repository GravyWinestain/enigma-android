// MainActivity.kt - entry point, hosting the machine.
//
// The panel fills the window and the dark case colour is painted behind it, so
// there is no flash of the system background when the app opens.

package com.gnomeworx.enigma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.gnomeworx.enigma.ui.EnigmaScreen
import com.gnomeworx.enigma.ui.theme.EnigmaTheme
import com.gnomeworx.enigma.ui.theme.PanelBg

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge to edge, so the case colour runs under the status bar the way a
        // physical panel would run to the edge of its case.
        enableEdgeToEdge()
        setContent {
            EnigmaTheme {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PanelBg),
                ) {
                    EnigmaScreen()
                }
            }
        }
    }
}
