import cors from "cors";
import dotenv from "dotenv";
import express from "express";
import admin from "firebase-admin";
import { S3Client, PutObjectCommand, GetObjectCommand } from "@aws-sdk/client-s3";
import { getSignedUrl } from "@aws-sdk/s3-request-presigner";

dotenv.config({ path: "../.env" });
dotenv.config();

const requiredEnvKeys = [
  "STORJ_ENDPOINT",
  "STORJ_BUCKET",
  "STORJ_ACCESS_KEY",
  "STORJ_SECRET_KEY",
];

for (const key of requiredEnvKeys) {
  if (!process.env[key]) {
    throw new Error(`Missing environment variable: ${key}`);
  }
}

const app = express();
const port = Number(process.env.PORT || 8787);
const publicBaseUrl = (process.env.SIGNED_UPLOAD_API_BASE_URL || `http://localhost:${port}`).replace(/\/$/, "");

app.use(cors());
app.use(express.json({ limit: "2mb" }));

if (!admin.apps.length) {
  const serviceAccountJson = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (serviceAccountJson) {
    admin.initializeApp({
      credential: admin.credential.cert(JSON.parse(serviceAccountJson)),
    });
  } else {
    admin.initializeApp();
  }
}

const s3Client = new S3Client({
  endpoint: process.env.STORJ_ENDPOINT,
  region: process.env.STORJ_REGION || "global",
  forcePathStyle: true,
  credentials: {
    accessKeyId: process.env.STORJ_ACCESS_KEY,
    secretAccessKey: process.env.STORJ_SECRET_KEY,
  },
});

async function verifyFirebaseAuth(req, res, next) {
  try {
    const authHeader = req.headers.authorization || "";
    const token = authHeader.startsWith("Bearer ") ? authHeader.slice("Bearer ".length) : "";
    if (!token) {
      return res.status(401).json({ error: "Missing Authorization token." });
    }

    const decodedToken = await admin.auth().verifyIdToken(token);
    req.user = decodedToken;
    return next();
  } catch (error) {
    return res.status(401).json({ error: "Unauthorized request." });
  }
}

function encodePath(pathValue) {
  return pathValue.split("/").map(encodeURIComponent).join("/");
}

app.post("/proof-upload-url", verifyFirebaseAuth, async (req, res) => {
  try {
    const userId = req.user?.uid;
    const habitId = String(req.body?.habitId || "").trim();
    const contentType = String(req.body?.contentType || "image/jpeg").trim();
    if (!userId || !habitId) {
      return res.status(400).json({ error: "userId/habitId missing." });
    }

    const timestamp = Date.now();
    const key = `proofs/${userId}/${habitId}/${timestamp}.jpg`;
    const putCommand = new PutObjectCommand({
      Bucket: process.env.STORJ_BUCKET,
      Key: key,
      ContentType: contentType,
    });
    const uploadUrl = await getSignedUrl(s3Client, putCommand, { expiresIn: 300 });

    const encodedKey = encodePath(key);
    const fileUrl = `${publicBaseUrl}/proof-file/${encodedKey}`;
    return res.json({ uploadUrl, fileUrl, objectKey: key });
  } catch (error) {
    return res.status(500).json({ error: "Failed to generate upload URL." });
  }
});

app.get("/proof-file/*", async (req, res) => {
  try {
    const key = String(req.params[0] || "");
    if (!key) {
      return res.status(400).send("Missing key.");
    }

    const getCommand = new GetObjectCommand({
      Bucket: process.env.STORJ_BUCKET,
      Key: key,
    });
    const signedGetUrl = await getSignedUrl(s3Client, getCommand, { expiresIn: 120 });
    return res.redirect(signedGetUrl);
  } catch (error) {
    return res.status(404).send("Proof file not found.");
  }
});

app.get("/health", (_req, res) => {
  res.json({ ok: true });
});

if (!process.env.VERCEL) {
  app.listen(port, () => {
    // eslint-disable-next-line no-console
    console.log(`HabitSync proof backend running on port ${port}`);
  });
}

export default app;
