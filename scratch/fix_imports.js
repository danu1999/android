const fs = require('fs');
const path = require('path');

const bmpDir = 'c:\\Users\\danus\\Documents\\antigravity\\POSBah\\frontend\\src\\pages\\bmp';

// Files that need API_URL imported
const filesWithApiUrl = ['Invoices.jsx', 'Payroll.jsx', 'Pricelist.jsx', 'BahanNono.jsx'];

fs.readdirSync(bmpDir).forEach(filename => {
  const filepath = path.join(bmpDir, filename);
  if (!fs.statSync(filepath).isFile()) return;

  let content = fs.readFileSync(filepath, 'utf8');

  // Fix api import
  if (filesWithApiUrl.includes(filename)) {
    content = content.replace(
      "import api from '../services/api';",
      "import api, { API_URL } from '../../services/apiBmp';"
    );
  } else {
    content = content.replace(
      "import api from '../services/api';",
      "import api from '../../services/apiBmp';"
    );
  }

  // Fix auth context import for Login.jsx
  if (filename === 'Login.jsx') {
    content = content.replace(
      "import { AuthContext } from '../contexts/AuthContext';",
      "import { AuthContext } from '../../contexts/BmpAuthContext';"
    );
  }

  // Replace railway URL with API_URL
  content = content.replaceAll(
    "import.meta.env.VITE_API_URL || 'https://bmp.up.railway.app/api'",
    "API_URL"
  );

  fs.writeFileSync(filepath, content, 'utf8');
  console.log(`Processed basic imports for: ${filename}`);
});

// Process InvoiceImageTemplate.jsx
const templatePath = 'c:\\Users\\danus\\Documents\\antigravity\\POSBah\\frontend\\src\\components\\InvoiceImageTemplate.jsx';
let templateContent = fs.readFileSync(templatePath, 'utf8');
templateContent = "import { API_URL } from '../services/apiBmp';\n" + templateContent;
templateContent = templateContent.replaceAll(
  "import.meta.env.VITE_API_URL || 'https://bmp.up.railway.app/api'",
  "API_URL"
);
fs.writeFileSync(templatePath, templateContent, 'utf8');
console.log('Processed InvoiceImageTemplate.jsx');

// Apply custom logic to Login.jsx
const loginPath = path.join(bmpDir, 'Login.jsx');
let loginContent = fs.readFileSync(loginPath, 'utf8');

// Replace handleLogin and handleDemoLogin with proper error handling
const oldHandleLogin = `  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const response = await api.post('/login', {
        username,
        password,
      });
      if (response.data.success) {
        login(response.data.token);
        navigate('/');
      }
    } catch (err) {
      setError(_optionalChain([err, 'access', _ => _.response, 'optionalAccess', _2 => _2.data, 'optionalAccess', _3 => _3.message]) || 'Login gagal');
    }
  };

  const handleDemoLogin = async () => {
    setError('');
    try {
      const response = await api.post('/login', {
        username: 'demouser',
        password: 'demouser123',
      });
      if (response.data.success) {
        login(response.data.token);
        navigate('/');
      }
    } catch (err) {
      setError(_optionalChain([err, 'access', _4 => _4.response, 'optionalAccess', _5 => _5.data, 'optionalAccess', _6 => _6.message]) || 'Login demo gagal');
    }
  };`;

const newHandleLogin = `  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const response = await api.post('/login', {
        username,
        password,
      });
      if (response.data.success) {
        login(response.data.token);
        navigate('/');
      }
    } catch (err) {
      if (!err.response) {
        setError('Gagal terhubung ke server backend (Network Error). Pastikan server aktif.');
      } else {
        setError(_optionalChain([err, 'access', _ => _.response, 'optionalAccess', _2 => _2.data, 'optionalAccess', _3 => _3.message]) || 'Login gagal');
      }
    }
  };

  const handleDemoLogin = async () => {
    setError('');
    try {
      const response = await api.post('/login', {
        username: 'demouser',
        password: 'demouser123',
      });
      if (response.data.success) {
        login(response.data.token);
        navigate('/');
      }
    } catch (err) {
      if (!err.response) {
        setError('Gagal terhubung ke server backend (Network Error). Pastikan server aktif.');
      } else {
        setError(_optionalChain([err, 'access', _4 => _4.response, 'optionalAccess', _5 => _5.data, 'optionalAccess', _6 => _6.message]) || 'Login demo gagal');
      }
    }
  };

  const handleBackToPosbah = () => {
    localStorage.setItem('posbah_app_mode', 'FNB');
    window.location.href = '/';
  };`;

loginContent = loginContent.replace(oldHandleLogin, newHandleLogin);

// Add the Kembali ke POSBah buttons inside JSX
const oldDemoAdminBtn = `, React.createElement('div', { style: { marginTop: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 121}}
            , React.createElement('button', { 
              onClick: () => setShowDemoLogin(false), 
              style: { 
                background: 'none', 
                border: 'none', 
                color: '#3b82f6', 
                cursor: 'pointer', 
                fontSize: '13px',
                textDecoration: 'underline' 
              }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 122}}
, "Masuk sebagai Administrator"

            )
          )`;

const newDemoAdminBtn = `, React.createElement('div', { style: { marginTop: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 121}}
            , React.createElement('button', { 
              onClick: () => setShowDemoLogin(false), 
              style: { 
                background: 'none', 
                border: 'none', 
                color: '#3b82f6', 
                cursor: 'pointer', 
                fontSize: '13px',
                textDecoration: 'underline' 
              }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 122}}
, "Masuk sebagai Administrator"

            )
          )

          , React.createElement('div', { style: { marginTop: '20px', borderTop: '1px solid rgba(255, 255, 255, 0.1)', paddingTop: '20px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 142}}
            , React.createElement('button', { 
              type: "button",
              onClick: handleBackToPosbah, 
              style: { 
                background: 'none', 
                border: 'none', 
                color: '#60a5fa', 
                cursor: 'pointer', 
                fontSize: '13px',
                textDecoration: 'underline' 
              }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 143}}
, "← Kembali ke POSBah (Retail/Laundry/Rental)"

            )
          )`;

loginContent = loginContent.replace(oldDemoAdminBtn, newDemoAdminBtn);

const oldLoginDemoBtn = `, React.createElement('div', { style: { marginTop: '20px', textAlign: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 228}}
            , React.createElement('button', { 
              type: "button",
              onClick: () => setShowDemoLogin(true), 
              style: { 
                background: 'none', 
                border: 'none', 
                color: '#10b981', 
                cursor: 'pointer', 
                fontSize: '13px',
                textDecoration: 'underline' 
              }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 229}}
, "Kembali ke Login Demo"

            )
          )`;

const newLoginDemoBtn = `, React.createElement('div', { style: { marginTop: '20px', textAlign: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 228}}
            , React.createElement('button', { 
              type: "button",
              onClick: () => setShowDemoLogin(true), 
              style: { 
                background: 'none', 
                border: 'none', 
                color: '#10b981', 
                cursor: 'pointer', 
                fontSize: '13px',
                textDecoration: 'underline' 
              }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 229}}
, "Kembali ke Login Demo"

            )
          )

          , React.createElement('div', { style: { marginTop: '20px', borderTop: '1px solid rgba(255, 255, 255, 0.1)', paddingTop: '20px', textAlign: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 266}}
            , React.createElement('button', { 
              type: "button",
              onClick: handleBackToPosbah, 
              style: { 
                background: 'none', 
                border: 'none', 
                color: '#60a5fa', 
                cursor: 'pointer', 
                fontSize: '13px',
                textDecoration: 'underline' 
              }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 267}}
, "← Kembali ke POSBah (Retail/Laundry/Rental)"

            )
          )`;

loginContent = loginContent.replace(oldLoginDemoBtn, newLoginDemoBtn);

fs.writeFileSync(loginPath, loginContent, 'utf8');
console.log('Login.jsx custom logic applied successfully!');
