# Test run — SHIELD Android TV · landscape-0-sw1080

- **When:** 2026-08-23T17:59:11+00:00
- **Device:** SHIELD Android TV (Nvidia SHIELD Android TV) — Android 11 (serial `192.168.178.37:5555`)
- **Config:** landscape, rotation 0°, smallest width 1080dp
- **Package:** `net.slions.fulguris.full.download.debug`
- **Options:** restart=False, keep_tabs=False, orientation=default, filter=all
- **Result:** 4/4 passed in 210.6s

| Test | Description | Result | Duration |
|---|---|---|---|
| `test_cursor_movement_dpad_right_moves_right` | D-pad right moves the cursor right (click X increases) | ✅ pass | 36.7s |
| `test_cursor_movement_dpad_down_moves_down` | D-pad down moves the cursor down (click Y increases) | ✅ pass | 33.0s |
| `test_cursor_movement_edge_scrolls_page` | Pushing past the bottom edge scrolls the page | ✅ pass | 134.5s |
| `test_cursor_movement_gamepad_dpad_yields_to_focus_nav` | With the cursor on, a two-stick gamepad's D-pad is yielded to focus navigation (the right stick drives the cursor) while the stick-less D-pad still moves it | ✅ pass | 1.3s |
