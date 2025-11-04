# Receipt OCR Backend (Node.js)
This server accepts multipart/form-data `image` uploads and calls Google Cloud Vision `documentTextDetection`.
It expects to run with Application Default Credentials (ADC) when deployed on Cloud Run.

Local dev:
  1. Install dependencies: `npm install`
  2. Authenticate ADC for local dev: `gcloud auth application-default login` OR set GOOGLE_APPLICATION_CREDENTIALS
  3. Start: `npm start`

Deploy to Cloud Run:
  - `gcloud run deploy receipt-ocr --source=./server --region=us-central1 --platform=managed --allow-unauthenticated --service-account=OCR_RUN_SA@PROJECT_ID.iam.gserviceaccount.com`

NOTE: Do NOT store service account JSON in client apps. Use server-side ADC.
