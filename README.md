# SAA INVENTORY APP
### For Shri Amardevi Automobile & Spare Part

A complete, production-ready, offline-first Android inventory management system crafted specifically for automobile spare parts retail stores and workshops.

---

## 🌟 Key Capabilities

### 1. 100% Offline-First Architecture
- Built with **Android Jetpack Room & SQLite**.
- Operates completely without an active internet connection.
- Zero cloud dependency, no subscription fees, and no mandatory user logins.

### 2. Wi-Fi & Hotspot Peer-to-Peer Sync
- Multiple Android phones and tablets running the app can sync their database in real-time over the **same Wi-Fi network or mobile hotspot**.
- Uses an embedded local TCP socket server with conflict resolution and cryptographic verification (`SAA-AMARDEVI-2026`).
- Automatic peer discovery and instant 1-tap manual IP sync.

### 3. श्री AI Assistant ("Shree") — Voice & Reorder Intelligence
- **Continuous Purchase & Order Tracking**: Analyzes stock-in purchase bills and stock-out counter sales to identify shortage trends.
- **Pending Parts Audit**: Automatically generates a reorder list of critical zero-stock parts, items below safety thresholds, and high-demand items.
- **Text-To-Speech (TTS)**: Bilingual voice output (Hindi & English) for hands-free stock queries and spoken reports.
- **1-Tap WhatsApp Supplier Orders**: Pre-formats full purchase orders with part numbers, quantities, rack locations, and distributor details for direct messaging.

### 4. Automobile Spare Parts Management
- **Dual-Firm Stock Division**: Separate or combined stock tracking for **SAA** (Hero, Honda, Bajaj, Yamaha) and **TVS** (TVS Genuine Spares).
- **Physical Rack Indexing**: Tracks exact bin locations (e.g., `Rack A-12, Shelf 3`) for instant part retrieval at the counter.
- **Barcode & QR Integration**: Built-in 1D barcode scanner and Code 128 label generator for part labels.
- **Distributor Ledger**: Vendor contacts, inward purchase rates, invoice numbers, and bill history.
- **Backup & Restore**: Instant local JSON export and import for full database safety.

---

## 🛠️ Tech Stack & Requirements

- **Platform**: Android (Kotlin)
- **UI Framework**: Jetpack Compose with Material Design 3 (M3)
- **Architecture**: Clean Architecture / MVVM with StateFlow & Coroutines
- **Database**: AndroidX Room (SQLite) with KSP
- **Voice Engine**: Android `TextToSpeech` (Offline Hindi & English)
- **Barcode**: ZXing Embedded Android Scanner
- **Local Network Sync**: Java NIO Sockets & Local Wi-Fi Network Transport
- **Min SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 35 (Android 15)
- **Java / JDK**: JDK 17 or higher

---

## 🚀 How to Run in Android Studio

1. **Clone or Download**:
   - Clone this repository or unzip the exported ZIP folder.
2. **Open in Android Studio**:
   - Launch **Android Studio (Ladybug / Hedgehog or newer)**.
   - Select **Open** and choose the root directory of this repository.
3. **Gradle Sync**:
   - Android Studio will automatically resolve dependencies via Gradle.
4. **Run the App**:
   - Connect an Android device via USB (or start an Android Virtual Device / Emulator).
   - Press **Shift + F10** or click the green **Run (▶)** button in Android Studio.

---

## 📱 Default Project Configuration

- **App Name**: SAA Inventory
- **Shop Title**: Shri Amardevi Automobile & Spare Part
- **Pairing Key**: `SAA-AMARDEVI-2026`
- **Default Min Stock Threshold**: 5 units
- **Allow Negative Stock**: Configurable in Settings (Blocked by default)
