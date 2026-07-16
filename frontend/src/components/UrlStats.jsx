import { useEffect, useState } from "react";
import { fetchAllUrls, deleteUrl } from "../api/urlApi";

export default function UrlStats({ refreshTrigger }) {
  const [urls, setUrls] = useState([]);

  useEffect(() => {
    fetchAllUrls().then(setUrls).catch(console.error);
  }, [refreshTrigger]);

  async function handleDelete(shortCode) {
    await deleteUrl(shortCode);
    setUrls((prev) => prev.filter((u) => u.shortCode !== shortCode));
  }

  if (urls.length === 0) return (
    <p style={{ color: "#888", fontSize: "13px" }}>No URLs shortened yet.</p>
  );

  return (
    <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "13px" }}>
      <thead>
        <tr style={{ borderBottom: "1px solid #eee", textAlign: "left" }}>
          <th style={{ padding: "8px 0", fontWeight: 500 }}>Short code</th>
          <th style={{ padding: "8px 0", fontWeight: 500 }}>Original URL</th>
          <th style={{ padding: "8px 0", fontWeight: 500 }}>Clicks</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        {urls.map((u) => (
          <tr key={u.shortCode} style={{ borderBottom: "1px solid #f5f5f5" }}>
            <td style={{ padding: "8px 0", color: "#378ADD", fontWeight: 500 }}>
              {u.shortCode}
            </td>
            <td style={{ padding: "8px 0", maxWidth: "300px",
              overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
              {u.originalUrl}
            </td>
            <td style={{ padding: "8px 0" }}>{u.clickCount}</td>
            <td style={{ padding: "8px 0", textAlign: "right" }}>
              <button onClick={() => handleDelete(u.shortCode)}
                style={{ fontSize: "11px", padding: "3px 8px", borderRadius: "6px",
                  border: "1px solid #F09595", color: "#A32D2D",
                  background: "transparent", cursor: "pointer" }}>
                Delete
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}