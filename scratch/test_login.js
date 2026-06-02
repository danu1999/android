const { exec } = require('child_process');

const pemPath = "C:\\Users\\danus\\Documents\\muizz.pem";
const ip = "103.93.163.227";
const user = "muizz9900";
const payload = JSON.stringify({ username: "bahteramulyap@gmail.com", password: "Bahtera1!" });

// We will construct the command to run curl on the remote machine
// Using single quotes for JSON so it doesn't get messed up.
const curlCmd = `curl -s -X POST http://localhost:8080/api/login -H "Content-Type: application/json" -d '${payload}'`;
const sshCmd = `ssh -o StrictHostKeyChecking=no -i "${pemPath}" ${user}@${ip} "${curlCmd.replace(/"/g, '\\"')}"`;

console.log("Running SSH Command:", sshCmd);

exec(sshCmd, (err, stdout, stderr) => {
  if (err) {
    console.error("Error executing command:", err);
    return;
  }
  console.log("STDOUT:", stdout);
  console.error("STDERR:", stderr);
});
