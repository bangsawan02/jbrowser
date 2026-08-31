# GitHub Copilot Instructions for Android L10N

## Project Context

These are generic Android localization tools (strings.py, changelogs.py, publish_google_play.py) for managing translations and Google Play metadata across any Android application.

## Your Role

When the user asks to work on translations, you should:

1. **Check current status** using `python strings.py --check <lang-code>`
2. **Identify untranslated strings** from the check output
3. **⚠️ CRITICAL: Get the actual English source text** using `python strings.py --get source <string_id>`
   - **NEVER translate based on the string ID name alone** (e.g., `download_status_orphaned` ≠ "Orphaned")
   - **ALWAYS check what the English source actually says** (e.g., "File not found")
   - String IDs are technical identifiers, NOT the content to translate
4. **Provide translations** for the actual English content
5. **Apply translations** using the strings.py tool (PowerShell syntax)
6. **CHECK FOR ERRORS** in command output - look for `[ERROR]` or `[OK]` messages
7. **Fix any errors** before continuing
8. **Verify success** and continue with next batch

**Important:**
- Run all commands in the existing terminal session. Do not open new terminal windows.
- If you encounter "string does not exist in English source file" errors, **STOP and report the issue** to the user. The string must be added to English first.
- **Do not stop translation work until complete**, even if it takes a long time and many batches. Continue working through all untranslated strings systematically.

## Complete Documentation

📖 **For ALL translation instructions, see [L10N.md](L10N.md)**

The L10N.md file contains everything you need:
- **Complete strings.py tool documentation** with all commands
- **Translation workflow** (check → translate → apply → verify)
- **Android XML escaping rules** (CRITICAL: use `\'` not `&apos;`)
- **PowerShell syntax and quoting** (multiple options explained)
- **Validation rules** and error handling
- **Plural forms** handling
- **Special cases** (XML tags, near-matches, multi-line strings)

## Critical Reminders

**Always use PowerShell syntax** - This is a Windows project!

**Work in batches** - Translate 10-20 strings at a time for efficiency

**Check command output for errors** - Look for `[ERROR]` vs `[OK]` messages

**CRITICAL: Placeholder mismatches cause app crashes!** - The `--check` command automatically validates placeholder consistency. Look for `[CRITICAL]` issues and fix them immediately.

**Don't spawn new terminal windows** - Run commands in the existing terminal session

**String must exist in English** - If a string doesn't exist in English source file, it cannot be translated. Report to developers.

**Read L10N.md first** - All technical details are documented there

## PowerShell String Quoting Guide

### Simple Rule: Always Use Single Quotes

**ALWAYS use single quotes - no escaping needed for double quotes in XML!**

```powershell
# Simple strings
python strings.py --set string_id th-rTH 'คำแปล'

# Multiple languages at once (RECOMMENDED)
python strings.py --set app_name de-rDE 'MyApp' fr-rFR 'MonApp' es-rES 'MiApp'

# Strings with placeholders (%1$s, %d, etc.) - NO escaping needed!
python strings.py --set string_id th-rTH 'ดาวน์โหลด %1$s'

# Strings with double quotes inside - NO escaping needed!
python strings.py --set string_id th-rTH 'สลับไปที่ "%s"'

# Strings with BOTH quotes and placeholders
python strings.py --set string_id th-rTH 'ดาวน์โหลด "%1$s"'

# Strings with newlines (\n) - works perfectly in single quotes
python strings.py --set string_id th-rTH 'บรรทัด1\n\nบรรทัด2'

# Strings with apostrophes (single quotes) - double them up
python strings.py --set action_dont_allow th-rTH 'Don''t allow'
python strings.py --set save_data th-rTH 'Request ''Save-Data'''

# Complex XML - single quotes, no escaping needed for double quotes!
python strings.py --raw --set string_id th-rTH 'ข้อความ <xliff:g id="example">%1$s</xliff:g>'
```

**Why single quotes work perfectly:**
- PowerShell single quotes treat everything literally (no variable expansion)
- Dollar signs `$` in placeholders like `%1$s` work perfectly - no escaping needed
- Double quotes in XML attributes need NO escaping
- Newlines `\n` work correctly - PowerShell passes `\` and `n` as separate characters
- **Apostrophes (single quotes) must be doubled:** `''` → PowerShell converts to `'`
- Safe from accidental PowerShell variable interpolation

## Quick Start Examples

```powershell
# Check what needs translation
python strings.py --check th-rTH

# ⚠️ ALWAYS check the English source content FIRST
python strings.py --get source download_status_orphaned
# Output: File not found (NOT "Orphaned" - don't translate the ID name!)

# ONE string across MULTIPLE languages (for same string in different languages)
python strings.py --set app_name de-rDE 'MyApp' fr-rFR 'MonApp' es-rES 'MiApp'

# MULTIPLE strings in ONE language (for bulk translation - RECOMMENDED for efficiency)
python strings.py --set-batch th-rTH string1 'คำแปล1' string2 'คำแปล2' string3 'คำแปล3'

# Strings with placeholders - single quotes, no escaping needed for $
python strings.py --set dialog_download th-rTH 'คุณต้องการดาวน์โหลดไฟล์นี้หรือไม่? (%1$s)'

# Strings with quotes - single quotes, no escaping needed!
python strings.py --set session_switched th-rTH 'สลับไปที่ "%s"'

# Strings with apostrophes - double the single quotes
python strings.py --set action_dont_allow da-rDK 'Tillad ikke'  # No apostrophe in Danish
python strings.py --set action_dont_allow en-rUS 'Don''t allow'  # English has apostrophe - double it

# Strings with BOTH quotes and placeholders
python strings.py --set string_id th-rTH 'ดาวน์โหลด "%1$s"'

# Complex XML - single quotes, no escaping needed for quotes!
python strings.py --raw --set string_id th-rTH 'ข้อความ <xliff:g id="example">%s</xliff:g>'

# Complex XML with placeholders - still single quotes!
python strings.py --raw --set match_x_of_n th-rTH 'ตรงกัน <xliff:g id="current_match" example="1">%1$d</xliff:g> จาก <xliff:g id="match_count" example="10">%2$d</xliff:g>'

# Edit English source strings (developers only) - use 'source' as language code
python strings.py --get source settings           # Read from English source
python strings.py --set settings source 'Settings'  # Update English source string

# Bulk translate MULTIPLE strings in ONE language (efficient for large translation batches)
python strings.py --set-batch th-rTH action_close 'ปิด' action_exit 'ออก' action_save 'บันทึก'

# Bulk translate with XML content (use --raw flag)
python strings.py --raw --set-batch th-rTH string1 'Text <xliff:g id="x">%s</xliff:g>' string2 'Other text'
```

## Error Checking - IMPORTANT!

**Always examine command output** for success/failure indicators:

✅ **Success looks like:**
```
[OK] string_id1
[OK] string_id2
Successfully updated: 2
```

❌ **Error looks like:**
```
[ERROR] XML Validation Failed for 'string_id'!
  Attribute value not quoted: id=example
[ERROR] String 'string_id' does not exist
```

**Common Errors & Fixes:**

| Error | Cause | Fix |
|-------|-------|-----|
| `Attribute value not quoted` | Missing quotes in XML | Use single quotes - no escaping needed |
| `Entity '&apos;' detected` | Wrong entity for Android | Use `\'` instead |
| `Placeholder mismatch` | Missing or wrong placeholders | Match English placeholders exactly |
| `String does not exist` | String not in language file | **STOP** - Report to user, don't add! |

**If you see "String does not exist" error:**
1. **STOP translation work immediately**
2. **Report the issue to the user** - the string may be missing from English source or file is out of sync
3. **DO NOT attempt to add the string** - adding strings should only be done deliberately via `--add` command
4. **Wait for user guidance** before continuing

**For other errors:**
1. **Stop immediately** - don't continue with more translations
2. **Read the error message** carefully
3. **Fix the issue** (usually quoting or escaping)
4. **Retry the failed string**
5. **Only continue** after seeing `[OK]`

## Google Play Metadata

Android apps using Google Play can maintain language-specific store metadata:
- **Example Languages:** en-US, en-GB, ar, cs-CZ, da-DK, de-DE, el-GR, es-ES, fi-FI, fr-FR, hi-IN, hr, hu-HU, id, it-IT, ja-JP, ko-KR, lt, nl-NL, no-NO, pl-PL, pt-BR, pt-PT, ro, ru-RU, sr, sv-SE, th, tr-TR, uk, vi, zh-CN, zh-TW
- **Directory:** `fastlane/metadata/android/{language}/`
- **Files:** `title.txt`, `short_description.txt`, `full_description.txt`, `changelogs/{version}.txt`

### Language Code Differences

**IMPORTANT:** Android resource codes ≠ Google Play codes

| Android Resource | Google Play | Example Language |
|------------------|-------------|------------------|
| `en-rUS` | `en-US` | English (US) |
| `en-rGB` | `en-GB` | English (UK) |
| `ar-rSA` | `ar` | Arabic |
| `hi-rIN` | `hi-IN` | Hindi |
| `in-rID` | `id` | Indonesian |
| `zh-rCN` | `zh-CN` | Chinese (Simplified) |

**Pattern:**
- **Android**: `{lang}-r{REGION}` (e.g., `en-rGB`, `pt-rBR`)
- **Google Play**: `{lang}-{REGION}` or just `{lang}` (e.g., `en-GB`, `pt-BR`, `ar`)

**Note:** Some Android languages are **NOT supported** by Google Play Console:
- `bs-rBA` (Bosnian), `sat-rIN` (Santali), `af-rZA` (Afrikaans), `ca-rES` (Catalan), `iw-rIL` (Hebrew), `me-rME` (Montenegro)

### Adding a New Metadata Language

**Prerequisites:** Android string translations should already exist in `app/src/main/res/values-{lang}/strings.xml`

**Steps:**

1. **Create directory structure:**
```powershell
mkdir fastlane\metadata\android\{google-play-code}
mkdir fastlane\metadata\android\{google-play-code}\changelogs
```

2. **Create metadata files:**
```powershell
# title.txt (30 chars max)
echo "YourApp" > fastlane\metadata\android\{google-play-code}\title.txt

# short_description.txt (80 chars max)
echo "Your tagline here" > fastlane\metadata\android\{google-play-code}\short_description.txt

# full_description.txt (4000 chars max) - copy and translate from en-US
copy fastlane\metadata\android\en-US\full_description.txt fastlane\metadata\android\{google-play-code}\full_description.txt
```

3. **Update `fastlane/changelogs.py`:**
Add the Google Play language code to the `languages` list:
```python
languages = [
    'en-US',  # English (US)
    'new-CODE',  # Your New Language - ADD HERE
    'ar',     # Arabic
    # ... rest of languages
]
```

4. **Create changelogs for recent versions:**
```powershell
# Copy from en-US and translate
copy fastlane\metadata\android\en-US\changelogs\251.txt fastlane\metadata\android\{google-play-code}\changelogs\251.txt
copy fastlane\metadata\android\en-US\changelogs\252.txt fastlane\metadata\android\{google-play-code}\changelogs\252.txt
```

5. **Test changelog compilation:**
```powershell
python fastlane\changelogs.py 252
# Should show "Found changelogs for X/X languages" with incremented count
```

6. **Verify with Google Play Console:**
   - Go to Store presence → Main store listing
   - Click "Add language"
   - Ensure your language code is supported by Google Play

**Example - Adding English (UK):**
```powershell
# Already translated in app/src/main/res/values-en-rGB/strings.xml ✓

# 1. Create directory
mkdir fastlane\metadata\android\en-GB
mkdir fastlane\metadata\android\en-GB\changelogs

# 2. Create metadata files (British English spellings)
echo "Your App Name" > fastlane\metadata\android\en-GB\title.txt
echo "Your short description here" > fastlane\metadata\android\en-GB\short_description.txt
copy fastlane\metadata\android\en-US\full_description.txt fastlane\metadata\android\en-GB\full_description.txt
# Edit full_description.txt: organize→organised, color→colour, trash→rubbish bin

# 3. Update changelogs.py (add 'en-GB' after 'en-US')

# 4. Create changelogs
copy fastlane\metadata\android\en-US\changelogs\*.txt fastlane\metadata\android\en-GB\changelogs\
# Edit for British English: localization→localisation

# 5. Test
python fastlane\changelogs.py 252
```

**See [L10N.md](../L10N.md#adding-a-new-language) for complete documentation.**

---

## Creating Release Notes

When the user asks to "create release notes for version X" or "work on changelog for version X":

### Process Overview

1. **Identify the last release tag**
2. **Get all commits since that tag**
3. **Analyze and categorize the changes**
4. **Create concise, user-focused changelog**
5. **Follow the emoji-based format**

### Step-by-Step Commands

```powershell
# 1. Find the most recent release tag
git tag --sort=-creatordate | Select-Object -First 5

# 2. Get commit log since last tag (replace TAG_NAME with actual tag)
git log TAG_NAME..HEAD --oneline --no-merges

# 3. Get full commit messages for analysis
git log TAG_NAME..HEAD --pretty=format:"%s" --no-merges | Out-String

# 4. Count commits (optional, for context)
git log TAG_NAME..HEAD --oneline --no-merges | Measure-Object -Line
```

### Changelog Format

**File location:** `fastlane/metadata/android/en-US/changelogs/{version}.txt`

**Format rules:**
- **Emoji prefix** for each feature (🗺️ 🌐 🍪 🚫 📋 ⚙️ 🐛 etc.)
- **Brief, user-focused descriptions** (not technical implementation details)
- **Group related changes** into single lines when possible
- **Keep total to 3-7 lines** (Google Play limit: 500 chars)
- **Bug fixes always last** with generic "Various improvements and bug fixes"

**Common emojis:**
- 🗺️ Location/Maps features
- 🌐 Network/Web features
- 🍪 Cookies
- 🚫 Ad blocking
- 📋 Menu/UI
- ⚙️ Settings/Configuration
- 🖥️ Developer/Console features
- 📊 Stats/Display
- 🎭 Themes
- 🌍 Localization
- 🐛 Bug fixes (always last line)

### Analysis Workflow

**Group commits by category:**
1. **Major features** - New functionality users will notice
2. **Enhancements** - Improvements to existing features
3. **UI/Menu changes** - Visible interface updates
4. **Bug fixes** - Group generically unless critical fix
5. **L10N/Docs** - Usually omit from user-facing changelog

**Example categorization from 44 commits:**
```
Location/Domain Settings (16 commits):
  → 🗺️ Location permissions per domain

Network Engine (11 commits):
  → 🌐 Add network engine selection (OkHttp, HttpUrlConnection)
  → ⚙️ Network cache size configuration

Menu Enhancements (5 commits):
  → 🍪 Add cookies menu item & count display
  → 🚫 Ad blocker shows blocked requests count
  → 📋 Display current session name on menu

Bug Fixes (multiple):
  → 🐛 Various UI improvements and bug fixes
```

### Example Session

```powershell
# Find last tag
git tag --sort=-creatordate | Select-Object -First 5
# Output: YourApp-v2.0.1, YourApp-v2.0.0, ...

# Get commits since v2.0.1
git log YourApp-v2.0.1..HEAD --pretty=format:"%s" --no-merges | Out-String

# Analyze output and create changelog
# Result: fastlane/metadata/android/en-US/changelogs/253.txt
```

**Example changelog (253.txt):**
```
🗺️ Location permissions per domain
🌐 Add network engine selection (OkHttp, HttpUrlConnection)
🍪 Add cookies menu item & count display
🚫 Ad blocker shows blocked requests count
📋 Display current session name on menu
⚙️ Network cache size configuration
🐛 Various UI improvements and bug fixes
```

### Translation to Other Languages

After creating English changelog, translate to all 33 Play Store languages if needed (see Google Play Metadata section in L10N.md).

### Tips

- **Focus on user-visible changes** - Skip internal refactoring
- **Combine related commits** - Don't list every fix separately
- **Check previous changelogs** - Match style and emoji usage
- **Keep concise** - Play Store has 500 char limit per language
- **Test length** - Longer descriptions may need trimming

---

**For complete instructions, commands, examples, and troubleshooting: See [L10N.md](L10N.md)**

**Last Updated:** December 22, 2025
