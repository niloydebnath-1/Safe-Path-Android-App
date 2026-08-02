# Short Technical Write-up

## Overview

Nirapod is a role-based Android public-safety reporting prototype. Citizens can report crime-prone locations and infrastructure hazards with a photo, description and GPS coordinates. Gemini AI suggests a category, severity, concise summary and responsible authority. Reports appear on a community map and in an authority dashboard. Authorities can verify reports and update their status. Citizens can track progress and confirm that an issue is still present. A prototype SOS flow shares the user's current location with the in-app control room.

## Architecture

The Android client uses Kotlin, XML layouts and Material Components. The project follows MVVM with a Repository abstraction. Fragments observe ViewModel StateFlows. Repositories encapsulate Firebase, Supabase, AI, location and demo data sources.

Firebase Authentication identifies users. Cloud Firestore stores users, reports, confirmation records and SOS alerts. Firestore snapshot listeners provide real-time updates. Images are uploaded to Supabase Storage through a Retrofit API client. Glide loads remote and local image URIs. CameraX captures evidence photos. Fused Location Provider supplies report and SOS coordinates. MapLibre renders OpenStreetMap tiles and report markers.

## AI workflow

Firebase AI Logic sends the hazard image and user description to the Gemini Developer API model configured in `local.properties`. The prompt requests JSON containing category, severity, summary, risk and suggested authority. The result is displayed as a suggestion. Users can edit the category, and authorities must verify the report. AI does not make the final decision.

## Report routing

The prototype stores the AI-suggested authority in the report. The authority dashboard receives all reports through Firestore real-time listeners. In production, jurisdiction boundaries and department rules can replace this simple routing field.

## Security

Firebase App Check is initialized with the debug provider during development and Play Integrity for release builds. Firestore rules restrict report creation to the signed-in reporter, authority status changes to authority/admin accounts, confirmation creation to the matching user ID, and SOS access to the owner or authority. Secrets are read from `local.properties`, which should not be committed.

## Free-tier design

The app can run entirely in offline demo mode. Cloud mode is designed for Firebase Spark/Gemini Developer API free-tier quotas and an optional Supabase free project. All free services have quotas and terms that must be checked before a public launch.
