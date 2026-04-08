# HabitSync Proof Backend (Storj)

This backend generates signed Storj upload URLs and serves short-lived proof image URLs.

## 1) Prerequisites

- Node.js 18+
- Firebase service account JSON downloaded from Firebase Console
- Root `.env` filled with Storj credentials and endpoint values

## 2) Firebase Admin credential file

Place your service account file at:

`backend/serviceAccountKey.json`

or set `GOOGLE_APPLICATION_CREDENTIALS` to another absolute path.

## 3) Install and run

```bash
cd backend
npm install
npm start
```

Server runs on `PORT` (default `8787`).

## 4) Endpoints

- `POST /proof-upload-url` (requires Firebase Bearer token)
  - Body: `{ "habitId": "..." , "contentType": "image/jpeg" }`
  - Returns: `{ "uploadUrl", "fileUrl", "objectKey" }`
- `GET /proof-file/<key>`
  - Redirects to short-lived signed Storj GET URL
- `GET /health`

## 5) Android base URL

For Android emulator local backend:

`SIGNED_UPLOAD_API_BASE_URL=http://10.0.2.2:8787`
