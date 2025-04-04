const https = require('https');
const fs = require('fs');
const path = require('path');

const urls = [
  'https://raw.githubusercontent.com/Leaflet/Leaflet/main/dist/images/marker-icon-2x.png',
  'https://raw.githubusercontent.com/Leaflet/Leaflet/main/dist/images/marker-icon.png',
  'https://raw.githubusercontent.com/Leaflet/Leaflet/main/dist/images/marker-shadow.png'
];

const destination = path.join(__dirname, 'public', 'leaflet');

urls.forEach(url => {
  const fileName = path.basename(url);
  const filePath = path.join(destination, fileName);
  
  https.get(url, (response) => {
    const fileStream = fs.createWriteStream(filePath);
    response.pipe(fileStream);
    
    fileStream.on('finish', () => {
      fileStream.close();
      console.log(`Downloaded ${fileName}`);
    });
  }).on('error', (err) => {
    console.error(`Error downloading ${fileName}:`, err.message);
  });
});
