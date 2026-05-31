const fs = require('fs');
const path = require('path');

const srcDir = 'C:\\Users\\danus\\Documents\\antigravity\\invoice-bmp-go\\golang-frontend\\src';
const destDir = 'c:\\Users\\danus\\Documents\\antigravity\\POSBah\\frontend\\src';

function cleanTypeScript(code) {
  // Remove type definitions
  code = code.replace(/interface\s+\w+\s*\{[\s\S]*?\}/g, '');
  code = code.replace(/type\s+\w+\s*=\s*[\s\S]*?;/g, '');
  code = code.replace(/import\s+type\s+[\s\S]*?;/g, '');

  // Replace type annotations in variables and functions
  code = code.replace(/:\s*React\.FC(\s*<[^>]*>)?/g, '');
  code = code.replace(/:\s*React\.FormEvent/g, '');
  code = code.replace(/:\s*React\.MouseEvent/g, '');
  code = code.replace(/:\s*React\.ChangeEvent<[^>]*>/g, '');
  code = code.replace(/:\s*React\.ReactNode/g, '');
  
  code = code.replace(/useState\s*<[^>]*>\s*\(/g, 'useState(');
  code = code.replace(/useRef\s*<[^>]*>\s*\(/g, 'useRef(');
  code = code.replace(/useContext\s*<[^>]*>\s*\(/g, 'useContext(');
  code = code.replace(/createContext\s*<[^>]*>\s*\(/g, 'createContext(');

  code = code.replace(/:\s*any/g, '');
  code = code.replace(/:\s*string/g, '');
  code = code.replace(/:\s*number/g, '');
  code = code.replace(/:\s*boolean/g, '');
  code = code.replace(/:\s*string\[\]/g, '');
  code = code.replace(/:\s*number\[\]/g, '');
  code = code.replace(/:\s*any\[\]/g, '');
  code = code.replace(/as\s+string/g, '');
  code = code.replace(/as\s+any/g, '');

  code = code.replace(/import\s+\{\s*AuthContextType\s*\}\s*from\s*'.*';/g, '');

  return code;
}

function copyAndClean(src, dest) {
  if (fs.statSync(src).isDirectory()) {
    if (!fs.existsSync(dest)) fs.mkdirSync(dest, { recursive: true });
    fs.readdirSync(src).forEach(file => {
      copyAndClean(path.join(src, file), path.join(dest, file));
    });
  } else {
    let filename = path.basename(src);
    let isTS = filename.endsWith('.ts') || filename.endsWith('.tsx');
    let destFilename = filename;
    if (isTS) {
      destFilename = filename.replace(/\.tsx?$/, filename.endsWith('.tsx') ? '.jsx' : '.js');
    }
    
    const destPath = path.join(path.dirname(dest), destFilename);
    
    if (isTS) {
      let content = fs.readFileSync(src, 'utf8');
      let cleaned = cleanTypeScript(content);
      fs.writeFileSync(destPath, cleaned, 'utf8');
      console.log(`Cleaned and copied: ${filename} -> ${destFilename}`);
    } else {
      fs.copyFileSync(src, destPath);
      console.log(`Copied: ${filename}`);
    }
  }
}

// Ensure directories exist
const bmpPagesDest = path.join(destDir, 'pages', 'bmp');
if (!fs.existsSync(bmpPagesDest)) fs.mkdirSync(bmpPagesDest, { recursive: true });

// Copy pages to pages/bmp
fs.readdirSync(path.join(srcDir, 'pages')).forEach(file => {
  copyAndClean(path.join(srcDir, 'pages', file), path.join(bmpPagesDest, file));
});

// Copy services/api.ts as services/apiBmp.js
const servicesDest = path.join(destDir, 'services');
if (!fs.existsSync(servicesDest)) fs.mkdirSync(servicesDest, { recursive: true });
copyAndClean(path.join(srcDir, 'services', 'api.ts'), path.join(servicesDest, 'apiBmp.ts'));

// Copy contexts/AuthContext.tsx as contexts/BmpAuthContext.jsx
const contextsDest = path.join(destDir, 'contexts');
if (!fs.existsSync(contextsDest)) fs.mkdirSync(contextsDest, { recursive: true });
copyAndClean(path.join(srcDir, 'contexts', 'AuthContext.tsx'), path.join(contextsDest, 'BmpAuthContext.tsx'));

console.log('Frontend merging script completed!');
