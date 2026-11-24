import "jsr:@supabase/functions-js/edge-runtime.d.ts";

// Otumfour GIF search. The Tenor key stays in the TENOR_API_KEY secret and is never sent to the app.
const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Content-Type": "application/json",
};

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: cors });
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (req.method !== "POST") return json({ ok: false, error: "POST only" }, 405);
  const key = Deno.env.get("TENOR_API_KEY");
  if (!key) return json({ ok: false, missingKey: "TENOR_API_KEY" });
  let body: { query?: string; limit?: number };
  try {
    body = await req.json();
  } catch {
    return json({ ok: false, error: "Invalid JSON" }, 400);
  }
  const query = typeof body.query === "string" ? body.query.trim().slice(0, 80) : "";
  const limit = Math.min(Math.max(body.limit ?? 20, 1), 30);
  const url = new URL(query ? "https://tenor.googleapis.com/v2/search" : "https://tenor.googleapis.com/v2/featured");
  url.searchParams.set("key", key);
  url.searchParams.set("limit", String(limit));
  url.searchParams.set("media_filter", "gif,tinygif");
  url.searchParams.set("contentfilter", "medium");
  if (query) url.searchParams.set("q", query);
  const response = await fetch(url);
  if (!response.ok) return json({ ok: false, error: "GIF service error" }, 502);
  const data = await response.json();
  const gifs = (data.results ?? []).map((item: {
    title?: string;
    media_formats?: { gif?: { url?: string }; tinygif?: { url?: string } };
  }) => ({
    title: item.title ?? "",
    gif: item.media_formats?.gif?.url ?? "",
    preview: item.media_formats?.tinygif?.url ?? item.media_formats?.gif?.url ?? "",
  })).filter((item: { gif: string }) => item.gif.startsWith("https://"));
  return json({ ok: true, gifs });
});
