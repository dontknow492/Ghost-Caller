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

- **Dynamic Theming**  
  Adapts to system light/dark mode and blends with wallpaper tones.

- **Responsive Layouts**  
  Seamlessly adapts across:
  - 📱 Portrait
  - 🔄 Landscape
  - 💻 Tablet / Widescreen (≥600dp)

---

## 📞 Advanced Calling & System Integration

- **Default Dialer Experience**  
  Fully integrates with Android Telecom using `InCallService` + `RoleManager`.

- **Incoming Call UI (Lockscreen Ready)**  
  Displays full-screen incoming call UI even when:
  - App is closed
  - Device is locked

- **Call Controls (Modern UI)**
  - Accept / Reject
  - Mute / Unmute
  - Speaker toggle
  - Hold support

- **Quick Actions (New ✨)**
  - 📩 Send SMS directly from call screen
  - ⏰ Remind me (future extensible feature)
  - 📇 Add to contacts instantly

- **Smart Call Logs**
  - Groups repeated calls
  - Shows relative time ("Just now", "2 min ago")
  - Fast scroll + smooth animations

---

## 🧠 Real-Time Telephony Engine

- **Live Call State Sync**
  - Dialing
  - Ringing
  - Active
  - Disconnected

- **Zero-Lag UI Navigation**
  - Call screen appears instantly (no cold-start lag)
  - Precomputed navigation stack

- **Reactive Call Flow**
  - `StateFlow` + Compose = real-time updates
  - Auto-dismiss call screen when call ends

- **Auto Call Log Refresh**
  - Updates immediately after call ends
  - Uses observers + coroutines

---

## 📇 Contacts & Communication

- **Contacts Integration**
  - View, Add, Edit contacts
  - Fast search with debounce + paging

- **One-Tap Actions**
  - Call
  - SMS
  - Email

- **Intent Handling (Refactored ✨)**
  - Unified intent launcher with:
    - Toast feedback
    - Error handling
    - Clean reusable API

---

## 🧩 Navigation System (Modern)

- Built using **Navigation3 (Experimental Compose-first API)**

### Key Highlights:
- State-driven navigation
- No fragment overhead
- Instant transitions
- Predictive back support

### Smart Behavior:
- Automatically opens `CallScreen` on incoming call
- Pops screen when call ends
- Closes app if launched only for call

---

## 🎨 UI/UX Highlights

- Smooth animations (`slideInHorizontally`, `slideOutHorizontally`)
- Material 3 design system
- Ripple feedback on buttons
- Disabled + active states handled cleanly
- Clean, minimal call controls UI

---

## 🛠️ Technical Stack

| Category        | Technology |
|----------------|----------|
| **Language**    | Kotlin |
| **UI Framework**| Jetpack Compose (Material 3) |
| **Architecture**| MVVM + Clean Architecture |
| **Navigation**  | Navigation3 (Experimental) |
| **DI**          | Koin |
| **Telephony**   | InCallService, Telecom API |
| **State**       | Coroutines, StateFlow |
| **Image**       | Coil |
| **Logging**     | Timber |
| **Phone Utils** | libphonenumber |
| **Build**       | Gradle (Kotlin DSL) |

---

## 🏗️ Architecture Overview

### 📡 Telecom Flow

1. App requests **Default Dialer Role**
2. System binds `InCallService`
3. Incoming/Outgoing call triggers:
  - `onCallAdded()`
4. App launches UI instantly
5. `CallViewModel` updates UI state
6. UI reacts via Compose

---

### 🔁 Call State Flow

Telecom System
↓
InCallService
↓
CallManager
↓
CallViewModel (StateFlow)
↓
Jetpack Compose UI

---

### 📱 Navigation Flow

Recent Calls → Call Screen
Contacts → Contact Detail → Call Screen
Incoming Call → Direct Call Screen (no lag)

---

## ⚡ Performance Optimizations

- Precomputed navigation stack (removes cold start lag)
- Debounced search (300ms)
- Paging for contacts
- Minimal recompositions
- Efficient state observation

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

## 📥 Installation

### Prerequisites

- Android Studio Ladybug+
- Android SDK 30+
- Physical device required (for Telecom APIs)

---

### Setup

git clone https://github.com/dontknow492/Ghost-Caller.git

Open in Android Studio
Sync Gradle
Run on real device
Accept Default Dialer permission

---

## ⚠️ Important Notes

- Emulator does NOT support real call testing
- Must grant:
  - Phone permissions
  - Default Dialer role
- Some features require Android 12+ (blur effects)

---

## 🚀 Future Roadmap

- 📊 Call analytics dashboard
- 🤖 Spam detection (ML-based)
- ☁️ Cloud sync for contacts & logs
- 🎙️ Call recording (region-dependent)
- 🧠 Smart suggestions (AI-powered dialer)

---

## 🤝 Contributing

Pull requests are welcome!

If you'd like to improve UI, performance, or features:

Fork the repo
Create a feature branch
Submit PR

---

## 📜 License

MIT License © Ghost Caller

---

## 👻 Author

Built with ❤️ by Ghost Rider

"Not just a dialer — a smarter way to communicate."