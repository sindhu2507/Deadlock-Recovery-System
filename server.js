// server.js
const express = require('express');
const multer = require('multer');
const path = require('path');
const { exec } = require('child_process');
const fs = require('fs');
const cors = require('cors');

const app = express();
const PORT = 3000;

// Set up file uploads
const upload = multer({ dest: 'uploads/' });

// Enable CORS for cross-origin requests (useful for debugging)
app.use(cors());

// Serve static files from 'public' folder
app.use(express.static(path.join(__dirname, 'public')));

// Upload endpoint
app.post('/check-deadlock', upload.single('file'), (req, res) => {
  console.log('Received POST request to /check-deadlock');

  if (!req.file) {
    console.error('No file uploaded');
    return res.status(400).json({ error: 'No file uploaded' });
  }

  const uploadedFilePath = path.resolve(req.file.path);
  console.log(`Uploaded file saved to: ${uploadedFilePath}`);

  const javaClass = 'DeadLock';
  const classpath = `.;${path.join(__dirname, 'lib', 'gson-2.10.1.jar')}`; // Dynamic path for Gson

  // Construct Java command
  const javaCommand = `java -cp "${classpath}" ${javaClass} "${uploadedFilePath}"`;
  console.log(`Executing Java command: ${javaCommand}`);

  // Run Java program
  exec(javaCommand, (error, stdout, stderr) => {
    // Clean up uploaded file
    try {
      fs.unlinkSync(uploadedFilePath);
      console.log(`Deleted uploaded file: ${uploadedFilePath}`);
    } catch (e) {
      console.error(`Failed to delete file: ${e.message}`);
    }

    if (error) {
      console.error(`Java execution error: ${error.message}`);
      return res.status(500).json({ error: 'Failed to execute Java program', details: error.message });
    }

    if (stderr) {
      console.error(`Java stderr: ${stderr}`);
      return res.status(500).json({ error: 'Java program error', details: stderr });
    }

    try {
      // Parse JSON output from Java
      const result = JSON.parse(stdout);
      console.log('Java program output:', result);
      res.json(result);
    } catch (e) {
      console.error(`JSON parse error: ${e.message}, stdout: ${stdout}`);
      res.status(500).json({ error: 'Invalid output from Java program', details: stdout });
    }
  });
});

// Handle root route for debugging
app.get('/', (req, res) => {
  console.log('Serving index.html');
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// Start server
app.listen(PORT, () => {
  console.log(`✅ Server running on http://localhost:${PORT}`);
});