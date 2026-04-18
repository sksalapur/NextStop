<h1 align="center">🚌 NextStop</h1>

<p align="center">
  <strong>Your bus is 3 minutes away. We checked.</strong>
</p>

<p align="center">
  <em>Real-time school bus tracking with live GPS, per-stop ETA, geofenced polylines,<br/>and push notifications — built entirely in Kotlin with Jetpack Compose.</em>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.0+-7F52FF?logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?logo=jetpackcompose&logoColor=white" />
  <img src="https://img.shields.io/badge/Firebase-Auth_%7C_Firestore_%7C_RTDB-FFCA28?logo=firebase&logoColor=black" />
  <img src="https://img.shields.io/badge/Google_Maps_SDK-Places_API-34A853?logo=googlemaps&logoColor=white" />
  <img src="https://img.shields.io/badge/Dagger_Hilt-DI-232F3E" />
</p>

---

## 🤔 The Problem

Every parent and student has asked the same question: **"Where is the bus?"**

Current solutions are either:
- 📞 **Phone calls** — "Driver, where are you?" (annoying for everyone)
- 📍 **WhatsApp location sharing** — unreliable, battery-draining, no ETA
- 💰 **Commercial fleet trackers** — expensive, designed for logistics companies, not schools

**NextStop is purpose-built for schools.** Students see their bus on a live map, get a per-stop ETA, and receive a push notification when the bus is approaching their stop. Admins manage the entire fleet — routes, buses, drivers, stops — from a single dashboard.

---

## ⚡ How It Works

```
┌─────────────────────────────────────────────────────────────────┐
│                     NextStop System                              │
│                                                                  │
│  ┌──────────────────┐                                           │
│  │  DRIVER APP       │                                           │
│  │  Foreground Service│                                          │
│  │  ├── FusedLocation │──── GPS ping every 4s ────┐             │
│  │  │   (HIGH_ACCURACY)│                           │             │
│  │  ├── Speed filter   │                           ▼             │
│  │  │   (>0.5 m/s)     │               ┌──────────────────┐     │
│  │  └── Bearing data   │               │ Firebase Realtime │     │
│  └──────────────────┘               │ Database          │     │
│                                      │ /live/{busId}     │     │
│                                      │ ├── latitude      │     │
│  ┌──────────────────┐               │ ├── longitude     │     │
│  │  STUDENT APP      │◄──── Live ────│ ├── speed        │     │
│  │                   │   listener    │ ├── bearing      │     │
│  │  ├── Google Map   │               │ ├── timestamp    │     │
│  │  │   with route   │               │ └── active       │     │
│  │  │   polyline     │               └──────────────────┘     │
│  │  │                │                                          │
│  │  ├── Per-stop ETA │    Haversine distance ÷ current speed    │
│  │  │   computation  │    = minutes to each stop                │
│  │  │                │                                          │
│  │  ├── Proximity    │    If ETA < 5 min for assigned stop      │
│  │  │   notification │    → Firebase Cloud Messaging push       │
│  │  │                │                                          │
│  │  └── Stop progress│    Animated progress bar with bus icon   │
│  │      bar          │    tracking across the route              │
│  └──────────────────┘                                           │
│                                                                  │
│  ┌──────────────────┐               ┌──────────────────┐        │
│  │  ADMIN DASHBOARD  │               │ Cloud Firestore  │        │
│  │                   │◄────────────►│                  │        │
│  │  ├── Manage Buses │               │ /buses           │        │
│  │  ├── Manage Routes│               │ /routes          │        │
│  │  ├── Assign Drivers│              │ /users           │        │
│  │  ├── CRUD Stops   │               │ /assignments     │        │
│  │  └── Fleet tracker│               └──────────────────┘        │
│  └──────────────────┘                                            │
└─────────────────────────────────────────────────────────────────┘
```

---

## ✨ Features

### 👨‍🎓 Student Experience

#### 📍 Live GPS Tracking
- Real-time bus position on a Google Map with custom bus marker icon
- Location updates every **4 seconds** via `FusedLocationProviderClient` at `PRIORITY_HIGH_ACCURACY`
- **Location freshness indicator**: "● Live" (green), "● 3 min ago" (yellow), "● Offline" (red) — students always know how fresh the data is

#### ⏱️ Intelligent ETA
- **Per-stop ETA computation**: Haversine distance from bus coordinates to each stop, divided by current GPS speed
- **Boarding stop ETA**: Personalized "Arrives at *your stop* in 4 min" — not a generic fleet-wide estimate
- **Edge case handling**: "Calculating...", "Bus has already passed", "Bus is far away"

#### 🔔 Smart Boarding Alerts
- Set your **personal boarding stop** using Google Places Autocomplete (restricted to India)
- Toggle proximity notifications — get a push when the bus is approaching *your* stop
- Stop assignments persist in Firestore — works across app restarts

#### 🗺️ Dynamic Route Visualization
- **Google Directions API polylines**: Road-snapped route paths (not naive straight lines between stops)
- **Geofenced stop progression**: "Passed" stops dim into gray, "Next" stop highlights in primary color
- **Animated progress bar**: Bus icon slides across a stop-by-stop progress track with smooth `animateFloatAsState` transitions
- **Bottom sheet map**: BottomSheetScaffold with draggable stop list + map overlay

#### 🔄 Direction Toggle
- **To College** / **To Home** / **All Scheduled** — filter buses by trip direction
- Only shows buses that pass through your boarding stop (unless viewing "All Scheduled")

### 👑 Administrator Dashboard

#### 🚍 Fleet Management CRUD
- **Buses**: Create, edit, assign numbers and types
- **Routes**: Define routes with ordered stops, departure times, assigned buses
- **Stops**: Add stops via Google Places Autocomplete with lat/lng auto-fill, reorder with drag handles
- **Drivers**: Assign drivers to buses, manage authentication and roster

#### 📊 Live Fleet Tracking
- Mirror any active route on the admin dashboard — see exactly what students see
- Fleet-wide overview of which buses are active, inactive, or offline

---

## 🛠️ Tech Stack

| Layer | Technology | Why This Choice |
|-------|------------|-----------------|
| **UI** | Jetpack Compose + Material 3 | Declarative UI with BottomSheetScaffold, AnimatedContent |
| **Language** | Kotlin 2.0 | Coroutines, StateFlow, type-safe throughout |
| **Architecture** | MVVM + `StateFlow` + Dagger Hilt DI | Clean separation, testable, injectable |
| **Auth** | Firebase Auth | Role-based: Student / Driver / Admin |
| **Metadata DB** | Cloud Firestore | Routes, buses, stops, users, assignments |
| **Realtime DB** | Firebase Realtime Database | Live GPS coordinates — low latency, high write throughput |
| **Maps** | Google Maps SDK (Compose) | Vector overlays, Polylines, Markers, Camera animations |
| **Places** | Google Places API (New) | Autocomplete restricted to IN region for stop selection |
| **Directions** | Google Directions API | Road-snapped polyline computation between stops |
| **Location** | FusedLocationProviderClient | High-accuracy GPS with configurable intervals |
| **Foreground Service** | LocationForegroundService + Binder | Persistent tracking that survives app backgrounding |
| **Notifications** | Firebase Cloud Messaging | Per-stop proximity push alerts |

---

## 📐 Project Structure

```
NextStop/
├── app/src/main/java/com/yourteam/nextstop/
│   ├── data/repository/
│   │   ├── AdminRepository.kt        # Fleet CRUD operations
│   │   ├── AuthRepository.kt         # Firebase Auth + role detection
│   │   ├── DriverRepository.kt       # Live location writes to RTDB
│   │   └── StudentRepository.kt      # Stop assignments, bus tracking
│   │
│   ├── di/
│   │   └── AppModule.kt              # Dagger Hilt module — Firebase, Maps
│   │
│   ├── models/
│   │   ├── Bus.kt, Route.kt, User.kt # Domain models
│   │   ├── LiveLocation.kt           # RTDB GPS data class
│   │   ├── TripState.kt              # Sealed class: Running | Stopped
│   │   └── CollegeLocation.kt        # Campus coordinates
│   │
│   ├── service/
│   │   ├── LocationForegroundService.kt  # GPS tracking service (4s interval)
│   │   └── ProximityNotificationService.kt # FCM push notification handler
│   │
│   ├── ui/
│   │   ├── student/
│   │   │   ├── StudentHomeScreen.kt       # Fleet dashboard with direction toggle
│   │   │   ├── StudentViewModel.kt        # Live bus tracking, ETA computation
│   │   │   └── HomeStopSetupScreen.kt     # Boarding stop selection
│   │   ├── driver/
│   │   │   ├── DriverHomeScreen.kt        # Trip start/stop with speed display
│   │   │   └── DriverViewModel.kt         # Foreground service binding
│   │   ├── admin/
│   │   │   ├── AdminHomeScreen.kt         # Tab layout: Dashboard, Manage, Assign
│   │   │   ├── AdminDashboardTab.kt       # Live fleet overview
│   │   │   ├── AdminManageTab.kt          # Buses, Routes, Stops CRUD
│   │   │   └── AdminViewModel.kt          # Fleet management state
│   │   ├── auth/
│   │   │   ├── LoginScreen.kt             # Email/password + role selection
│   │   │   └── AuthViewModel.kt           # Auth state machine
│   │   └── components/
│   │       ├── TrackingComponents.kt      # SharedTrackerScreen, BusTrackingMap, StopProgressBar
│   │       ├── CustomPlacesSearchField.kt # Google Places autocomplete widget
│   │       ├── ShimmerSkeleton.kt         # Loading skeleton components
│   │       └── NextStopTopBar.kt          # Reusable top app bar
│   │
│   ├── util/
│   │   ├── LocationUtils.kt              # Haversine, ETA, vector bitmap helpers
│   │   └── DirectionsFetcher.kt          # Google Directions API polyline decoder
│   │
│   └── navigation/
│       ├── AppNavigation.kt              # Role-based navigation graph
│       └── NavRoutes.kt                  # Route definitions
│
├── google-services.json                   # Firebase config (gitignored)
└── build.gradle.kts
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug+ · Kotlin 2.0
- Firebase project (Auth + Firestore + Realtime Database)
- Google Cloud project with Maps SDK, Places API, Directions API enabled

### Setup
1. Clone the repo
2. Place `google-services.json` in `app/`
3. Add your Maps API key to `local.properties`:
   ```properties
   MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY
   ```
4. Sync Gradle and run on your device

### Firebase Structure
| Collection/Path | Purpose |
|-----------------|---------|
| `users/{uid}` | User profile with role (student/driver/admin) |
| `buses/{busId}` | Bus metadata (number, type, capacity) |
| `routes/{routeId}` | Route with ordered stops, departure time, assigned bus |
| `assignments/{id}` | Driver ↔ Bus ↔ Route assignments |
| `live/{busId}` (RTDB) | Real-time GPS coordinates (4s updates) |

---

<p align="center">
  <strong>Never ask "where is the bus?" again.</strong><br/>
  <em>Real-time tracking, built for schools, powered by Kotlin.</em>
</p>

<p align="center">
  Made with ❤️ by <a href="https://github.com/sksalapur">Shashank Salapur</a>
</p>
