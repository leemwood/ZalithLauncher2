/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.game.sdl;

import android.view.KeyEvent;
import android.view.MotionEvent;

import org.lwjgl.glfw.CallbackBridge;

/**
 * 把 Android 手柄原始事件标准化为 GLFW gamepad 布局，写入共享的 GLFWgamepadstate 缓冲。
 * 游戏 JVM 的 {@code glfwGetGamepadState} 直接 memCopy 同一 native 内存，无需依赖 SDL 手柄事件。
 *
 * 仅在 {@link CallbackBridge#sGamepadDirectInput}（游戏请求过 GLFW gamepad API）后生效。
 * 参考实现（映射逻辑）：https://github.com/AngelAuraMC/Amethyst-Android (customcontrols/gamepad/direct/DirectGamepad)
 */
public final class DirectGamepad {
    // GLFW 标准 gamepad 布局常量（与 GLFW/glfw3.h 一致）
    public static final byte GLFW_RELEASE = 0;
    public static final byte GLFW_PRESS = 1;

    public static final short GLFW_GAMEPAD_BUTTON_A = 0;
    public static final short GLFW_GAMEPAD_BUTTON_B = 1;
    public static final short GLFW_GAMEPAD_BUTTON_X = 2;
    public static final short GLFW_GAMEPAD_BUTTON_Y = 3;
    public static final short GLFW_GAMEPAD_BUTTON_LEFT_BUMPER = 4;
    public static final short GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER = 5;
    public static final short GLFW_GAMEPAD_BUTTON_BACK = 6;
    public static final short GLFW_GAMEPAD_BUTTON_START = 7;
    // Home button, unused because Android takes the home button events for itself
    public static final short GLFW_GAMEPAD_BUTTON_GUIDE = 8;
    public static final short GLFW_GAMEPAD_BUTTON_LEFT_THUMB = 9;
    public static final short GLFW_GAMEPAD_BUTTON_RIGHT_THUMB = 10;
    public static final short GLFW_GAMEPAD_BUTTON_DPAD_UP = 11;
    public static final short GLFW_GAMEPAD_BUTTON_DPAD_RIGHT = 12;
    public static final short GLFW_GAMEPAD_BUTTON_DPAD_DOWN = 13;
    public static final short GLFW_GAMEPAD_BUTTON_DPAD_LEFT = 14;

    public static final short GLFW_GAMEPAD_AXIS_LEFT_X = 0;
    public static final short GLFW_GAMEPAD_AXIS_LEFT_Y = 1;
    public static final short GLFW_GAMEPAD_AXIS_RIGHT_X = 2;
    public static final short GLFW_GAMEPAD_AXIS_RIGHT_Y = 3;
    public static final short GLFW_GAMEPAD_AXIS_LEFT_TRIGGER = 4;
    public static final short GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER = 5;

    /** 按钮判定的阈值（与 AAMC/GLFW 一致） */
    private static final float PRESS_THRESHOLD = 0.85f;

    private DirectGamepad() {
    }

    /**
     * 手柄按键事件入口（{@link KeyEvent#ACTION_DOWN} / {@link KeyEvent#ACTION_UP}）。
     * 不消费事件，调用方继续走 ZL2 既有手柄管线。
     */
    public static boolean handleKeyEvent(KeyEvent event) {
        if (!CallbackBridge.sGamepadDirectInput) return false;
        boolean isDown = event.getAction() == KeyEvent.ACTION_DOWN;
        handleGamepadInput(event.getKeyCode(), isDown ? 1.0f : 0.0f);
        return false;
    }

    /**
     * 手柄摇杆/扳机/十字键运动事件入口（{@link MotionEvent#ACTION_MOVE}）。
     * 不消费事件，调用方继续走 ZL2 既有手柄管线。
     */
    public static boolean handleMotionEvent(MotionEvent event) {
        if (!CallbackBridge.sGamepadDirectInput) return false;
        handleGamepadInput(MotionEvent.AXIS_X, event.getAxisValue(MotionEvent.AXIS_X));
        handleGamepadInput(MotionEvent.AXIS_Y, event.getAxisValue(MotionEvent.AXIS_Y));
        handleGamepadInput(MotionEvent.AXIS_Z, event.getAxisValue(MotionEvent.AXIS_Z));
        handleGamepadInput(MotionEvent.AXIS_RZ, event.getAxisValue(MotionEvent.AXIS_RZ));
        handleGamepadInput(MotionEvent.AXIS_LTRIGGER, event.getAxisValue(MotionEvent.AXIS_LTRIGGER));
        handleGamepadInput(MotionEvent.AXIS_RTRIGGER, event.getAxisValue(MotionEvent.AXIS_RTRIGGER));
        handleGamepadInput(MotionEvent.AXIS_HAT_X, event.getAxisValue(MotionEvent.AXIS_HAT_X));
        handleGamepadInput(MotionEvent.AXIS_HAT_Y, event.getAxisValue(MotionEvent.AXIS_HAT_Y));
        return false;
    }

    /**
     * 与 AAMC DirectGamepad.handleGamepadInput 对齐的映射核心：
     * Android keycode/axis -> GLFW gamepad button/axis，写入共享缓冲。
     */
    public static void handleGamepadInput(int keycode, float value) {
        int gKeycode = -1, gAxis = -1;
        switch (keycode) {
            case KeyEvent.KEYCODE_BUTTON_A: gKeycode = GLFW_GAMEPAD_BUTTON_A; break;
            case KeyEvent.KEYCODE_BUTTON_B: gKeycode = GLFW_GAMEPAD_BUTTON_B; break;
            case KeyEvent.KEYCODE_BUTTON_X: gKeycode = GLFW_GAMEPAD_BUTTON_X; break;
            case KeyEvent.KEYCODE_BUTTON_Y: gKeycode = GLFW_GAMEPAD_BUTTON_Y; break;
            case KeyEvent.KEYCODE_BUTTON_L1: gKeycode = GLFW_GAMEPAD_BUTTON_LEFT_BUMPER; break;
            case KeyEvent.KEYCODE_BUTTON_R1: gKeycode = GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER; break;
            case KeyEvent.KEYCODE_BUTTON_L2:
            case MotionEvent.AXIS_LTRIGGER:
                gAxis = GLFW_GAMEPAD_AXIS_LEFT_TRIGGER;
                break;
            case KeyEvent.KEYCODE_BUTTON_R2:
            case MotionEvent.AXIS_RTRIGGER:
                gAxis = GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER;
                break;
            case KeyEvent.KEYCODE_BUTTON_THUMBL: gKeycode = GLFW_GAMEPAD_BUTTON_LEFT_THUMB; break;
            case KeyEvent.KEYCODE_BUTTON_THUMBR: gKeycode = GLFW_GAMEPAD_BUTTON_RIGHT_THUMB; break;
            case KeyEvent.KEYCODE_BUTTON_START: gKeycode = GLFW_GAMEPAD_BUTTON_START; break;
            case KeyEvent.KEYCODE_BUTTON_SELECT: gKeycode = GLFW_GAMEPAD_BUTTON_BACK; break;
            case KeyEvent.KEYCODE_DPAD_UP: gKeycode = GLFW_GAMEPAD_BUTTON_DPAD_UP; break;
            case KeyEvent.KEYCODE_DPAD_DOWN: gKeycode = GLFW_GAMEPAD_BUTTON_DPAD_DOWN; break;
            case KeyEvent.KEYCODE_DPAD_LEFT: gKeycode = GLFW_GAMEPAD_BUTTON_DPAD_LEFT; break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: gKeycode = GLFW_GAMEPAD_BUTTON_DPAD_RIGHT; break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                // GLFW 没有 dpad center 键位，行为与 AAMC 一致：释放全部 dpad
                CallbackBridge.sGamepadButtonBuffer.put(GLFW_GAMEPAD_BUTTON_DPAD_UP, GLFW_RELEASE);
                CallbackBridge.sGamepadButtonBuffer.put(GLFW_GAMEPAD_BUTTON_DPAD_DOWN, GLFW_RELEASE);
                CallbackBridge.sGamepadButtonBuffer.put(GLFW_GAMEPAD_BUTTON_DPAD_LEFT, GLFW_RELEASE);
                CallbackBridge.sGamepadButtonBuffer.put(GLFW_GAMEPAD_BUTTON_DPAD_RIGHT, GLFW_RELEASE);
                return;
            case MotionEvent.AXIS_X: gAxis = GLFW_GAMEPAD_AXIS_LEFT_X; break;
            case MotionEvent.AXIS_Y: gAxis = GLFW_GAMEPAD_AXIS_LEFT_Y; break;
            case MotionEvent.AXIS_Z: gAxis = GLFW_GAMEPAD_AXIS_RIGHT_X; break;
            case MotionEvent.AXIS_RZ: gAxis = GLFW_GAMEPAD_AXIS_RIGHT_Y; break;
            case MotionEvent.AXIS_HAT_X:
                CallbackBridge.sGamepadButtonBuffer.put(
                        GLFW_GAMEPAD_BUTTON_DPAD_LEFT,
                        value < -PRESS_THRESHOLD ? GLFW_PRESS : GLFW_RELEASE
                );
                CallbackBridge.sGamepadButtonBuffer.put(
                        GLFW_GAMEPAD_BUTTON_DPAD_RIGHT,
                        value > PRESS_THRESHOLD ? GLFW_PRESS : GLFW_RELEASE
                );
                return;
            case MotionEvent.AXIS_HAT_Y:
                CallbackBridge.sGamepadButtonBuffer.put(
                        GLFW_GAMEPAD_BUTTON_DPAD_UP,
                        value < -PRESS_THRESHOLD ? GLFW_PRESS : GLFW_RELEASE
                );
                CallbackBridge.sGamepadButtonBuffer.put(
                        GLFW_GAMEPAD_BUTTON_DPAD_DOWN,
                        value > PRESS_THRESHOLD ? GLFW_PRESS : GLFW_RELEASE
                );
                return;
            default:
                return; // 未知 axis（如 PRESSURE/SIZE）直接忽略
        }
        if (gKeycode != -1) {
            CallbackBridge.sGamepadButtonBuffer.put(gKeycode, value > PRESS_THRESHOLD ? GLFW_PRESS : GLFW_RELEASE);
        }
        if (gAxis != -1) {
            CallbackBridge.sGamepadAxisBuffer.put(gAxis, value);
        }
    }
}