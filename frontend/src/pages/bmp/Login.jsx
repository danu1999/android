import { useState, useContext } from "react";
import { AuthContext } from '../../contexts/BmpAuthContext';
import { useNavigate } from "react-router-dom";
import api from '../../services/apiBmp';
const Login = () => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const { login } = useContext(AuthContext);
  const navigate = useNavigate();
  const isDemoEnv = window.location.hostname.includes("manufaktur") || window.location.hostname.includes("demo") || import.meta.env.VITE_DEMO_MODE === "true";
  const [showDemoLogin, setShowDemoLogin] = useState(isDemoEnv);
  const handleLogin = async (e) => {
    e.preventDefault();
    setError("");
    try {
      const response = await api.post("/login", {
        username,
        password
      });
      if (response.data.success) {
        login(response.data.token);
        navigate("/");
      }
    } catch (err) {
      setError(err.response?.data?.message || "Login gagal");
    }
  };
  const handleDemoLogin = async () => {
    setError("");
    try {
      const response = await api.post("/login", {
        username: "demouser",
        password: "demouser123"
      });
      if (response.data.success) {
        login(response.data.token);
        navigate("/");
      }
    } catch (err) {
      setError(err.response?.data?.message || "Login demo gagal");
    }
  };
  return <div style={{
    display: "flex",
    justifyContent: "center",
    alignItems: "center",
    height: "100vh",
    background: "linear-gradient(135deg, #0f172a 0%, #1e1b4b 100%)",
    fontFamily: "Arial, sans-serif"
  }}>
      {showDemoLogin ? <div style={{
    background: "rgba(255, 255, 255, 0.05)",
    backdropFilter: "blur(16px)",
    border: "1px solid rgba(255, 255, 255, 0.1)",
    padding: "40px",
    borderRadius: "16px",
    boxShadow: "0 20px 25px -5px rgba(0, 0, 0, 0.3)",
    width: "100%",
    maxWidth: "400px",
    color: "white",
    textAlign: "center"
  }}>
          <img
    src="/images/logo.jpg"
    alt="Logo"
    style={{
      width: "80px",
      height: "80px",
      borderRadius: "20px",
      objectFit: "cover",
      margin: "0 auto 15px",
      display: "block",
      boxShadow: "0 8px 16px rgba(0,0,0,0.3)",
      border: "1px solid rgba(255,255,255,0.1)"
    }}
  />
          <h2 style={{ margin: "0 0 10px 0", fontSize: "26px", fontWeight: "800", background: "linear-gradient(135deg, #3b82f6 0%, #06b6d4 100%)", WebkitBackgroundClip: "text", WebkitTextFillColor: "transparent" }}>
            Demo Invoice BMP
          </h2>
          <p style={{ margin: "0 0 25px 0", color: "#94a3b8", fontSize: "14px", fontWeight: "400" }}>
            Sistem Informasi Invoice & Manufaktur Plastik
          </p>
          
          {error && <div style={{ color: "#ef4444", marginBottom: "15px", fontSize: "14px", background: "rgba(239, 68, 68, 0.1)", padding: "10px", borderRadius: "8px" }}>{error}</div>}
          
          <button
    onClick={handleDemoLogin}
    style={{
      width: "100%",
      padding: "14px",
      background: "linear-gradient(135deg, #10b981 0%, #059669 100%)",
      color: "white",
      border: "none",
      borderRadius: "10px",
      cursor: "pointer",
      fontSize: "16px",
      fontWeight: "bold",
      boxShadow: "0 4px 15px rgba(16, 185, 129, 0.3)",
      transition: "transform 0.2s",
      marginBottom: "20px"
    }}
    onMouseOver={(e) => e.currentTarget.style.transform = "scale(1.02)"}
    onMouseOut={(e) => e.currentTarget.style.transform = "scale(1)"}
  >
            Masuk sebagai Demo User
          </button>
          
          <div style={{ marginTop: "15px" }}>
            <button
    onClick={() => setShowDemoLogin(false)}
    style={{
      background: "none",
      border: "none",
      color: "#3b82f6",
      cursor: "pointer",
      fontSize: "13px",
      textDecoration: "underline"
    }}
  >
              Masuk sebagai Administrator
            </button>
          </div>
        </div> : <form onSubmit={handleLogin} style={{
    background: "rgba(255, 255, 255, 0.05)",
    backdropFilter: "blur(16px)",
    border: "1px solid rgba(255, 255, 255, 0.1)",
    padding: "40px",
    borderRadius: "16px",
    boxShadow: "0 20px 25px -5px rgba(0, 0, 0, 0.3)",
    width: "100%",
    maxWidth: "400px",
    color: "white"
  }}>
          <img
    src="/images/logo.jpg"
    alt="Logo"
    style={{
      width: "80px",
      height: "80px",
      borderRadius: "20px",
      objectFit: "cover",
      margin: "0 auto 15px",
      display: "block",
      boxShadow: "0 8px 16px rgba(0,0,0,0.3)",
      border: "1px solid rgba(255,255,255,0.1)"
    }}
  />
          <h2 style={{ textAlign: "center", margin: "0 0 20px 0", fontSize: "26px", fontWeight: "800", background: "linear-gradient(135deg, #3b82f6 0%, #06b6d4 100%)", WebkitBackgroundClip: "text", WebkitTextFillColor: "transparent" }}>
            Login Admin
          </h2>
          {error && <div style={{ color: "#ef4444", marginBottom: "15px", fontSize: "14px", background: "rgba(239, 68, 68, 0.1)", padding: "10px", borderRadius: "8px", textAlign: "center" }}>{error}</div>}
          
          <div style={{ marginBottom: "15px" }}>
            <label style={{ display: "block", marginBottom: "8px", fontSize: "13px", color: "#94a3b8" }}>Username</label>
            <input
    type="text"
    value={username}
    onChange={(e) => setUsername(e.target.value)}
    style={{
      width: "100%",
      padding: "12px",
      boxSizing: "border-box",
      border: "1px solid rgba(255,255,255,0.1)",
      borderRadius: "10px",
      background: "rgba(0,0,0,0.2)",
      color: "white",
      outline: "none"
    }}
    required
  />
          </div>
          <div style={{ marginBottom: "20px" }}>
            <label style={{ display: "block", marginBottom: "8px", fontSize: "13px", color: "#94a3b8" }}>Password</label>
            <input
    type="password"
    value={password}
    onChange={(e) => setPassword(e.target.value)}
    style={{
      width: "100%",
      padding: "12px",
      boxSizing: "border-box",
      border: "1px solid rgba(255,255,255,0.1)",
      borderRadius: "10px",
      background: "rgba(0,0,0,0.2)",
      color: "white",
      outline: "none"
    }}
    required
  />
          </div>
          
          <button
    type="submit"
    style={{
      width: "100%",
      padding: "14px",
      background: "linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%)",
      color: "white",
      border: "none",
      borderRadius: "10px",
      cursor: "pointer",
      fontSize: "16px",
      fontWeight: "bold",
      boxShadow: "0 4px 15px rgba(59, 130, 246, 0.3)",
      transition: "transform 0.2s"
    }}
    onMouseOver={(e) => e.currentTarget.style.transform = "scale(1.02)"}
    onMouseOut={(e) => e.currentTarget.style.transform = "scale(1)"}
  >
            Sign In
          </button>
          
          <div style={{ marginTop: "20px", textAlign: "center" }}>
            <button
    type="button"
    onClick={() => setShowDemoLogin(true)}
    style={{
      background: "none",
      border: "none",
      color: "#10b981",
      cursor: "pointer",
      fontSize: "13px",
      textDecoration: "underline"
    }}
  >
              Kembali ke Login Demo
            </button>
          </div>
        </form>}
    </div>;
};
export default Login;
