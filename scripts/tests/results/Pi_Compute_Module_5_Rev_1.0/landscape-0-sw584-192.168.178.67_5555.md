# Test run — Pi Compute Module 5 Rev 1.0 · landscape-0-sw584

- **When:** 2026-08-23T17:21:31+00:00
- **Device:** Raspberry Pi 5 TV box (Raspberry Pi Compute Module 5 Rev 1.0) — Android 16 (serial `192.168.178.67:5555`)
- **Config:** landscape, rotation 0°, smallest width 584dp
- **Package:** `net.slions.fulguris.full.download.debug`
- **Options:** restart=False, keep_tabs=False, orientation=default, filter=all
- **Result:** 4/4 passed in 109.0s

| Test | Description | Result | Duration |
|---|---|---|---|
| `test_cursor_movement_dpad_right_moves_right` | D-pad right moves the cursor right (click X increases) | ✅ pass | 28.5s |
| `test_cursor_movement_dpad_down_moves_down` | D-pad down moves the cursor down (click Y increases) | ✅ pass | 32.5s |
| `test_cursor_movement_edge_scrolls_page` | Pushing past the bottom edge scrolls the page | ✅ pass | 43.9s |
| `test_cursor_movement_gamepad_dpad_yields_to_focus_nav` | With the cursor on, a two-stick gamepad's D-pad is yielded to focus navigation (the right stick drives the cursor) while the stick-less D-pad still moves it | ✅ pass | 1.3s |
