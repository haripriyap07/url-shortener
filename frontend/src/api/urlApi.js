```javascript
const BASE = "https://url-shortener-backend-ifux.onrender.com/api";

export async function shortenUrl(url, ttlHours = null) {
  const res = await fetch(`${BASE}/shorten`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ url, ttlHours }),
  });

  if (!res.ok) {
    let message = "Failed to shorten URL";

    try {
      const err = await res.json();
      message = err.detail || err.message || message;
    } catch {
      // Response was not JSON
    }

    throw new Error(message);
  }

  return res.json();
}

export async function fetchAllUrls() {
  const res = await fetch(`${BASE}/urls`);

  if (!res.ok) {
    throw new Error("Failed to fetch URLs");
  }

  return res.json();
}

export async function deleteUrl(shortCode) {
  const res = await fetch(`${BASE}/urls/${shortCode}`, {
    method: "DELETE",
  });

  if (!res.ok) {
    throw new Error("Failed to delete URL");
  }
}
```
