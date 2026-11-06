# Fire TV Stick 3rd Gen (sheldonp) — Debloat & Optimization Guide

> **Device Context**  
> - **Model**: Amazon Fire TV Stick 3rd Generation (`AFTSSS`)  
> - **Codename**: `sheldonp`  
> - **Fire OS Version**: 7.7.1.7 (Android 9 Pie, API 28)  
> - **SoC & Architecture**: MediaTek MT8695 Quad-Core Cortex-A53 (`armeabi-v7a`, 32-bit)  
> - **RAM**: ~900 MB physical memory (~47 MB free at baseline stock state)  
> - **Storage**: 4.9 GB internal flash (`/data`)  

---

## 1. Overview & Objective

The Amazon Fire TV Stick 3rd Generation operates on a resource-constrained hardware architecture (900 MB total physical memory). In stock configuration, background advertising brokers, whole-home telemetry daemons, retail services, and preloaded apps consume over 85% of total system memory, degrading launcher responsiveness and streaming playback.

This document records the exact modifications executed on **November 6, 2026** via ADB to neutralize advertising, eliminate telemetry, uninstall unneeded third-party bloat, and protect critical system components.

---

## 2. Advertising & Ad-Tracking Services

These background services were disabled using `pm disable-user --user 0 <package>` to stop ad bidding, telemetry beacons, and sponsored UI injection.

| Package Name | Service Name | Role & Functional Impact | Status |
|---|---|---|---|
| `com.amazon.whad` | **Whole-Home Ads Daemon** | Background ad delivery broker. Consumed ~8.2 MB RAM continuously. | **`disabled-user`** |
| `com.amazon.hybridadidservice` | **Ad-ID Tracking Service** | Maintains advertising identifier for cross-app behavioral tracking. | **`disabled-user`** |
| `com.fireos.sdk.ftve.addp` | **Ads Data Display Provider** | Data provider for Fire TV sponsored carousel ads and banners. | **`disabled-user`** |
| `com.amazon.shoptv.client` | **ShopTV Shopping Client** | Shopping promotions and push advertisement engine. | **`disabled-user`** |
| `com.amazon.shoptv.firetv.client` | **ShopTV Fire TV Client** | UI client for promotional retail shopping items. | **`disabled-user`** |
| `com.amazon.aria` | **Sponsored Suggester** | Algorithmic sponsored suggestions injected into search and home. | **`disabled-user`** |

### Protected Packages (AppOps Mitigation)

In Fire OS 7.7.1.7, Amazon added certain advertising packages to the internal system protection whitelist (`protected-packages.xml`), throwing `SecurityException: Cannot disable a protected package` when attempting `pm disable-user`.

| Protected Package | Role | Workaround Applied |
|---|---|---|
| `com.amazon.adep` | **Amazon Ads Execution Platform** (Consumed ~42 MB RAM) | Background execution disabled via AppOps: `cmd appops set com.amazon.adep RUN_IN_BACKGROUND ignore` and `RUN_ANY_IN_BACKGROUND ignore`. |
| `com.amazon.ftvads.deeplinking` | **Sponsored Ad Deep-Linking** | Background execution disabled via AppOps: `cmd appops set com.amazon.ftvads.deeplinking RUN_IN_BACKGROUND ignore` and `RUN_ANY_IN_BACKGROUND ignore`. |

---

## 3. Stock Bloatware & Unused Amazon Services

These pre-installed Amazon packages run background synchronization and preload media, consuming system RAM and CPU cycles.

| Package Name | Service Name | Reason for Disabling | Status |
|---|---|---|---|
| `com.amazon.bueller.photos` | **Amazon Photos TV** | Background cloud sync and screensaver preload (~25–30 MB RAM). | **`disabled-user`** |
| `com.amazon.gamehub` | **Amazon Game Hub** | Unused game launcher and cloud gaming showcase. | **`disabled-user`** |
| `com.amazon.tv.livetv` | **Live TV Recommendation** | Injects promotional live channel tiles into launcher. | **`disabled-user`** |

---

## 4. Uninstalled Third-Party Applications

The following 7 user-space applications were completely uninstalled via `pm uninstall <package>` to free up internal flash storage and remove clutter from the "All Apps" drawer:

| Application Name | Package Name | Result |
|---|---|---|
| **Fawesome TV** | `com.future.moviesByFawesomeAndroidTV` | **Uninstalled (Success)** |
| **HappyKids** | `com.future.HappyKids` | **Uninstalled (Success)** |
| **Hungama Play** | `com.hungama.movies` | **Uninstalled (Success)** |
| **NDTV India News** | `com.july.ndtv` | **Uninstalled (Success)** |
| **Aha** | `ahaflix.tv` | **Uninstalled (Success)** |
| **Airtel Xstream** | `tv.airtel.xstream.tvapp` | **Uninstalled (Success)** |
| **SonyLIV** | `com.onemainstream.sonyliv.android` | **Uninstalled (Success)** |

---

## 5. Preserved System Whitelist (DO NOT TOUCH)

The following components were verified and strictly kept active to preserve all essential Fire OS and FireLite capabilities:

- **`com.amazon.vizzini`** — Alexa Voice Service (voice input on remote).
- **`com.amazon.venezia`** — Amazon Appstore (handles OTT license validation and DRM tokens).
- **`com.amazon.whisperplay.*`** — Screen Mirroring & DIAL remote casting bridge.
- **`com.amazon.tv.settings.v2`** — Core Fire TV Settings application.
- **`com.amazon.device.software.ota`** — System software updates.
- **`com.amazon.tv.launcher`** — Stock Home launcher fallback.
- **`in.startv.hotstar`**, **`com.netflix.ninja`**, **`com.amazon.firetv.youtube`**, **`com.amazon.firebat`**, **`com.suntv.sunnxt`**, **`com.zee5.amazon`** — Active OTT streaming applications.

---

## 6. Verification Results

Following these operations:
- **Available Storage**: `/data` partition has **1.2 GB free** (74% used out of 4.9 GB).
- **Installed Packages**: Total package count reduced from 257 to 250; all 7 targeted apps uninstalled.
- **Background Ads**: Whole-home ad daemon (`whad`), ad tracking identifier (`hybridadidservice`), and ad data provider (`ftve.addp`) no longer initialize on boot.

---

## 7. Reversal & Restoration Guide

All modifications are 100% reversible via ADB without requiring a factory reset.

### Re-enabling Disabled Services
To restore any disabled service, execute:
```bash
adb shell pm enable <package-name>
```

**Common Examples:**
```bash
# Restore Amazon Photos
adb shell pm enable com.amazon.bueller.photos

# Restore Live TV rows
adb shell pm enable com.amazon.tv.livetv

# Restore Amazon Whole-Home Ads
adb shell pm enable com.amazon.whad

# Restore Amazon Game Hub
adb shell pm enable com.amazon.gamehub
```

### Resetting AppOps Restrictions
To re-allow background execution for protected ad packages:
```bash
adb shell cmd appops set com.amazon.adep RUN_IN_BACKGROUND default
adb shell cmd appops set com.amazon.adep RUN_ANY_IN_BACKGROUND default
adb shell cmd appops set com.amazon.ftvads.deeplinking RUN_IN_BACKGROUND default
adb shell cmd appops set com.amazon.ftvads.deeplinking RUN_ANY_IN_BACKGROUND default
```

### Reinstalling Uninstalled Third-Party Apps
Third-party apps can be reinstalled directly from the Amazon Appstore or via ADB:
```bash
adb install <path-to-apk>
```
