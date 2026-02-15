# Copy Path App

A minimal native Android app that extracts filesystem paths from shared files and copies them to the clipboard. Designed for Termux terminal usage.

## Features

- Registers as a share target for all file types
- Extracts filesystem path or content URI from shared files
- Copies path to clipboard
- Simple toast feedback
- No visible UI (transparent activity)

## Installation

Download the latest APK from [Releases](https://github.com/jmonloop/copy-path-app/releases) and install on your Android device.

## Usage

1. Open any file manager or app with files
2. Share a file
3. Select "Copy Path" from the share menu
4. The file path is copied to clipboard
5. Paste in Termux or any other app

## Path Resolution

The app tries multiple strategies to get the best path:
1. Direct `file://` URI extraction
2. MediaStore `_DATA` column query
3. DocumentsContract parsing for download documents
4. `/proc/self/fd` symlink resolution
5. Fallback to content URI string

## Requirements

- Android 7.0 (API 24) or higher
- Target SDK: Android 14 (API 34)

## Building

```bash
./gradlew assembleDebug
```

The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`

## License

MIT License
