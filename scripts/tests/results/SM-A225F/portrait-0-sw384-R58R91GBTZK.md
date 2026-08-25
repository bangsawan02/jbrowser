# Test run — SM-A225F · portrait-0-sw384

- **When:** 2026-08-25T23:25:51+00:00
- **Device:** Galaxy A22 5G (Samsung SM-A225F) — Android 13 (serial `R58R91GBTZK`)
- **Config:** portrait, rotation 0°, smallest width 384dp
- **Package:** `net.slions.fulguris.full.agent.debug`
- **Options:** restart=False, keep_tabs=False, orientation=default, filter=all
- **Result:** 3/3 passed in 98.5s

| Test | Description | Result | Duration |
|---|---|---|---|
| `test_cursor_toggle_hotkey_shows_and_hides_overlay` | Long-press play/pause toggles the cursor overlay on and off | ✅ pass | 27.6s |
| `test_cursor_toggle_exit_focuses_menu_button` | Turning the cursor off moves focus to the toolbar menu button | ✅ pass | 26.4s |
| `test_cursor_survives_options_sheet_dismiss` | Opening then dismissing the options bottom sheet does not leave the cursor suspended (resumes on sheet close) | ✅ pass | 41.5s |
