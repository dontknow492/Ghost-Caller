# 👻 Ghost Caller

> A **modern, premium Android dialer** built with **Jetpack Compose**, featuring **glassmorphism UI**, **real-time Telecom integration**, and **intelligent call management**.

Ghost Caller reimagines the calling experience with a **frosted-glass aesthetic**, **wallpaper-aware UI**, and **deep system integration** — delivering a smooth, responsive, and elegant communication interface as a fully functional Default Dialer.

---

# 🌟 Features

## 🖼️ Glassmorphism & Adaptive UI

- **Wallpaper Integration**  
  Dynamically uses the device wallpaper as a blurred background for call screens.
- **Frosted Glass Effects (Android 12+)**  
  Real-time blur using `RenderEffect` for a premium system-like feel.
- **Responsive Layouts**  
  Seamlessly adapts across:
    - 📱 Portrait
    - 🔄 Landscape
    - 💻 Tablet / Widescreen (≥600dp)

---

## 📞 Advanced Calling & System Integration

- **Default Dialer Experience**  
  Fully integrates with the Android Telecom subsystem using `InCallService` and `RoleManager`.
- **Background & Lock Screen Support**  
  Proactively wakes the device and displays the call UI over the lock screen for incoming calls, even when the app is closed.
- **Intelligent Call Management**  
  - Answer/Reject/End calls directly within the app.
  - Speakerphone, Mute, and Hold controls.
- **Smart Call Logs**  
  Automatically groups consecutive calls from the same contact to reduce clutter.
- **Advanced Caller Identification**  
  Offline location detection (City / State / Country) using **Google libphonenumber**.

---

## 🧠 Real-Time Telephony Engine

- **Live In-Call Service**  
  Uses the standard Android `InCallService` API to react to live call states (Ringing, Dialing, Active, Disconnected) with zero latency.
- **Automatic Log Sync**  
  Refreshes call logs immediately after call termination using background coroutines and specialized observers.

---

## 🛠️ Technical Stack

| Category        | Technology |
|----------------|----------|
| **Language**    | Kotlin |
| **UI Framework**| Jetpack Compose (Material 3) |
| **Architecture**| MVVM + Clean Architecture |
| **Navigation**  | **Navigation3** (Experimental Compose-first Nav) |
| **DI**          | **Koin** |
| **Telephony**   | **InCallService (Telecom SDK)**, TelephonyManager |
| **State**       | Coroutines, StateFlow, ViewModel |
| **Libraries**   | libphonenumber (Google), Geocoder, Timber, Coil |
| **Build System**| Gradle (Kotlin DSL) |

---

## 📸 Preview

<div align="center">

### 🏠 Home Screen
<img src="images/home_light.png" width="240"/>
<img src="images/home_dark.png" width="240"/>

<br/><br/>

### 📞 Call Screen
<img src="images/caller_light.png" width="240"/>
<img src="images/caller_dark.png" width="240"/>

</div>

---

## 🏗️ Architecture Overview

### 📡 Telecom & InCallService Flow

The app serves as a system-level communication handler.

1. **Role Management**: Requests `ROLE_DIALER` to become the primary phone app.
2. **Service Binding**: The system binds to `CallService` (extending `InCallService`) when a call occurs.
3. **UI Activation**:
    - `CallService` detects `onCallAdded`.
    - Automatically launches `MainActivity` with `FLAG_SHOW_WHEN_LOCKED` to bypass the lock screen.
    - Navigation automatically pushes the `CallScreen` based on the live `CallStatus`.
4. **Call Control**: User interactions are routed through `CallManager` which communicates directly with the `android.telecom.Call` object.

---

### 📱 Navigation & State

- **Navigation3**: Uses the latest navigation approach for ultra-lightweight, state-driven screen transitions.
- **Reactive UI**: The `CallViewModel` observes the `InCallService` state and pushes updates to the Compose UI via `StateFlow`.

---

## 📥 Installation

### Prerequisites

- Android Studio Ladybug+
- Android SDK 30+ (API 34+ recommended for full feature support)
- **Physical device required** for testing actual calling features and Default Dialer roles.

---

### Setup

```bash
git clone https://github.com/dontknow492/Ghost-Caller.git
```

1. Open in Android Studio.
2. Build and run on a physical device.
3. **Accept the "Set as Default Dialer" prompt** to enable full calling functionality.
