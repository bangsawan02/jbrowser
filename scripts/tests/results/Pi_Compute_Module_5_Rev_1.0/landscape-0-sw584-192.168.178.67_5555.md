# Test run — Pi Compute Module 5 Rev 1.0 · landscape-0-sw584

- **When:** 2026-08-23T00:39:41+00:00
- **Device:** Raspberry Pi 5 TV box (Raspberry Pi Compute Module 5 Rev 1.0) — Android 16 (serial `192.168.178.67:5555`)
- **Config:** landscape, rotation 0°, smallest width 584dp
- **Package:** `net.slions.fulguris.full.download.debug`
- **Options:** restart=False, keep_tabs=False, orientation=default, filter=repeated_long_press
- **Result:** 0/1 passed in 32.2s

| Test | Description | Result | Duration |
|---|---|---|---|
| `test_cursor_context_menu_repeated_long_press_touch_stays_clean` | Repeated long presses (same page) each deliver a fresh touch — the synthetic long press must not leave the WebView's touch state stuck | ❌ fail | 31.2s |
| | _each of the 3 long presses should deliver a fresh touch (pointerdown) to the page, but only 2 did — the WebView's touch state is stuck (UP-after-cancel); log='pd0 ts3 pc537 ctx538 tc562 pd3179 ts3180 pc3682 ctx3682 tc3692 ctx6890'_ | | |
