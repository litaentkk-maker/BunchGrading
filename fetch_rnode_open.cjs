const https = require('https');

https.get('https://raw.githubusercontent.com/markqvist/Reticulum/master/RNS/Interfaces/RNodeInterface.py', (resp) => {
  let data = '';
  resp.on('data', (chunk) => { data += chunk; });
  resp.on('end', () => {
    const lines = data.split('\n');
    const initStart = lines.findIndex(l => l.includes('def open_port('));
    if (initStart !== -1) {
      console.log(lines.slice(initStart, initStart + 50).join('\n'));
    } else {
      console.log("Not found");
    }
  });
}).on("error", (err) => {
  console.log("Error: " + err.message);
});
