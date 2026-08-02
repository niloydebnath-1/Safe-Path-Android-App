# Safe Path

Safe Path is an Android-based community safety and hazard reporting prototype. It allows citizens to report local hazards, view nearby incidents on a live map, send prototype SOS alerts, and communicate reports to approved authorities.

The application uses role-based access for Citizens, Authorities, and Administrators.

---

## Project Overview

Safe Path was developed to make community hazard reporting faster, more organized, and location-aware.

Users can submit reports with:

- A captured image
- Description of the incident
- GPS location
- Hazard category
- Severity level
- AI-assisted report analysis

Submitted reports are stored in Cloud Firestore and displayed as live markers on the safety map.

> **Important:** Safe Path is currently a prototype. It is not connected to police, fire services, hospitals, or any official emergency service.

---

## Main Features

### Citizen Panel

Citizens can:

- Register and log in
- Capture and submit hazard reports
- Add descriptions and GPS locations
- Analyze reports using AI
- View submitted reports
- View community hazards on a live map
- Confirm existing hazard reports
- Send prototype SOS alerts
- Receive nearby high-risk hazard warnings

### Authority Panel

Authority accounts require administrator approval.

Supported authority types:

- Police
- City Corporation
- Disaster Management Board

Authorities can:

- View reports assigned to their authority type
- Review report details
- Update report status
- Monitor active SOS alerts
- Resolve assigned incidents

### Administrator Panel

Administrators can:

- Review pending Authority and Admin accounts
- Approve or reject accounts
- View all submitted reports
- Manage account approval status
- Access administrative monitoring features

---

## Authority-Based Report Filtering

| Authority | Assigned Reports |
|---|---|
| Police | Crime, theft, robbery, assault, snatching and security incidents |
| City Corporation | Damaged roads, drains, manholes, civic hazards and infrastructure issues |
| Disaster Management Board | Flooding, waterlogging, fire and disaster-related incidents |
| Admin | Access to all reports |

---

## Safety Map

The application uses MapLibre with an OpenFreeMap vector style.

Map features include:

- Real-time Firestore hazard markers
- Marker information windows
- Report details navigation
- Automatic camera movement to reported locations
- Current-location navigation
- Nearby danger alerts
- Severity-based incident display

---

## Technology Stack

### Android

- Kotlin
- XML layouts
- Material Components
- MVVM architecture
- Repository pattern
- Kotlin Coroutines
- StateFlow
- View Binding
- Navigation Component

### Firebase

- Firebase Authentication
- Cloud Firestore
- Firebase Cloud Messaging
- Firebase App Check
- Firebase AI Logic

### Maps and Location

- MapLibre Native
- OpenFreeMap
- OpenStreetMap data
- Google Fused Location Provider

### Media and Networking

- CameraX
- Glide
- Retrofit
- Supabase Storage support

---

## User Roles

### Citizen

```text
Registration
→ Automatically approved
→ Citizen Home
