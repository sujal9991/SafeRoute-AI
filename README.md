# 🚗 SafeRoute AI

> An Android navigation application focused on safer, smarter, and more context-aware route planning.

SafeRoute AI is a Google Maps-inspired Android navigation application that combines GPS navigation, multiple route alternatives, live traffic, weather conditions, and route-aware information to help users make better travel decisions.

The goal is not simply to find the fastest route, but to provide the user with more information about the conditions affecting their journey.

## ✨ Features

### 🗺️ Navigation
- Real-time GPS location
- Interactive OpenStreetMap map
- Current-location marker
- Destination search
- Location autocomplete
- Multiple route alternatives
- Fastest and alternative route comparison
- Route distance and ETA
- Turn-by-turn instructions
- Live route progress
- Navigation mode
- Recenter map
- Voice turn-by-turn instructions
- Arrival detection
- Android system Back gesture support
- Google Maps-style navigation interface

### 🚦 Live Traffic
SafeRoute AI integrates TomTom Traffic APIs to provide live traffic information.

- Live traffic incidents
- Traffic flow information
- Traffic severity
- Traffic-aware ETA
- Traffic delay estimation
- Route-specific traffic analysis
- Traffic updates during navigation
- Traffic incident detection
- Road closure information where available

### 🌦️ Weather
Weather information is retrieved using Open-Meteo.

- Current temperature
- Precipitation
- Wind speed
- Wind gusts
- Weather condition
- Severe weather detection
- Weather information along the selected route
- Weather updates during navigation

### 🔎 Destination Search
SafeRoute AI uses OpenStreetMap's Nominatim service for destination search.

- Search destinations in India
- Search suggestions while typing
- Search by place name/address
- Current-location-aware search
- Select destination directly from search results

## 🧠 Project Vision

Traditional navigation applications primarily optimize for:

> "How do I get there fastest?"

SafeRoute AI aims to answer:

> "Which route is the best choice considering the current conditions?"

The long-term goal is to combine traffic, weather, road conditions, incidents, route characteristics, vehicle information, time of travel, historical data, and machine learning to produce a personalized Safety Score for each route.

## 🏗️ Technology Stack

### Android
- Kotlin
- Jetpack Compose
- Material 3
- Android SDK
- Google Fused Location Provider

### Maps
- OpenStreetMap
- osmdroid

### Routing
- OSRM
- OpenStreetMap routing infrastructure

### Search
- OpenStreetMap Nominatim

### Traffic
- TomTom Traffic API
- TomTom Traffic Incidents
- TomTom Traffic Flow

### Weather
- Open-Meteo API

### Voice Navigation
- Android Text-to-Speech

## 📱 Application Architecture

```text
                        ┌─────────────────────┐
                        │     SafeRoute AI    │
                        └──────────┬──────────┘
                                   │
             ┌─────────────────────┼─────────────────────┐
             │                     │                     │
             ▼                     ▼                     ▼
       ┌───────────┐         ┌───────────┐        ┌────────────┐
       │ GPS /     │         │ Destination│        │  Vehicle   │
       │ Location  │         │  Search    │        │  Profile   │
       └─────┬─────┘         └─────┬─────┘        └────────────┘
             │                     │
             ▼                     ▼
       ┌─────────────────────────────────┐
       │          Route Engine            │
       │              OSRM                │
       └───────────────┬─────────────────┘
                       │
             ┌─────────┼─────────┐
             │         │         │
             ▼         ▼         ▼
        ┌────────┐ ┌────────┐ ┌────────┐
        │Traffic │ │Weather │ │  Road  │
        │TomTom  │ │Open-   │ │  Data  │
        │        │ │Meteo   │ │        │
        └────┬───┘ └────┬───┘ └────┬───┘
             │          │          │
             └──────────┼──────────┘
                        ▼
                ┌───────────────┐
                │ Route Analysis│
                └───────┬───────┘
                        │
                        ▼
                ┌───────────────┐
                │ Safety Score  │
                │   (Planned)   │
                └───────────────┘
```

## 📂 Project Structure

```text
SafeRoute_AII/
│
├── app/
│   └── src/
│       └── main/
│           ├── java/
│           │   └── com/
│           │       └── saferoute/
│           │           └── ai/
│           │               ├── core/
│           │               ├── map/
│           │               ├── model/
│           │               ├── navigation/
│           │               ├── search/
│           │               ├── traffic/
│           │               ├── weather/
│           │               ├── LocationService.kt
│           │               ├── MainActivity.kt
│           │               └── TomTomTestActivity.kt
│           └── res/
│
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
├── gradlew.bat
└── README.md
```

## 🚀 Getting Started

### Requirements
- Android Studio
- JDK 11+
- Android SDK
- Android device or emulator
- Internet connection
- Android 8.0 / API 26 or newer

### 1. Clone the repository

```bash
git clone https://github.com/sujal9991/SafeRoute-AI.git
cd SafeRoute_AII
```

### 2. Open in Android Studio

Open the project directory in Android Studio and allow Gradle to sync.

### 3. Configure TomTom API Key

Create or edit:

```text
local.properties
```

Add:

```properties
TOMTOM_API_KEY=YOUR_TOMTOM_API_KEY
```

The API key is intentionally stored in `local.properties` and should never be committed to GitHub.

### 4. Build the application

Windows:

```powershell
.\gradlew.bat assembleDebug
```

The generated APK will be located at:

```text
app\build\outputs\apk\debug\app-debug.apk
```

### 5. Install on an Android device

Enable Developer Options and USB Debugging.

Then:

```powershell
adb devices
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## 🔑 API Services

| Service | Purpose |
|---|---|
| OpenStreetMap | Map data |
| osmdroid | Android map rendering |
| Nominatim | Destination search |
| OSRM | Route calculation |
| TomTom Traffic | Live traffic |
| Open-Meteo | Weather |

The project is designed around free/open or free-tier services.

## 🚦 Traffic Processing

Traffic information is retrieved from TomTom and analyzed against the selected route.

```text
Route
  ↓
TomTom Traffic Flow
  ↓
Traffic Segments
  ↓
Route Segment Matching
  ↓
Traffic Level
  ↓
Estimated Delay
  ↓
Traffic-adjusted ETA
```

Traffic conditions can be classified into:

```text
FREE
LIGHT
MODERATE
HEAVY
CLOSED
```

Traffic is refreshed periodically during active navigation.

## 🌦️ Weather Processing

SafeRoute AI retrieves current weather conditions for a representative point along the selected route.

Current information includes:

```text
Temperature
Precipitation
Wind Speed
Wind Gusts
Weather Condition
Severe Weather
```

## 🧭 Navigation Flow

```text
Open App
   ↓
Open Explore
   ↓
Search Destination
   ↓
Select Destination
   ↓
Calculate Routes
   ↓
Compare Routes
   ↓
Select Route
   ↓
Start Navigation
   ↓
Live GPS Tracking
   ↓
Turn-by-turn Instructions
   ↓
Traffic + Weather Updates
   ↓
Arrival
```

## 📱 Android Back Gesture

SafeRoute AI supports Android's system Back gesture.

The behavior is state-aware:

```text
Navigation
    ↓ Back
Route Preview

Search
    ↓ Back
Close Search

Destination Selected
    ↓ Back
Clear Destination

Normal Explore
    ↓ Back
Home
```

## 🔮 Future Development

### Safety Intelligence
- Vehicle profile
- Vehicle-aware routing
- Road condition analysis
- Safety score
- Risk zones
- Route safety comparison
- Weather-aware route scoring
- Incident-aware route scoring
- Dynamic safety recommendations

### AI & Machine Learning
- Accident risk prediction
- Personalized driver risk profile
- Driving behavior analysis
- Camera-based hazard detection
- Pothole and road damage detection
- Community hazard reporting
- Predictive weather impact
- Vehicle-specific risk prediction

### Analytics
- Trip history
- Safety heatmaps
- Driver statistics
- Risk trends
- Route history
- Safety reports

## 🎯 Long-Term Goal

The ultimate goal of SafeRoute AI is to move beyond conventional navigation.

Instead of only showing:

```text
FASTEST ROUTE
```

SafeRoute AI aims to provide:

```text
┌─────────────────────────────┐
│       ROUTE COMPARISON      │
├─────────────────────────────┤
│ 🚗 Fastest                 │
│ 42 min                      │
│                             │
│ ⚖ Balanced                 │
│ 45 min                      │
│                             │
│ 🛡 Safest                  │
│ 48 min                      │
│ Safety Score: 91/100        │
└─────────────────────────────┘
```

The system will eventually explain why a route is considered safer.

For example:

> Route B is recommended because it has lower traffic, fewer reported incidents, better weather conditions, and lower predicted risk despite being 4 minutes longer.

## 🔐 Security

Never commit:

```text
local.properties
```

or any file containing API keys.

The TomTom API key is loaded locally through:

```text
BuildConfig.TOMTOM_API_KEY
```

API credentials should remain private.

## 📌 Project Status

**Status: 🚧 Active Development**

### Currently Working
- ✅ Android application
- ✅ Jetpack Compose UI
- ✅ GPS location
- ✅ OpenStreetMap map
- ✅ Destination search
- ✅ Search autocomplete
- ✅ OSRM routing
- ✅ Multiple routes
- ✅ Turn-by-turn navigation
- ✅ Voice navigation
- ✅ Route progress
- ✅ TomTom traffic incidents
- ✅ TomTom traffic flow
- ✅ Traffic-adjusted ETA
- ✅ Open-Meteo weather
- ✅ Weather during navigation
- ✅ Android Back gesture

### Planned
- 🚧 Safety scoring
- 🚧 Vehicle-aware routing
- 🚧 Risk prediction
- 🚧 Machine learning
- 🚧 Driver behavior analysis
- 🚧 Camera hazard detection

## 👨‍💻 Developer

**Sujal Bhandarge**

SafeRoute AI is a computer science project exploring the combination of mobile development, navigation systems, APIs, real-time data, geographic information systems, artificial intelligence, and machine learning.

## 📄 License

This project is currently intended for educational and development purposes.

Individual third-party services and datasets used by SafeRoute AI remain subject to their respective licenses and terms of use.
