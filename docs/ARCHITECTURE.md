# System Architecture

## Layers

### Presentation

XML layouts, Activities, Fragments, RecyclerView adapters and Material Components.

### ViewModel

- `AuthViewModel`
- `ReportViewModel`
- `SosViewModel`

ViewModels expose StateFlow-based UI state and launch coroutines.

### Repository

Interfaces isolate the UI from data sources:

- `AuthRepository`
- `ReportRepository`
- `SosRepository`
- `ImageStorageRepository`
- `AiRepository`

`AppContainer` selects Firebase/Supabase repositories when cloud keys exist; otherwise it selects in-memory demo repositories.

### Data sources

- Firebase Auth
- Cloud Firestore
- Firebase AI Logic
- Supabase Storage REST API through Retrofit
- Fused Location Provider
- CameraX
- MapLibre + OpenStreetMap
- Firebase Cloud Messaging receiver

## Main workflow

```text
Photo + description + GPS
          ↓
Gemini AI analysis
          ↓
User verifies AI suggestion
          ↓
Image upload → image URL
          ↓
Firestore report
          ↓
Authority real-time dashboard
          ↓
Status update
          ↓
Citizen tracking / community verification
          ↓
Resolved
```

## Firestore collections

### users/{uid}

```text
uid
name
email
role: CITIZEN | AUTHORITY | ADMIN
fcmToken
createdAt
```

### reports/{reportId}

```text
reporterId
reporterName
category
description
latitude
longitude
imageUrl
status
severity
aiCategory
aiSeverity
aiSummary
assignedAuthority
confirmations
createdAt
updatedAt
```

### reports/{reportId}/confirmations/{uid}

Prevents the same signed-in account from confirming the same report twice in cloud mode.

### sos_alerts/{sosId}

```text
userId
userName
latitude
longitude
status: ACTIVE | RESOLVED
createdAt
```
