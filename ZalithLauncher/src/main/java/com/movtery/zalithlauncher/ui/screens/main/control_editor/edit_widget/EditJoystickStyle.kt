package com.movtery.zalithlauncher.ui.screens.main.control_editor.edit_widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.movtery.layer_controller.observable.ObservableJoystickData
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.screens.TitledNavKey

@Composable
fun EditJoystickStyle(
    screenKey: TitledNavKey,
    currentKey: TitledNavKey?,
    data: ObservableJoystickData,
    openStyleList: () -> Unit
) {
    BaseScreen(
        screenKey = screenKey,
        currentKey = currentKey
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

        }
    }
}
