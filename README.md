# ✦ ZorvynOne | Next-Gen Personal Finance

> A premium, AI-powered personal finance and habit-tracking Android application built entirely with Jetpack Compose. 

ZorvynOne rethinks the standard budgeting app by combining real-time algorithmic health scoring, hyper-premium 3D UI interactions, and on-device privacy. It doesn't just track your money—it grades your financial discipline and provides Gemini AI-driven insights to help you build better wealth habits.

---

## Key Features

* **Algorithmic Health Score:** Moves beyond simple balances. An interactive, real-time "Zorvyn Score" that reacts instantly to your income, expenses, and accepted financial habits.
* **Dynamic 'Apple-Style' 3D UI:** Features an immersive, breathing metallic balance card with rolling number animations, deep casting shadows, and time-aware typography that adapts the greeting based on the user's clock.
* **Gemini AI Integration:** A custom-built AI engine that analyzes local spending patterns to generate personalized financial insights and actionable habit plans.
* **Flawless Navigation State:** Implements official Google-standard Bottom Navigation routing (`launchSingleTop`, `restoreState`) to prevent backstack freezing and ensure buttery-smooth tab switching.
* **Gesture-Driven UX:** Swipe-to-delete transaction management and fluid `animateContentSize` expanding habit cards.

---

##  Tech Stack & Architecture

This project strictly adheres to modern Android Development standards:
* **UI:** 100% Kotlin & Jetpack Compose (Material 3)
* **Architecture:** Clean Architecture principles with MVVM (Model-View-ViewModel)
* **Local Database:** Room (SQLite) for offline-first, zero-latency data processing
* **Asynchronous Operations:** Kotlin Coroutines & StateFlows for reactive UI updates
* **Navigation:** Jetpack Navigation Compose
* **AI Engine:** Custom REST implementation to interface with Google's Gemini API

---

## Technical Decisions & Trade-offs

* **UI/UX Architecture:** Built entirely with Jetpack Compose to leverage a purely state-driven UI, enabling complex, high-performance visual elements—like the 3D animated balance card and dynamic typography—without the overhead of traditional XML layouts.
* **Navigation State Management:** Engineered a unified Compose Navigation graph utilizing `launchSingleTop` and `restoreState` routing rules to effectively manage the backstack, preventing memory leaks and UI freezing during rapid bottom-tab switching.
* **AI Integration Trade-off:** Opted to build a custom REST client to communicate directly with the Gemini model rather than importing heavy official SDKs, trading rapid setup convenience for a significantly smaller APK size and granular control over JSON parsing.
* **Data Privacy & Latency (Trade-off):** Chose a local, offline-first SQLite architecture to process transactions and calculate the "Zorvyn Health Score" dynamically on-device, prioritizing strict user data privacy and zero-latency UI updates over multi-device cloud synchronization.
* **State Reactivity:** The application's core logic is heavily reactive; accepting an AI-suggested financial habit immediately recalculates global state variables via Kotlin Flows, triggering seamless, real-time visual updates across multiple deeply nested screens.

---

## 🚀 Getting Started

### Prerequisites
* Android Studio (Latest stable version recommended)
* Minimum SDK: 24 (Android 7.0)

### Installation
1. Clone the repository:
   ```bash
   git clone [https://github.com/yourusername/ZorvynOne.git](https://github.com/yourusername/ZorvynOne.git)
