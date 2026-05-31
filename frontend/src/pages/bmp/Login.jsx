const _jsxFileName = "C:\\Users\\danus\\Documents\\antigravity\\invoice-bmp-go\\golang-frontend\\src\\pages\\Login.tsx"; function _optionalChain(ops) { let lastAccessLHS = undefined; let value = ops[0]; let i = 1; while (i < ops.length) { const op = ops[i]; const fn = ops[i + 1]; i += 2; if ((op === 'optionalAccess' || op === 'optionalCall') && value == null) { return undefined; } if (op === 'access' || op === 'optionalAccess') { lastAccessLHS = value; value = fn(value); } else if (op === 'call' || op === 'optionalCall') { value = fn((...args) => value.call(lastAccessLHS, ...args)); lastAccessLHS = undefined; } } return value; }import React, { useState, useContext } from 'react';
import { AuthContext } from '../../contexts/BmpAuthContext';
import { useNavigate } from 'react-router-dom';
import api from '../../services/apiBmp';

const Login = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const { login } = useContext(AuthContext);
  const navigate = useNavigate();

  // Deteksi apakah ini situs demo berdasarkan URL atau environment
  const isDemoEnv = 
    window.location.hostname.includes('manufaktur') || 
    window.location.hostname.includes('demo') || 
    import.meta.env.VITE_DEMO_MODE === 'true';

  const [showDemoLogin, setShowDemoLogin] = useState(isDemoEnv);

  const handleLogin = async (e) => {
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
  };

  return (
    React.createElement('div', { style: { 
      display: 'flex', 
      justifyContent: 'center', 
      alignItems: 'center', 
      height: '100vh', 
      background: 'linear-gradient(135deg, #0f172a 0%, #1e1b4b 100%)',
      fontFamily: 'Arial, sans-serif'
    }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 55}}
      , showDemoLogin ? (
        React.createElement('div', { style: { 
          background: 'rgba(255, 255, 255, 0.05)', 
          backdropFilter: 'blur(16px)',
          border: '1px solid rgba(255, 255, 255, 0.1)',
          padding: '40px', 
          borderRadius: '16px', 
          boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.3)', 
          width: '100%', 
          maxWidth: '400px',
          color: 'white',
          textAlign: 'center'
        }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 64}}
          , React.createElement('img', { 
            src: "/images/logo.jpg", 
            alt: "Logo", 
            style: { 
              width: '80px', 
              height: '80px', 
              borderRadius: '20px', 
              objectFit: 'cover', 
              margin: '0 auto 15px', 
              display: 'block',
              boxShadow: '0 8px 16px rgba(0,0,0,0.3)',
              border: '1px solid rgba(255,255,255,0.1)'
            }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 76}} 
          )
          , React.createElement('h2', { style: { margin: '0 0 10px 0', fontSize: '26px', fontWeight: '800', background: 'linear-gradient(135deg, #3b82f6 0%, #06b6d4 100%)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 90}}, "Demo Invoice BMP"

          )
          , React.createElement('p', { style: { margin: '0 0 25px 0', color: '#94a3b8', fontSize: '14px', fontWeight: '400' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 93}}, "Sistem Informasi Invoice & Manufaktur Plastik"

          )

          , error && React.createElement('div', { style: { color: '#ef4444', marginBottom: '15px', fontSize: '14px', background: 'rgba(239, 68, 68, 0.1)', padding: '10px', borderRadius: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 97}}, error)

          , React.createElement('button', { 
            onClick: handleDemoLogin, 
            style: { 
              width: '100%', 
              padding: '14px', 
              background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)', 
              color: 'white', 
              border: 'none', 
              borderRadius: '10px', 
              cursor: 'pointer', 
              fontSize: '16px',
              fontWeight: 'bold',
              boxShadow: '0 4px 15px rgba(16, 185, 129, 0.3)',
              transition: 'transform 0.2s',
              marginBottom: '20px'
            },
            onMouseOver: (e) => e.currentTarget.style.transform = 'scale(1.02)',
            onMouseOut: (e) => e.currentTarget.style.transform = 'scale(1)', __self: this, __source: {fileName: _jsxFileName, lineNumber: 99}}
, "Masuk sebagai Demo User"

          )

          , React.createElement('div', { style: { marginTop: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 121}}
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
          , React.createElement('div', { style: { marginTop: '15px' } }
            , React.createElement('button', { 
              type: "button",
              onClick: () => {
                localStorage.setItem('posbah_app_mode', 'FNB');
                window.location.href = '/';
              }, 
              style: { 
                background: 'none', 
                border: 'none', 
                color: '#f43f5e', 
                cursor: 'pointer', 
                fontSize: '13px',
                textDecoration: 'underline',
                fontWeight: 'bold'
              } }
              , "← Kembali ke Portal POSBah"
            )
          )
        )
      ) : (
        React.createElement('form', { onSubmit: handleLogin, style: { 
          background: 'rgba(255, 255, 255, 0.05)', 
          backdropFilter: 'blur(16px)',
          border: '1px solid rgba(255, 255, 255, 0.1)',
          padding: '40px', 
          borderRadius: '16px', 
          boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.3)', 
          width: '100%', 
          maxWidth: '400px',
          color: 'white'
        }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 138}}
          , React.createElement('img', { 
            src: "/images/logo.jpg", 
            alt: "Logo", 
            style: { 
              width: '80px', 
              height: '80px', 
              borderRadius: '20px', 
              objectFit: 'cover', 
              margin: '0 auto 15px', 
              display: 'block',
              boxShadow: '0 8px 16px rgba(0,0,0,0.3)',
              border: '1px solid rgba(255,255,255,0.1)'
            }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 149}} 
          )
          , React.createElement('h2', { style: { textAlign: 'center', margin: '0 0 20px 0', fontSize: '26px', fontWeight: '800', background: 'linear-gradient(135deg, #3b82f6 0%, #06b6d4 100%)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 163}}, "Login Admin"

          )
          , error && React.createElement('div', { style: { color: '#ef4444', marginBottom: '15px', fontSize: '14px', background: 'rgba(239, 68, 68, 0.1)', padding: '10px', borderRadius: '8px', textAlign: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 166}}, error)

          , React.createElement('div', { style: { marginBottom: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 168}}
            , React.createElement('label', { style: { display: 'block', marginBottom: '8px', fontSize: '13px', color: '#94a3b8' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 169}}, "Username")
            , React.createElement('input', {
              type: "text",
              value: username,
              onChange: (e) => setUsername(e.target.value),
              style: { 
                width: '100%', 
                padding: '12px', 
                boxSizing: 'border-box', 
                border: '1px solid rgba(255,255,255,0.1)', 
                borderRadius: '10px',
                background: 'rgba(0,0,0,0.2)',
                color: 'white',
                outline: 'none'
              },
              required: true, __self: this, __source: {fileName: _jsxFileName, lineNumber: 170}}
            )
          )
          , React.createElement('div', { style: { marginBottom: '20px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 187}}
            , React.createElement('label', { style: { display: 'block', marginBottom: '8px', fontSize: '13px', color: '#94a3b8' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 188}}, "Password")
            , React.createElement('input', {
              type: "password",
              value: password,
              onChange: (e) => setPassword(e.target.value),
              style: { 
                width: '100%', 
                padding: '12px', 
                boxSizing: 'border-box', 
                border: '1px solid rgba(255,255,255,0.1)', 
                borderRadius: '10px',
                background: 'rgba(0,0,0,0.2)',
                color: 'white',
                outline: 'none'
              },
              required: true, __self: this, __source: {fileName: _jsxFileName, lineNumber: 189}}
            )
          )

          , React.createElement('button', { 
            type: "submit", 
            style: { 
              width: '100%', 
              padding: '14px', 
              background: 'linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%)', 
              color: 'white', 
              border: 'none', 
              borderRadius: '10px', 
              cursor: 'pointer', 
              fontSize: '16px',
              fontWeight: 'bold',
              boxShadow: '0 4px 15px rgba(59, 130, 246, 0.3)',
              transition: 'transform 0.2s'
            },
            onMouseOver: (e) => e.currentTarget.style.transform = 'scale(1.02)',
            onMouseOut: (e) => e.currentTarget.style.transform = 'scale(1)', __self: this, __source: {fileName: _jsxFileName, lineNumber: 207}}
, "Sign In"

          )

          , React.createElement('div', { style: { marginTop: '20px', textAlign: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 228}}
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
          , React.createElement('div', { style: { marginTop: '15px', textAlign: 'center' } }
            , React.createElement('button', { 
              type: "button",
              onClick: () => {
                localStorage.setItem('posbah_app_mode', 'FNB');
                window.location.href = '/';
              }, 
              style: { 
                background: 'none', 
                border: 'none', 
                color: '#f43f5e', 
                cursor: 'pointer', 
                fontSize: '13px',
                textDecoration: 'underline',
                fontWeight: 'bold'
              } }
              , "← Kembali ke Portal POSBah"
            )
          )
        )
      )
    )
  );
};

export default Login;
