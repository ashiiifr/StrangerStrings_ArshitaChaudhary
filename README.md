# HabitSync

HabitSync is a social habit tracking Android app built with Kotlin, Jetpack Compose (Material 3), MVVM, and Firebase.

## Team
- Team: Stranger Strings
- Team ID: `KPT021`
- Team Leader: Arshita Chaudhary
- Member: Mudit Sharma

## Features
- Email/password authentication with session persistence
- Premium first-launch onboarding (6-page animated pager, theme-aware)
- Habit creation and streak tracking
- Proof-based completion flow:
  - camera or gallery proof upload
  - optional skip
  - thumbnail + full preview in card
- Social layer:
  - leaderboard (rank by score)
  - activity feed (including proof completion events)
- Smooth Compose animations and polished Material 3 UI

## Architecture
- MVVM + clean modular package structure
- UI: `ui/*`
- ViewModels: `viewmodel/*`
- Data + repositories: `data/*`, `data/repository/*`
- Navigation: `navigation/*`

## Tech Stack
- Kotlin
- Jetpack Compose + Material 3
- Firebase Auth + Firestore (realtime listeners)
- Storj (S3-compatible) for proof image storage via signed URLs
- Node backend for secure signed upload URL generation

## Project Structure
- `app/` Android app
- `backend/` signed URL service for Storj proof uploads

## Setup
1. Add `google-services.json` to `app/`.
2. Fill root `.env` (Storj + backend vars).
3. Place Firebase Admin service account key at:
   - `backend/serviceAccountKey.json`
4. Sync Gradle in Android Studio after changes to Firebase/BuildConfig values.

## Run Backend
```bash
cd backend
npm install
npm start
```

## Run Android App
1. Open project in Android Studio.
2. Run `app` on emulator/physical device.
3. Keep backend running during proof upload tests.

For emulator:
- `SIGNED_UPLOAD_API_BASE_URL=http://10.0.2.2:8787`

For physical device:
- set `SIGNED_UPLOAD_API_BASE_URL=http://<your-laptop-lan-ip>:8787`

## Onboarding Behavior
- First launch: `Entry -> Onboarding -> Login/Home`
- Returning user:
  - logged in: `Entry -> Home`
  - logged out: `Entry -> Login`
- Reset onboarding (for demo/testing):
  - clear app data from device/emulator settings

## Firestore Notes
- Leaderboard query uses composite ordering (`score DESC`, `lastUpdated DESC`), so create the suggested Firestore index when prompted.
- Security rules for production are in:
  - `firestore.rules`
- To apply rules quickly:
  1. Firebase Console -> Firestore Database -> Rules
  2. Paste contents of `firestore.rules`
  3. Publish

## Security Note
- Never commit real secrets (`.env`, service account keys, Storj secret key).
- Rotate keys immediately if exposed.
