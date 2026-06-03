const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const srcDir = path.join(__dirname, '..', 'bmp-go', 'golang-frontend', 'src');
const destDir = path.join(__dirname, '..', 'frontend', 'src');

function copyAndClean(src, dest) {
  if (fs.statSync(src).isDirectory()) {
    if (!fs.existsSync(dest)) fs.mkdirSync(dest, { recursive: true });
    fs.readdirSync(src).forEach(file => {
      copyAndClean(path.join(src, file), path.join(dest, file));
    });
  } else {
    const filename = path.basename(src);
    const isTS = filename.endsWith('.ts') || filename.endsWith('.tsx');
    let destPath = dest;
    
    if (isTS) {
      const ext = path.extname(dest);
      if (ext === '.ts' || ext === '.tsx') {
        destPath = dest.substring(0, dest.length - ext.length) + (ext === '.tsx' ? '.jsx' : '.js');
      } else {
        // If dest is a directory, append the converted filename
        const destFilename = filename.replace(/\.tsx?$/, filename.endsWith('.tsx') ? '.jsx' : '.js');
        destPath = path.join(dest, destFilename);
      }
    }

    if (isTS) {
      try {
        execSync(`npx esbuild "${src}" --jsx=preserve --outfile="${destPath}"`, { stdio: 'ignore' });
        console.log(`Transpiled: ${filename} -> ${path.basename(destPath)}`);
      } catch (err) {
        console.error(`Failed to transpile ${filename}:`, err);
      }
    } else {
      const destDirName = path.dirname(destPath);
      if (!fs.existsSync(destDirName)) fs.mkdirSync(destDirName, { recursive: true });
      fs.copyFileSync(src, destPath);
      console.log(`Copied: ${filename}`);
    }
  }
}

// Ensure destDir/pages/bmp exists
const bmpPagesDest = path.join(destDir, 'pages', 'bmp');
if (!fs.existsSync(bmpPagesDest)) fs.mkdirSync(bmpPagesDest, { recursive: true });

// Copy pages to pages/bmp
fs.readdirSync(path.join(srcDir, 'pages')).forEach(file => {
  copyAndClean(path.join(srcDir, 'pages', file), bmpPagesDest);
});

// Copy services/api.ts as services/apiBmp.ts (which will be transpiled to apiBmp.js)
// const servicesDest = path.join(destDir, 'services');
// if (!fs.existsSync(servicesDest)) fs.mkdirSync(servicesDest, { recursive: true });
// copyAndClean(path.join(srcDir, 'services', 'api.ts'), path.join(servicesDest, 'apiBmp.ts'));

// Copy contexts/AuthContext.tsx as contexts/BmpAuthContext.tsx (which will be transpiled to BmpAuthContext.jsx)
const contextsDest = path.join(destDir, 'contexts');
if (!fs.existsSync(contextsDest)) fs.mkdirSync(contextsDest, { recursive: true });
copyAndClean(path.join(srcDir, 'contexts', 'AuthContext.tsx'), path.join(contextsDest, 'BmpAuthContext.tsx'));

console.log('Frontend merging script completed!');
