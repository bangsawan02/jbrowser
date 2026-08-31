# L10N Tools

Multi-platform localization tools for mobile and web applications.

## Platforms

### 🤖 Android
Complete localization toolkit for Android applications.

- **strings.py** - Manage Android XML string resources
- **changelogs.py** - Compile Google Play changelogs
- **publish_google_play.py** - Upload metadata to Play Store

**Documentation:** [docs/android/L10N.md](docs/android/L10N.md)

**Usage:**
```bash
# From your Android project root
python subs/l10n/android/strings.py --check
python subs/l10n/android/changelogs.py 254
```

### 🍎 iOS
*Coming soon*

### 🌐 Web
*Coming soon*

## Installation

### As Git Submodule (Recommended)

```bash
# Add to your project
git submodule add https://github.com/Slion/l10n.git subs/l10n

# Initialize and update
git submodule update --init --recursive

# Use the tools
python subs/l10n/android/strings.py --check
```

### Direct Clone

```bash
git clone https://github.com/Slion/l10n.git
cd l10n/android
python strings.py --help
```

## Project Structure

```
l10n/
├── android/          Android localization tools
│   ├── strings.py
│   ├── changelogs.py
│   └── publish_google_play.py
├── ios/              iOS localization tools (future)
├── web/              Web localization tools (future)
└── docs/
    ├── android/      Android-specific documentation
    └── shared/       Cross-platform guidelines
```

## Requirements

### Android
- Python 3.7+
- Android project with `app/src/main/res/` structure
- For Google Play uploads: Service account JSON key

## Contributing

Contributions welcome for:
- Android tool improvements
- iOS localization tools
- Web platform support
- Additional platform support

## License

MIT License
