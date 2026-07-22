package com.movtery.zalithlauncher.ui.screens.main.control_editor.edit_widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.movtery.layer_controller.data.JoystickDirection
import com.movtery.layer_controller.event.ClickEvent
import com.movtery.layer_controller.observable.ObservableJoystickData
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.keycodes.ControlEventKeyName
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.MarqueeText
import com.movtery.zalithlauncher.ui.control.Keyboard
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.main.control_editor.InfoLayoutItem
import com.movtery.zalithlauncher.ui.screens.main.control_editor.InfoLayoutTextItem
import com.movtery.zalithlauncher.ui.theme.itemColor
import com.movtery.zalithlauncher.ui.theme.onItemColor

private val directionLabels = mapOf(
    JoystickDirection.East to R.string.control_editor_edit_joystick_direction_east,
    JoystickDirection.NorthEast to R.string.control_editor_edit_joystick_direction_north_east,
    JoystickDirection.North to R.string.control_editor_edit_joystick_direction_north,
    JoystickDirection.NorthWest to R.string.control_editor_edit_joystick_direction_north_west,
    JoystickDirection.West to R.string.control_editor_edit_joystick_direction_west,
    JoystickDirection.SouthWest to R.string.control_editor_edit_joystick_direction_south_west,
    JoystickDirection.South to R.string.control_editor_edit_joystick_direction_south,
    JoystickDirection.SouthEast to R.string.control_editor_edit_joystick_direction_south_east
)

@Composable
fun EditJoystickEvents(
    screenKey: TitledNavKey,
    currentKey: TitledNavKey?,
    data: ObservableJoystickData
) {
    BaseScreen(
        screenKey = screenKey,
        currentKey = currentKey
    ) {
        Column(
            modifier = Modifier
                .padding(start = 4.dp, end = 8.dp)
                .fillMaxSize()
        ) {
            val scrollState = rememberLazyListState()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                state = scrollState
            ) {
                JoystickDirection.entries.filter { it != JoystickDirection.None }.forEach { direction ->
                    item {
                        DirectionEventSection(
                            data = data,
                            direction = direction
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 锁定事件
                item {
                    LockEventSection(data = data)
                }
            }
        }
    }
}

@Composable
private fun DirectionEventSection(
    data: ObservableJoystickData,
    direction: JoystickDirection
) {
    val events = data.directionEvents[direction] ?: emptyList()
    var showKeyboard by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 方向标题
        MarqueeText(
            text = stringResource(directionLabels[direction] ?: R.string.generic_unspecified),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 8.dp)
        )

        // 添加事件按钮
        InfoLayoutTextItem(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.control_editor_edit_event_key_new),
            onClick = { showKeyboard = true },
            showArrow = false
        )

        // 已绑定的事件列表
        events.forEach { event ->
            EventItem(
                event = event,
                onDelete = {
                    val current = data.directionEvents[direction] ?: emptyList()
                    data.directionEvents += (direction to current.filterNot { it == event })
                }
            )
        }

        if (showKeyboard) {
            Keyboard(
                onDismissRequest = { showKeyboard = false },
                isTapMode = true,
                onTap = { selectedKey ->
                    val event = ClickEvent(type = ClickEvent.Type.Key, key = selectedKey)
                    val current = data.directionEvents[direction] ?: emptyList()
                    data.directionEvents += (direction to current + event)
                    showKeyboard = false
                }
            )
        }
    }
}

@Composable
private fun LockEventSection(
    data: ObservableJoystickData
) {
    var showKeyboard by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MarqueeText(
            text = stringResource(R.string.control_editor_edit_joystick_lock_events),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 8.dp)
        )

        InfoLayoutTextItem(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.control_editor_edit_event_key_new),
            onClick = { showKeyboard = true },
            showArrow = false
        )

        data.lockEvents.forEach { event ->
            EventItem(
                event = event,
                onDelete = {
                    data.lockEvents = data.lockEvents.filterNot { it == event }
                }
            )
        }

        if (showKeyboard) {
            Keyboard(
                onDismissRequest = { showKeyboard = false },
                isTapMode = true,
                onTap = { selectedKey ->
                    val event = ClickEvent(type = ClickEvent.Type.Key, key = selectedKey)
                    data.lockEvents = data.lockEvents + event
                    showKeyboard = false
                }
            )
        }
    }
}

@Composable
private fun EventItem(
    event: ClickEvent,
    onDelete: () -> Unit,
    color: Color = itemColor(false),
    contentColor: Color = onItemColor()
) {
    val name = remember(event.key) { ControlEventKeyName.getNameByKey(event.key) }

    InfoLayoutItem(
        modifier = Modifier.fillMaxWidth(),
        onClick = {},
        color = color,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MarqueeText(
                text = stringResource(R.string.control_editor_edit_event_key_value, name ?: event.key),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                painter = painterResource(R.drawable.ic_delete_outlined),
                tint = contentColor,
                contentDescription = stringResource(R.string.generic_delete)
            )
        }
    }
}
