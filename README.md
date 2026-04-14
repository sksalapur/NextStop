# 🚌 NextStop

NextStop is a robust, dynamic Android application designed to track and manage school/institutional bus fleets. Built with modern UI patterns natively using Jetpack Compose, NextStop offers real-time fleet synchronization, smart asynchronous ETA tracking, and seamless cross-platform Firebase backend integrations.

## ✨ Key Features

### 👨‍🎓 Student Module
* **Live GPS Tracking**: View the live location of assigned buses updated in real-time.
* **Intelligent ETA Calculations**: Dynamically resolves spatial location distances to accurately compute Estimated Time of Arrival (ETA) based on geographical data.
* **Custom Boarding Alerts**: Easily filter through Stops natively utilizing the Google Places API. Assign a custom Boarding Stop to receive timely Push Notifications and specific individual ETAs exactly when your bus is approaching your designation.
* **Dynamic Geofenced Polylines**: The tracker elegantly maps visually passed routes versus trailing tracking lines instantly based on continuous ping bounds.

### 👑 Administrator Module
* **Live Fleet Parity**: Directly mirror any active route/bus on the network natively through the Fleet Tracking Dashboard UI.
* **Complete CRUD Configuration**: Create and distribute Drivers, custom Fleet Buses, and interconnected Route Stops using integrated Google Maps search components to orchestrate fleet networking.
* **Route Polyline Optimization**: Visually assign and adjust route stops directly on top of natively configured Google Place map overlays.

## 💻 Tech Stack
- **UI Architecture:** Jetpack Compose, Material 3
- **Language:** Kotlin
- **Backend Infrastructure:** Firebase (Authentication, Cloud Firestore, Realtime Database)
- **Map Services:** Google Maps SDK (Vector Overlay Support) & Google Places API (Regional IN Restrictions)
- **Dependency Injection:** Dagger Hilt
- **Coroutines & ViewModels:** Native `StateFlow` threading.

## 🚀 Setup & Installation

To run this application locally, you must provide your own configured API keys.

1. **Clone the Repository**
   ```bash
   git clone https://github.com/yourusername/NextStop.git
   ```

2. **Add Firebase Credentials**
   Place your configured `google-services.json` inside the `app/` directory. Be sure Firestore and Realtime Database are structurally instantiated.

3. **Provide Maps API Access**
   Inside your project's root `local.properties` file, insert your Google Maps SDK API key:
   ```properties
   MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY
   ```
   *(Note: For security, `local.properties` is ignored by git natively).*

4. **Compile & Run**
   Sync grandle and assemble locally using Android Studio or `./gradlew assembleDebug`.

---
*Built tightly with Kotlin and Jetpack Compose for the ultimate responsive Android Experience.*
