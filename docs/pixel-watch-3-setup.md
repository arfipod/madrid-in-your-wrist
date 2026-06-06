# Pixel Watch 3 ADB Wi-Fi Setup

This repository assumes the real watch is connected through ADB wireless debugging.

## 1. Enable Developer options

On the Pixel Watch 3:

```text
Settings
→ System
→ About
→ Versions / Build number
→ tap Build number 7 times
```

## 2. Enable ADB debugging

```text
Settings
→ Developer options
→ ADB debugging: ON
→ Wireless debugging: ON
```

Keep the watch and the PC on the same Wi-Fi network.

## 3. Pair

The watch will show a pairing address and pairing code.

```bash
adb pair WATCH_IP:PAIRING_PORT
```

Enter the pairing code.

## 4. Connect

The watch will show a different ADB connection port.

```bash
adb connect WATCH_IP:ADB_PORT
adb devices -l
```

Expected result:

```text
WATCH_IP:ADB_PORT device product:... model:Pixel_Watch_3 ...
```

When more than one device or emulator is visible, select the watch explicitly:

```bash
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/install-watch.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/launch-watch.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/screenshot-watch.sh
```

## 5. Keep the session alive

Wireless debugging can disappear during idle sessions. When you are actively
working with the watch, keep one terminal running:

```bash
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/keep-watch-adb-alive.sh
```

The default ping is every 25 seconds and does not wake the screen. If the watch
is especially aggressive about sleeping during a session, use a shorter interval
or an occasional wakeup:

```bash
ADB_KEEP_ALIVE_INTERVAL_SECONDS=15 ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/keep-watch-adb-alive.sh
ADB_KEEP_ALIVE_WAKE_EVERY=20 ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/keep-watch-adb-alive.sh
```

Stop it with `Ctrl-C` when the session is over.

## 6. Common issues

### `offline`

Restart ADB:

```bash
adb kill-server
adb start-server
adb connect WATCH_IP:ADB_PORT
```

### Device not visible from WSL

Use ADB Wi-Fi instead of USB. WSL often does not expose USB devices unless
`usbipd-win` is configured.

### Multiple ADB servers

Avoid running different ADB servers from Windows and WSL at the same time. Pick
one shell as the source of truth for device control.
