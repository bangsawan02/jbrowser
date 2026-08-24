# Test run — Pi Compute Module 5 Rev 1.0 · landscape-0-sw584

- **When:** 2026-08-24T00:52:20+00:00
- **Device:** Raspberry Pi 5 TV box (Raspberry Pi Compute Module 5 Rev 1.0) — Android 16 (serial `192.168.178.67:5555`)
- **Config:** landscape, rotation 0°, smallest width 584dp
- **Package:** `net.slions.fulguris.full.download.debug`
- **Options:** restart=False, keep_tabs=False, orientation=default, filter=scrubber_seek_after_idle
- **Result:** 1/1 passed in 33.6s

| Test | Description | Result | Duration |
|---|---|---|---|
| `test_cursor_youtube_scrubber_seek_after_idle` | Click seeks even after controls auto-hid (dispatchHover+delay re-shows them before BUTTON_PRESS lands) (leanback only) | ✅ pass | 32.7s |
