/*
 * The contents of this file are subject to the Common Public Attribution License Version 1.0.
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 * https://github.com/Slion/Fulguris/blob/main/LICENSE.CPAL-1.0.
 * The License is based on the Mozilla Public License Version 1.1, but Sections 14 and 15 have been
 * added to cover use of software over a computer network and provide for limited attribution for
 * the Original Developer. In addition, Exhibit A has been modified to be consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * The Original Code is Fulguris.
 *
 * The Original Developer is the Initial Developer.
 * The Initial Developer of the Original Code is Stéphane Lenclud.
 *
 * All portions of the code written by Stéphane Lenclud are Copyright © 2020 Stéphane Lenclud.
 * All Rights Reserved.
 */

package fulguris.cursor

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Choreographer
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import java.lang.reflect.Method
import kotlin.math.abs
import timber.log.Timber

/**
 * Owns the whole on-screen cursor: its position and velocity, the frame loop that moves it from
 * D-pad and analog-joystick input, and the construction/dispatch of synthetic pointer
 * [MotionEvent]s into the target view.
 *
 * Web pages get real `:hover` / `mouseover` / `mouseout` from continuous SOURCE_MOUSE
 * `ACTION_HOVER_MOVE` events as the cursor moves; the click is a precise touch DOWN→MOVE→UP at the
 * cursor coordinate (synthetic mouse *button* events can't be turned into a page click through the
 * public API on Android WebView — see [dispatchClick]). Reaching a WebView edge dispatches a
 * synthetic mouse **wheel** ([MotionEvent.ACTION_SCROLL]) at the cursor point so whichever DOM
 * element is under the cursor (including nested scrollable panels) scrolls, like a real mouse wheel.
 *
 * ## Two independent ways to drive the cursor
 *  - **Cursor on** ([enabled]): toggled with the hotkey / menu. While on, the **D-pad** moves the
 *    cursor and the select button clicks. This is the path for D-pad-only remotes and single-stick
 *    joysticks, where the D-pad would otherwise do focus navigation. One exception: a D-pad key
 *    that comes from a *two-stick* gamepad is always yielded back to focus navigation — on such a
 *    device the right stick (below) is the cursor's intended driver, so the D-pad keeps its normal
 *    role even with the cursor on.
 *  - **Right analog stick** ([onGenericMotionEvent]): on a two-stick gamepad the right stick moves
 *    the cursor at any time, *without* toggling the cursor on — the left stick still scrolls and
 *    the D-pad still does focus navigation (including while the cursor is on). The select button
 *    clicks whenever the cursor is [shown].
 *
 * The cursor fades out after [CursorSettings.fadeTimeoutSec] seconds of no movement and fades back in on any
 * movement.
 *
 * ## Movement is physical, not pixel-based
 * Speed and acceleration are expressed in cm/s and cm/s² and converted to pixels using the display's
 * DPI ([pxPerCm]), so a given setting feels comparable regardless of screen resolution / size.
 *
 * ## Boundary
 * The controller is deliberately decoupled from the browser activity. It only knows about:
 *  - [overlay]: the [CursorView] it renders into (and whose bounds it clamps to);
 *  - [targetProvider]: a way to fetch the view to dispatch into (the current WebView, or the
 *    fullscreen custom view while an HTML5 video is fullscreen), re-queried on every dispatch;
 *  - [settings]: [CursorSettings] for the hotkey / speed / acceleration / fade timeout;
 *  - [onCursorToggled]: notified when the cursor is toggled on/off (the activity shows feedback and moves focus).
 *
 * The activity forwards [dispatchKeyEvent] / [onGenericMotionEvent], adds (and, for fullscreen,
 * re-parents) the overlay view, and provides the target — nothing else. This keeps the component
 * reusable / lib-extractable.
 *
 * ## Toggle hotkey
 * A **long press** of [KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE] (held for [HOTKEY_LONG_PRESS_MS]) toggles
 * the cursor on/off; we detect it ourselves (media keys don't reliably deliver system long-press). A
 * **short** press is yielded back to the activity (returns false) so it can play/pause the page's
 * video. The activity forwards the key to us before it can reach a page's `MediaSession`.
 *
 * ## Media keys as a wheel
 * While the cursor is on screen, [KeyEvent.KEYCODE_MEDIA_FAST_FORWARD] / [KeyEvent.KEYCODE_MEDIA_REWIND]
 * dispatch a synthetic mouse wheel scroll up / down at the cursor point (see [dispatchScroll]); when
 * the cursor is off, they fall through to the activity's per-video seek.
 *
 * ## Context menu via the action key
 * While the cursor is on screen, a **deliberate long press** of the action key
 * ([KeyEvent.KEYCODE_DPAD_CENTER] / [KeyEvent.KEYCODE_ENTER] / [KeyEvent.KEYCODE_BUTTON_A] —
 * see [isConfirmKey], held for [settings.actionHoldSec], user-configurable between 0.5 and
 * 1 s) performs a long press at the cursor and so
 * opens the WebView's built-in context menu for the element under it (the same menu a touch long
 * press would open). A **short** press is the normal click at the cursor: the click is deferred to
 * the key UP so a held press can still be reclassified as a long press while it is held.
 *
 * The threshold is deliberately ~1 s and *not* the system long-press timeout: a human "short
 * click" on a remote is routinely held 400-700 ms — past the ~400 ms at which the OS starts
 * flagging the key with [KeyEvent.FLAG_LONG_PRESS] — and the system flag must therefore be
 * ignored here, or hesitant clicks would open the context menu instead of clicking.
 *
 * With the cursor off the action key falls through to its normal meaning.
 */
class CursorController(
    private val overlay: CursorView,
    private val targetProvider: () -> View?,
    private val settings: CursorSettings,
    private val onCursorToggled: (enabled: Boolean) -> Unit,
) {

    // Cursor on: D-pad drives the cursor and select clicks. Independent of [shown].
    var enabled: Boolean = false
        private set

    // Whether the cursor overlay is currently faded in (visible). Driven by the cursor being on OR
    // the right stick, and cleared by the fade-out timeout.
    private var shown = false

    // Whether the cursor has ever been placed (so re-enabling doesn't recenter a right-stick cursor).
    private var positioned = false

    // Logical cursor position, in overlay-local pixels.
    private var posX = 0f
    private var posY = 0f

    // Active D-pad directions (each -1, 0 or +1). Only set while the cursor is on.
    private var keyDx = 0
    private var keyDy = 0

    // Right-stick displacement after deadzone, -1..1. Drives the cursor regardless of whether the cursor is on.
    private var rsX = 0f
    private var rsY = 0f

    // When the current continuous-movement gesture started, for the acceleration ramp.
    private var moveStartMs = 0L

    // Last known overlay size. Cached so movement still works while the overlay is faded out (GONE),
    // when its measured width/height read back as 0.
    private var boundsX = 0f
    private var boundsY = 0f

    private val handler = Handler(Looper.getMainLooper())
    private val choreographer = Choreographer.getInstance()
    private var looping = false
    private var lastFrameNs = 0L

    // --- Toggle hotkey state ------------------------------------------------

    private var hotkeyDownHandled = false
    private val hotkeyLongPress = Runnable {
        hotkeyDownHandled = true
        toggle()
    }

    // --- Context-menu (action-key long press) state --------------------------
    // A short press of the action key is a click at the cursor (fired on the key UP); a deliberate
    // hold of [actionLongPressMs] (user setting, clamped to at least 500 ms) opens the context
    // menu. The DOWN arms the timer, the UP
    // resolves the press: if the timer already fired for this press, the UP is consumed. The OS's
    // own long-press flag (raised after the ~400 ms system timeout) is deliberately NOT honored
    // here — a human "short click" is routinely held that long, and acting on it would open the
    // menu instead of clicking.

    private var actionLongPressHandled = false
    // Deliberate-hold threshold for the context menu, from the user setting (seconds, clamped so
    // it stays well past the ~400 ms system long-press flag — see the class KDoc), in ms.
    private val actionLongPressMs: Long
        get() = (settings.actionHoldSec.coerceAtLeast(settings.minActionHoldSec) * 1000f).toLong()
    private val actionLongPress = Runnable {
        actionLongPressHandled = true
        dispatchLongPress()
    }

    // Diagnostic: accumulates the raw action-key event sequence of the current press
    // ("d23(r0)" = DOWN keycode 23 repeat 0, "u23" = UP ...) and logs it when the press
    // resolves on UP. This makes a misbehaving remote's event shape (a duplicated DOWN or
    // extra UP for one physical press, e.g. some Bluetooth remotes) visible in logcat.
    private val actionPressEvents = mutableListOf<String>()

    // --- Fade state ---------------------------------------------------------

    private val fadeRunnable = Runnable { hideCursor() }

    // ------------------------------------------------------------------------

    /**
     * Forward the activity's key events here first. Returns true when consumed.
     */
    fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // The toggle hotkey is a long press of play/pause, handled whether or not the cursor is
        // currently on. A short press is yielded back (returns false) so the activity can play/pause
        // the page's video.
        if (event.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            return handleHotkey(event)
        }

        // While the cursor is on screen, fast-forward / rewind become a mouse wheel scroll at the
        // cursor (up / down respectively); off-cursor they fall through to the activity's video seek.
        if ((enabled || shown) &&
            (event.keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD || event.keyCode == KeyEvent.KEYCODE_MEDIA_REWIND)) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                wakeCursor()
                val notches = if (event.keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) WHEEL_NOTCHES else -WHEEL_NOTCHES
                dispatchScroll(notches, 0f)
            }
            return true
        }

        // The action key (select / DPAD center / ENTER / BUTTON_A) drives the primary interaction
        // whenever the cursor is on screen — whether it got there via the hotkey toggle or the right
        // stick. A short press is a click at the cursor; a long press performs a long press there
        // and so opens the WebView's context menu for the element under the cursor. The click
        // fires on ACTION_UP, so a DOWN arms the long-press timer and the UP resolves the press.
        if (isConfirmKey(event.keyCode) && (enabled || shown)) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    actionPressEvents += "d${event.keyCode}(r${event.repeatCount})"
                    if (actionLongPressHandled) return true // already fired for this press
                    if (event.repeatCount == 0) {
                        handler.removeCallbacks(actionLongPress)
                        handler.postDelayed(actionLongPress, actionLongPressMs)
                    }
                    // Note: we intentionally do NOT react to event.isLongPress here — the OS
                    // raises it after the ~400 ms system timeout, and a human "short click" is
                    // routinely held that long (see the class KDoc). Only the deliberate
                    // actionLongPressMs hold counts.
                }
                KeyEvent.ACTION_UP -> {
                    actionPressEvents += "u${event.keyCode}"
                    handler.removeCallbacks(actionLongPress)
                    val longPressed = actionLongPressHandled
                    actionLongPressHandled = false
                    Timber.d("Cursor: action-key press resolved longPress=$longPressed events=[${actionPressEvents.joinToString("")}]")
                    actionPressEvents.clear()
                    if (!longPressed) dispatchClick()
                }
            }
            return true
        }

        // The D-pad only drives the cursor while the cursor is explicitly on; otherwise it
        // must fall through to normal focus navigation.
        if (!enabled) return false

        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                // A directional key coming from a *two-stick* gamepad is yielded back even with
                // the cursor on: on such a device the right analog stick is the cursor's
                // intended driver (it works at any time, cursor on or off), so the D-pad keeps
                // its normal role (focus navigation) rather than also steering the cursor.
                // D-pad keys from a D-pad-only remote — or from adb's virtual keyboard, which
                // has no right stick — still drive the cursor.
                if (hasRightStick(event.device)) {
                    Timber.d("Cursor: yielding D-pad ${event.keyCode} from a two-stick gamepad")
                    return false
                }
                handleDirectionKey(event)
                true
            }
            else -> false
        }
    }

    private fun isConfirmKey(keyCode: Int): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER,
        KeyEvent.KEYCODE_BUTTON_A -> true
        else -> false
    }

    /**
     * Forward the activity's generic motion events here. The **right analog stick** drives the
     * cursor at any time (no cursor toggle needed) on two-stick gamepads. Always returns false
     * (non-consuming) so the left stick's scroll and D-pad focus navigation are left untouched — the
     * right stick's Z/RZ axes aren't used by either of those.
     */
    fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.source and InputDevice.SOURCE_CLASS_JOYSTICK == 0) return false
        if (event.action != MotionEvent.ACTION_MOVE) return false
        // Only if the device actually has a centered right stick (exclude devices that report
        // triggers on Z/RZ, whose range is 0..1 rather than -1..1).
        if (!hasRightStick(event.device)) return false

        rsX = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_Z))
        rsY = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_RZ))
        if (rsX != 0f || rsY != 0f) {
            if (!hasMovementInput(exceptRightStick = true)) moveStartMs = SystemClock.uptimeMillis()
            wakeCursor()
            startLoop()
        } else {
            maybeStopLoop()
        }
        return false
    }

    /** Toggle the cursor on/off. Safe to call from the menu or the hotkey. */
    fun toggle() {
        if (enabled) disable() else enable()
    }

    fun enable() {
        if (enabled) return
        enabled = true
        Timber.d("Cursor: enable")
        // Make the overlay visible now so it gets laid out before we read its size to center.
        overlay.visibility = View.VISIBLE
        overlay.post {
            // Center the cursor the first time it appears; keep its place if the right stick already
            // positioned it, so toggling the cursor on doesn't make it jump.
            if (!positioned && overlay.maxX > 0f && overlay.maxY > 0f) {
                posX = overlay.maxX / 2f
                posY = overlay.maxY / 2f
                positioned = true
                overlay.setPosition(posX, posY)
                Timber.d("Cursor: centered at ($posX, $posY) in overlay ${overlay.maxX}x${overlay.maxY}")
            }
            wakeCursor()
            dispatchHover()
        }
        onCursorToggled(true)
    }

    fun disable() {
        if (!enabled) return
        enabled = false
        Timber.d("Cursor: disable")
        keyDx = 0; keyDy = 0
        // Keep the right stick able to move it; if nothing is driving it, let it hide.
        if (!hasMovementInput()) {
            hideCursor()
            stopLoop()
        }
        onCursorToggled(false)
    }

    /** Detach lifecycle hooks; call from the activity's onDestroy. */
    fun release() {
        handler.removeCallbacks(hotkeyLongPress)
        handler.removeCallbacks(actionLongPress)
        handler.removeCallbacks(fadeRunnable)
        stopLoop()
    }

    // --- Hotkey -------------------------------------------------------------

    private fun handleHotkey(event: KeyEvent): Boolean {
        if (!settings.hotkeyEnabled) return false
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (hotkeyDownHandled) return true // already toggled for this press
                if (event.repeatCount == 0) {
                    handler.removeCallbacks(hotkeyLongPress)
                    handler.postDelayed(hotkeyLongPress, HOTKEY_LONG_PRESS_MS)
                }
                // Some remotes (and `adb shell input keyevent --longpress`) also deliver a system
                // long-press event for media keys; honor it as a secondary trigger. Our own timer
                // above remains the primary, reliable path.
                if (event.isLongPress) {
                    handler.removeCallbacks(hotkeyLongPress)
                    hotkeyDownHandled = true
                    toggle()
                }
                // Consume DOWN so it never reaches a page MediaSession while we time the long press.
                return true
            }
            KeyEvent.ACTION_UP -> {
                handler.removeCallbacks(hotkeyLongPress)
                val toggled = hotkeyDownHandled
                hotkeyDownHandled = false
                // A completed long press (toggle) is consumed; a short press is yielded back to the
                // activity (returns false) so it can seek the page's video.
                return toggled
            }
        }
        return true
    }

    // --- Movement -----------------------------------------------------------

    private fun handleDirectionKey(event: KeyEvent) {
        val (ax, ay) = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> -1 to 0
            KeyEvent.KEYCODE_DPAD_RIGHT -> 1 to 0
            KeyEvent.KEYCODE_DPAD_UP -> 0 to -1
            KeyEvent.KEYCODE_DPAD_DOWN -> 0 to 1
            else -> 0 to 0
        }
        if (event.action == KeyEvent.ACTION_DOWN) {
            val wasIdle = !hasMovementInput()
            if (ax != 0) keyDx = ax
            if (ay != 0) keyDy = ay
            if (wasIdle) moveStartMs = SystemClock.uptimeMillis()
            // Move a fixed physical step on the initial press so a single tap (which releases almost
            // instantly, e.g. a discrete remote press) always nudges the cursor; the frame loop then
            // adds continuous, accelerating movement while the key stays held. Only on repeatCount 0
            // so auto-repeat DOWNs don't double up with the loop.
            if (event.repeatCount == 0) {
                val (pxCmX, pxCmY) = pxPerCm()
                val stepCm = STEP_MIN_CM + (settings.speed.coerceIn(1f, 100f) / 100f) * (STEP_MAX_CM - STEP_MIN_CM)
                moveBy(ax * stepCm * pxCmX, ay * stepCm * pxCmY)
            }
            startLoop()
        } else if (event.action == KeyEvent.ACTION_UP) {
            if (ax != 0 && keyDx == ax) keyDx = 0
            if (ay != 0 && keyDy == ay) keyDy = 0
            if (!hasMovementInput()) maybeStopLoop()
        }
    }

    private val frameCallback = Choreographer.FrameCallback { frameTimeNs -> onFrame(frameTimeNs) }

    private fun startLoop() {
        if (looping) return
        looping = true
        lastFrameNs = 0L
        choreographer.postFrameCallback(frameCallback)
    }

    private fun stopLoop() {
        looping = false
        choreographer.removeFrameCallback(frameCallback)
    }

    private fun maybeStopLoop() {
        if (!hasMovementInput()) stopLoop()
    }

    private fun hasMovementInput(exceptRightStick: Boolean = false): Boolean {
        if (keyDx != 0 || keyDy != 0) return true
        if (!exceptRightStick && (rsX != 0f || rsY != 0f)) return true
        return false
    }

    private fun onFrame(frameTimeNs: Long) {
        if (!looping) return
        val dt = if (lastFrameNs == 0L) 0f else (frameTimeNs - lastFrameNs) / 1_000_000_000f
        lastFrameNs = frameTimeNs

        if (hasMovementInput()) {
            // Keep the cursor awake while it is actively moving (a held stick may not emit new
            // motion events, so we reset the fade timer here rather than only on input events).
            wakeCursor()

            val (pxCmX, pxCmY) = pxPerCm()
            val baseCmS = baseSpeedCmPerSec()
            val accelCmS2 = accelCmPerSec2()
            val held = (SystemClock.uptimeMillis() - moveStartMs) / 1000f
            val speedCmS = (baseCmS + accelCmS2 * held).coerceAtMost(baseCmS * MAX_SPEED_MULT)

            // D-pad contributes ±1 per axis; right stick contributes its analog displacement.
            val dirX = (keyDx + rsX).coerceIn(-1.5f, 1.5f)
            val dirY = (keyDy + rsY).coerceIn(-1.5f, 1.5f)
            val dxPx = dirX * speedCmS * pxCmX * dt
            val dyPx = dirY * speedCmS * pxCmY * dt
            if (dxPx != 0f || dyPx != 0f) moveBy(dxPx, dyPx)
        }

        if (looping) choreographer.postFrameCallback(frameCallback)
    }

    private fun moveBy(dx: Float, dy: Float) {
        // Keep the last real size so movement still works once the cursor has faded out (GONE),
        // whose measured size reads back as 0.
        if (overlay.maxX > 0f) boundsX = overlay.maxX
        if (overlay.maxY > 0f) boundsY = overlay.maxY
        val maxX = boundsX
        val maxY = boundsY
        if (maxX <= 0f || maxY <= 0f) return

        // First movement (e.g. from the right stick before the cursor was ever toggled on) starts
        // from the center rather than the top-left corner.
        if (!positioned) {
            posX = maxX / 2f
            posY = maxY / 2f
            positioned = true
        }

        var nx = posX + dx
        var ny = posY + dy

        // At an edge, keep pushing translates into a mouse-wheel scroll at the cursor point (so a
        // nested scrollable region under the cursor scrolls); the cursor itself stays clamped.
        var overflowX = 0f
        var overflowY = 0f
        if (nx < 0f) { overflowX = nx; nx = 0f }
        else if (nx > maxX) { overflowX = nx - maxX; nx = maxX }
        if (ny < 0f) { overflowY = ny; ny = 0f }
        else if (ny > maxY) { overflowY = ny - maxY; ny = maxY }

        posX = nx
        posY = ny
        positioned = true
        overlay.setPosition(posX, posY)
        // Any movement makes the cursor visible and restarts its fade-out countdown.
        wakeCursor()
        dispatchHover()

        if (overflowX != 0f || overflowY != 0f) {
            // Wheel "notches": pushing down/right scrolls the content the same way a real wheel does.
            dispatchScroll(-overflowY / SCROLL_PX_PER_NOTCH, -overflowX / SCROLL_PX_PER_NOTCH)
        }
    }

    // --- Synthetic pointer events -------------------------------------------

    /** Map the overlay-local cursor position into the target view's coordinate space. */
    private fun targetCoords(target: View): Pair<Float, Float> {
        val t = IntArray(2); target.getLocationOnScreen(t)
        val o = IntArray(2); overlay.getLocationOnScreen(o)
        return (posX + o[0] - t[0]) to (posY + o[1] - t[1])
    }

    private fun dispatchHover(xOffset: Float = 0f) {
        val target = targetProvider() ?: return
        val (x, y) = targetCoords(target)
        val now = SystemClock.uptimeMillis()
        val event = obtainMouseEvent(now, now, MotionEvent.ACTION_HOVER_MOVE, x + xOffset, y, 0)
        try {
            target.dispatchGenericMotionEvent(event)
        } finally {
            event.recycle()
        }
    }

    private fun dispatchClick() {
        val target = targetProvider() ?: return
        wakeCursor()
        val (x, y) = targetCoords(target)
        Timber.d("Cursor: click at target ($x, $y)")
        dispatchHover()
        handler.postDelayed({
            val t = targetProvider() ?: return@postDelayed
            if (!dispatchMouseClick(t, x, y)) dispatchTouchClick(t, x, y)
        }, CLICK_DELAY_MS)
    }

    /**
     * Open the WebView's built-in context menu for the element under the cursor by performing a
     * long press. Like [dispatchClick] this is a synthetic **touch** (not a mouse button) — the
     * WebView's long-press detector (and thus its context menu) only reacts to the touch path.
     *
     * The hold is terminated with a **late ACTION_CANCEL**, dispatched *after* the long-press
     * dialog has appeared. Why, measured against the real input pipeline (DOM event log in
     * `scripts/tests/assets/longpress_log.html`):
     *
     *  - A real finger produces `ts … pc/ctx@~540ms tc@~564ms`. At ~560 ms the dialog window
     *    steals window focus (logcat: `onWindowFocusChanged` at ~561 ms after DOWN), and the
     *    input pipeline — which is tracking the in-progress touch — delivers the `tc`
     *    (touchcancel) to the WebView. That cancel cleanly releases the renderer's long-press
     *    input state; real long presses never wedge the page.
     *  - Our synthetic DOWN is dispatched straight to the view, bypassing the input pipeline,
     *    so the pipeline never sees it and never delivers that focus-loss cancel. The renderer
     *    keeps the touch's long-press state active. Terminating with an **UP** wedges that
     *    state: after ~2 long presses the page stops receiving *any* input — even real
     *    hardware taps — until a reload, and a late UP can be read as a stray click (the
     *    original "context menu opens and the page also navigates" symptom).
     *  - A **CANCEL** also has to land *after* the dialog has appeared: one sent early
     *    (~550 ms, before the focus change at ~561 ms) wedged the state just like an UP.
     *    Hence the delay: the cancel goes out at [longPressHoldMs] + [LONGPRESS_LATE_CANCEL_MS]
     *    (~850 ms), comfortably past the dialog's focus steal.
     *
     * The events carry the **same provenance as real touchscreen events** —
     * [InputDevice.SOURCE_TOUCHSCREEN] and [TOUCH_DEVICE_ID] — built with the full
     * [MotionEvent.obtain] constructor. The renderer matches in-progress touches by
     * device/pointer identity: a cancel built with the deprecated
     * `obtain(downTime, eventTime, action, x, y, metaState)` constructor (source
     * SOURCE_UNKNOWN, deviceId -1) is *not* matched to the tracked touch and is ignored,
     * which wedged the gesture state exactly like an UP. With matching provenance the
     * cancel is honored, the same as the pipeline's focus-loss cancel on a real touch.
     * A CANCEL (unlike an UP) can never produce a page click, and it is the exact event
     * the real pipeline sends in this situation — so repeated long presses leave the
     * renderer clean. Regression test:
     * `test_cursor_context_menu_repeated_long_press_touch_stays_clean`.
     */
    private fun dispatchLongPress() {
        val target = targetProvider() ?: return
        wakeCursor()
        val (x, y) = targetCoords(target)
        Timber.d("Cursor: long press (context menu) at target ($x, $y)")
        dispatchHover()
        val downTime = SystemClock.uptimeMillis()
        val down = touchEvent(downTime, downTime, MotionEvent.ACTION_DOWN, x, y)
        try {
            target.dispatchTouchEvent(down)
        } finally {
            down.recycle()
        }
        // Terminate the gesture with a CANCEL once the dialog is on screen (see the KDoc). The
        // posted delay starts counting from now, so the total hold is
        // longPressHoldMs() + LONGPRESS_LATE_CANCEL_MS.
        val cancelMs = longPressHoldMs() + LONGPRESS_LATE_CANCEL_MS
        handler.postDelayed({
            // Only terminate the gesture on the very view that received the DOWN. If the tab
            // changed in the window, an orphan cancel would land on a fresh WebView and could
            // corrupt its touch state; the old view is going away anyway.
            val t = targetProvider() ?: return@postDelayed
            if (t !== target) return@postDelayed
            val cancel = touchEvent(downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_CANCEL, x, y)
            try {
                t.dispatchTouchEvent(cancel)
            } finally {
                cancel.recycle()
            }
            Timber.d("Cursor: long press CANCEL dispatched (%d ms after DOWN)", cancelMs)
        }, cancelMs)
    }

    /**
     * A touch [MotionEvent] with the provenance of a real touchscreen event:
     * [InputDevice.SOURCE_TOUCHSCREEN] and a real device id (0 — the id InputDispatcher
     * uses for the primary touchscreen). The renderer's gesture tracker matches
     * in-progress touches by device/pointer identity, so a follow-up event with a
     * different source/deviceId (e.g. from the deprecated
     * `MotionEvent.obtain(downTime, eventTime, action, x, y, metaState)` constructor)
     * does not reach the tracked gesture.
     */
    private fun touchEvent(downTime: Long, eventTime: Long, action: Int, x: Float, y: Float): MotionEvent =
        MotionEvent.obtain(
            downTime, eventTime, action, 1,
            arrayOf(MotionEvent.PointerProperties().also { it.id = 0; it.toolType = MotionEvent.TOOL_TYPE_FINGER }),
            arrayOf(MotionEvent.PointerCoords().also { it.x = x; it.y = y; it.pressure = 1f; it.size = 1f }),
            0, 0, 1f, 1f, TOUCH_DEVICE_ID, 0,
            InputDevice.SOURCE_TOUCHSCREEN, 0
        )

    /**
     * How long to hold the long-press touch DOWN before the terminating CANCEL is scheduled
     * (ms). This is the base hold; [dispatchLongPress] adds [LONGPRESS_LATE_CANCEL_MS] so the
     * cancel lands *after* the long-press dialog has appeared and stolen window focus.
     */
    private fun longPressHoldMs(): Long =
        (ViewConfiguration.getLongPressTimeout() + LONGPRESS_MARGIN_MS).toLong()

    // setActionButton is @hide but needed for ACTION_BUTTON_PRESS/RELEASE (api=unsupported,test-api → allowed).
    private val setActionButtonMethod: Method? by lazy {
        try {
            MotionEvent::class.java.getDeclaredMethod("setActionButton", Int::class.java)
                .apply { isAccessible = true }
        } catch (_: Exception) { null }
    }

    private fun dispatchMouseClick(target: View, x: Float, y: Float): Boolean {
        val setter = setActionButtonMethod ?: return false
        val dt = SystemClock.uptimeMillis()
        val pp = arrayOf(MotionEvent.PointerProperties().also { it.id = 0; it.toolType = MotionEvent.TOOL_TYPE_MOUSE })
        fun at(px: Float) = arrayOf(MotionEvent.PointerCoords().also { it.x = px; it.y = y; it.pressure = 1f; it.size = 1f })
        fun ev(t: Long, action: Int, btns: Int, px: Float) =
            MotionEvent.obtain(dt, t, action, 1, pp, at(px), 0, btns, 1f, 1f, -1, 0, InputDevice.SOURCE_MOUSE, 0)
        // Exact event sequence from a real Bluetooth mouse (all via dispatchGenericMotionEvent):
        // ACTION_HOVER_EXIT → ACTION_DOWN → ACTION_BUTTON_PRESS → [MOVE] → ACTION_BUTTON_RELEASE → ACTION_UP
        // It is ACTION_BUTTON_PRESS with actionButton=BUTTON_PRIMARY that Chromium translates to mousedown(button=0).
        val hoverExit = ev(dt,    MotionEvent.ACTION_HOVER_EXIT,     MotionEvent.BUTTON_PRIMARY, x)
        val down      = ev(dt+10, MotionEvent.ACTION_DOWN,           MotionEvent.BUTTON_PRIMARY, x)
        val btnPress  = ev(dt+20, MotionEvent.ACTION_BUTTON_PRESS,   MotionEvent.BUTTON_PRIMARY, x)
        val move      = ev(dt+30, MotionEvent.ACTION_MOVE,           MotionEvent.BUTTON_PRIMARY, x + 2f)
        val btnRel    = ev(dt+50, MotionEvent.ACTION_BUTTON_RELEASE, 0,                          x)
        val up        = ev(dt+60, MotionEvent.ACTION_UP,             0,                          x)
        return try {
            setter.invoke(btnPress, MotionEvent.BUTTON_PRIMARY)
            setter.invoke(btnRel,   MotionEvent.BUTTON_PRIMARY)
            target.dispatchGenericMotionEvent(hoverExit)
            target.dispatchGenericMotionEvent(down)
            target.dispatchGenericMotionEvent(btnPress)
            target.dispatchGenericMotionEvent(move)
            target.dispatchGenericMotionEvent(btnRel)
            target.dispatchGenericMotionEvent(up)
            true
        } catch (_: Exception) { false }
        finally { listOf(hoverExit, down, btnPress, move, btnRel, up).forEach { it.recycle() } }
    }

    private fun dispatchTouchClick(target: View, x: Float, y: Float) {
        val downTime = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
        val move = MotionEvent.obtain(downTime, downTime + 10, MotionEvent.ACTION_MOVE, x + 2f, y, 0)
        val up = MotionEvent.obtain(downTime, downTime + 60, MotionEvent.ACTION_UP, x, y, 0)
        try {
            target.dispatchTouchEvent(down)
            target.dispatchTouchEvent(move)
            target.dispatchTouchEvent(up)
        } finally { down.recycle(); move.recycle(); up.recycle() }
    }

    private fun dispatchScroll(vNotches: Float, hNotches: Float) {
        val target = targetProvider() ?: return
        val (x, y) = targetCoords(target)
        val now = SystemClock.uptimeMillis()
        val props = MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_MOUSE
        }
        val coords = MotionEvent.PointerCoords().apply {
            this.x = x
            this.y = y
            setAxisValue(MotionEvent.AXIS_VSCROLL, vNotches)
            setAxisValue(MotionEvent.AXIS_HSCROLL, hNotches)
        }
        val event = MotionEvent.obtain(
            now, now, MotionEvent.ACTION_SCROLL, 1,
            arrayOf(props), arrayOf(coords),
            0, 0, 1f, 1f, 0, 0,
            InputDevice.SOURCE_MOUSE, 0
        )
        try {
            target.dispatchGenericMotionEvent(event)
        } finally {
            event.recycle()
        }
    }

    private fun obtainMouseEvent(downTime: Long, eventTime: Long, action: Int, x: Float, y: Float, buttonState: Int): MotionEvent {
        val props = MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_MOUSE
        }
        val coords = MotionEvent.PointerCoords().apply {
            this.x = x
            this.y = y
            pressure = 1f
            size = 1f
        }
        return MotionEvent.obtain(
            downTime, eventTime, action, 1,
            arrayOf(props), arrayOf(coords),
            0, buttonState, 1f, 1f, 0, 0,
            InputDevice.SOURCE_MOUSE, 0
        )
    }

    // --- Visibility / fade --------------------------------------------------

    /** Ensure the cursor is visible and (re)start its fade-out countdown. */
    private fun wakeCursor() {
        showCursor()
        handler.removeCallbacks(fadeRunnable)
        val timeoutMs = (settings.fadeTimeoutSec * 1000f).toLong()
        if (timeoutMs > 0) handler.postDelayed(fadeRunnable, timeoutMs)
    }

    private fun showCursor() {
        if (shown) return
        shown = true
        overlay.visibility = View.VISIBLE
        overlay.animate().alpha(1f).setDuration(FADE_ANIM_MS).start()
    }

    private fun hideCursor() {
        if (!shown) return
        shown = false
        overlay.animate().alpha(0f).setDuration(FADE_ANIM_MS).withEndAction {
            if (!shown) overlay.visibility = View.GONE
        }.start()
    }

    // --- Helpers ------------------------------------------------------------

    private fun hasRightStick(device: InputDevice?): Boolean {
        val dev = device ?: return false
        return isCenteredAxis(dev, MotionEvent.AXIS_Z) && isCenteredAxis(dev, MotionEvent.AXIS_RZ)
    }

    /** A stick axis rests centered (range crosses 0); a trigger axis rests at one end (min >= 0). */
    private fun isCenteredAxis(device: InputDevice, axis: Int): Boolean {
        val range = device.getMotionRange(axis, InputDevice.SOURCE_JOYSTICK) ?: return false
        return range.min < 0f
    }

    /** Pixels per physical centimetre on each axis, robust against TVs reporting bogus xdpi/ydpi. */
    private fun pxPerCm(): Pair<Float, Float> {
        val dm = overlay.resources.displayMetrics
        val dpiX = if (dm.xdpi in 40f..800f) dm.xdpi else dm.densityDpi.toFloat()
        val dpiY = if (dm.ydpi in 40f..800f) dm.ydpi else dm.densityDpi.toFloat()
        return (dpiX / CM_PER_INCH) to (dpiY / CM_PER_INCH)
    }

    private fun baseSpeedCmPerSec(): Float {
        val s = settings.speed.coerceIn(1f, 100f) / 100f
        return SPEED_MIN_CM_S + s * (SPEED_MAX_CM_S - SPEED_MIN_CM_S)
    }

    private fun accelCmPerSec2(): Float =
        (settings.acceleration.coerceIn(0f, 100f) / 100f) * ACCEL_MAX_CM_S2

    private fun applyDeadzone(v: Float): Float {
        if (abs(v) < JOYSTICK_DEADZONE) return 0f
        // Rescale so movement starts smoothly at the edge of the deadzone.
        val sign = if (v < 0) -1f else 1f
        return sign * ((abs(v) - JOYSTICK_DEADZONE) / (1f - JOYSTICK_DEADZONE))
    }

    /** Current cursor position (overlay-local). Exposed for tests / diagnostics. */
    val position: Pair<Float, Float> get() = posX to posY

    companion object {
        val HOTKEY_LONG_PRESS_MS: Long =
            ViewConfiguration.getLongPressTimeout().toLong().coerceAtLeast(500L)
        private const val CM_PER_INCH = 2.54f
        // Physical travel speed / acceleration the 1..100 settings map onto.
        private const val SPEED_MIN_CM_S = 1.5f
        private const val SPEED_MAX_CM_S = 22f
        private const val ACCEL_MAX_CM_S2 = 45f
        private const val MAX_SPEED_MULT = 6f
        // Physical nudge applied on a single discrete press.
        private const val STEP_MIN_CM = 0.08f
        private const val STEP_MAX_CM = 0.45f
        private const val JOYSTICK_DEADZONE = 0.15f
        private const val FADE_ANIM_MS = 200L
        // Delay between the pre-click hover (to show controls) and the actual click events (ms).
        private const val CLICK_DELAY_MS = 80L
        // Extra time past the system long-press timeout to hold a touch DOWN (so it is a long
        // press even on a slow device), used by the action-key context-menu long press.
        private const val LONGPRESS_MARGIN_MS = 150L
        // Extra delay past [longPressHoldMs] before the terminating CANCEL is sent. The cancel
        // must land *after* the long-press dialog has appeared and stolen window focus (~560 ms
        // after the DOWN, see [dispatchLongPress]) — one sent before that wedges the renderer's
        // input state just like an UP. 300 ms keeps it ~300 ms past the focus change even on a
        // slow device.
        private const val LONGPRESS_LATE_CANCEL_MS = 300L
        // Device id for synthetic touchscreen events: the id InputDispatcher uses for the
        // primary touchscreen. Must match on DOWN and follow-up events or the renderer's
        // gesture tracker won't match them to the tracked touch (see [touchEvent]).
        private const val TOUCH_DEVICE_ID = 0
        // Pixels of edge overflow that map to one mouse-wheel notch.
        private const val SCROLL_PX_PER_NOTCH = 40f
        // Wheel notches per fast-forward / rewind press.
        private const val WHEEL_NOTCHES = 3f
    }
}
