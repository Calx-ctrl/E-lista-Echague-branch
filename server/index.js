// server/index.js
const express = require('express');
const multer = require('multer');
const vision = require('@google-cloud/vision');

const upload = multer(); // memory storage
const app = express();
const port = process.env.PORT || 8080;

// Use Application Default Credentials (ADC).
// When deployed to Cloud Run, assign a service account; the client library will use ADC automatically.
const client = new vision.ImageAnnotatorClient();

app.get('/', (req, res) => res.send('Receipt OCR backend running'));

// POST /analyze - accepts multipart/form-data with field 'image' (binary)
app.post('/analyze', upload.single('image'), async (req, res) => {
  try {
    if (!req.file) return res.status(400).json({ error: 'no image file provided' });

    // Call documentTextDetection (best for receipts/documents)
    const [result] = await client.documentTextDetection({ image: { content: req.file.buffer } });

    const fullText = result.fullTextAnnotation ? result.fullTextAnnotation.text : '';
    // Return both fullText and the raw result to let the client parse as needed
    res.json({ ok: true, text: fullText, raw: result });
  } catch (err) {
    console.error('Vision error', err);
    res.status(500).json({ ok: false, error: err.message });
  }
});

app.listen(port, () => console.log(`Server running on port ${port}`));
