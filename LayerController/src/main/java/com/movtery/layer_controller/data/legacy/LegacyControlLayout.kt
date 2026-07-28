package com.movtery.layer_controller.data.legacy

import com.movtery.layer_controller.data.JoystickStyle
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 旧版编辑器布局（版本 8~11）中 special 字段
 */
@Serializable
data class LegacySpecial(
    @SerialName("joystickStyle")
    val joystickStyle: LegacyJoystickStyle? = null
)

/**
 * 旧版 [JoystickStyle]
 */
@Serializable
data class LegacyJoystickStyle(
    @SerialName("uuid")
    val uuid: String,
    @SerialName("lightStyle")
    val lightStyle: JoystickStyle.StyleConfig,
    @SerialName("darkStyle")
    val darkStyle: JoystickStyle.StyleConfig
) {
    fun toJoystickStyle(): JoystickStyle = JoystickStyle(
        name = "Legacy",
        uuid = uuid,
        commonStyle = true,
        lightStyle = lightStyle,
        darkStyle = darkStyle
    )
}
