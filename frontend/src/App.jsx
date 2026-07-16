import { useState } from "react";
import ShortenForm from "./components/ShortenForm";
import UrlStats from "./components/UrlStats";

export default function App() {
  const [refresh, setRefresh] = useState(0);

  return (
    <div style={{ maxWidth: "720px", margin: "60px auto", padding: "0 20px",
      fontFamily: "system-ui, sans-serif" }}>
      <h1 style={{ fontSize: "22px", fontWeight: 500, marginBottom: "4px" }}>
        URL Shortener
      </h1>
      <p style={{ fontSize: "13px", color: "#888", marginBottom: "28px" }}>
        Paste a long URL to get a short link
      </p>

      <ShortenForm onShortened={() => setRefresh((r) => r + 1)} />

      <hr style={{ border: "none", borderTop: "1px solid #eee", margin: "24px 0" }} />

      <h2 style={{ fontSize: "15px", fontWeight: 500, marginBottom: "16px" }}>
        All shortened URLs
      </h2>
      <UrlStats refreshTrigger={refresh} />
    </div>
  );
}