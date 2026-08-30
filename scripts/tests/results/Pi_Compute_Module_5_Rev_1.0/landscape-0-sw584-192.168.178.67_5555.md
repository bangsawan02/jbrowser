# Test run — Pi Compute Module 5 Rev 1.0 · landscape-0-sw584

- **When:** 2026-08-25T23:27:18+00:00
- **Device:** Raspberry Pi 5 TV box (Raspberry Pi Compute Module 5 Rev 1.0) — Android 16 (serial `192.168.178.67:5555`)
- **Config:** landscape, rotation 0°, smallest width 584dp
- **Package:** `net.slions.fulguris.full.agent.debug`
- **Options:** restart=False, keep_tabs=False, orientation=default, filter=all
- **Result:** 3/3 passed in 79.2s

| Test | Description | Result | Duration |
|---|---|---|---|
| `test_cursor_toggle_hotkey_shows_and_hides_overlay` | Long-press play/pause toggles the cursor overlay on and off | ✅ pass | 21.1s |
| `test_cursor_toggle_exit_focuses_menu_button` | Turning the cursor off moves focus to the toolbar menu button | ✅ pass | 20.4s |
| `test_cursor_survives_options_sheet_dismiss` | Opening then dismissing the options bottom sheet does not leave the cursor suspended (resumes on sheet close) | ✅ pass | 34.8s |
