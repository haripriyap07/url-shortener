import { useState } from "react";
import { shortenUrl } from "../api/urlApi";

export default function ShortenForm({ onShortened }) {
  const [url, setUrl] = useState("");
  const [ttlHours, setTtlHours] = useState("");
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [copied, setCopied] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setResult(null);
    setLoading(true);
    try {
      const data = await shortenUrl(url, ttlHours ? Number(ttlHours) : null);
      setResult(data);
      onShortened(); // refresh stats list
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleCopy() {
    await navigator.clipboard.writeText(result.shortUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  return (
    <div style={{ marginBottom: "2rem" }}>
      <form onSubmit={handleSubmit} style={{ display: "flex", gap: "8px" }}>
        <input
          type="text"
          placeholder="https://example.com/very/long/url"
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          style={{ flex: 1, padding: "8px 12px", borderRadius: "8px",
            border: "1px solid #ddd", fontSize: "14px" }}
          required
        />
        <input
          type="number"
          placeholder="TTL (hrs)"
          value={ttlHours}
          onChange={(e) => setTtlHours(e.target.value)}
          style={{ width: "100px", padding: "8px 12px", borderRadius: "8px",
            border: "1px solid #ddd", fontSize: "14px" }}
          min="1"
        />
        <button type="submit" disabled={loading}
          style={{ padding: "8px 20px", borderRadius: "8px", background: "#378ADD",
            color: "#fff", border: "none", cursor: "pointer", fontSize: "14px" }}>
          {loading ? "..." : "Shorten"}
        </button>
      </form>

      {error && (
        <p style={{ color: "#E24B4A", marginTop: "8px", fontSize: "13px" }}>{error}</p>
      )}

      {result && (
        <div style={{ marginTop: "12px", padding: "12px 16px", background: "#EAF3DE",
          borderRadius: "8px", display: "flex", alignItems: "center",
          justifyContent: "space-between" }}>
          <a href={result.shortUrl} target="_blank" rel="noreferrer"
            style={{ color: "#3B6D11", fontWeight: 500, fontSize: "14px" }}>
            {result.shortUrl}
          </a>
          <button onClick={handleCopy}
            style={{ padding: "4px 12px", borderRadius: "6px", fontSize: "12px",
              border: "1px solid #639922", background: "transparent",
              color: "#3B6D11", cursor: "pointer" }}>
            {copied ? "Copied!" : "Copy"}
          </button>
        </div>
      )}
    </div>
  );
}