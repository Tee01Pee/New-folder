import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

// Minimal Prestige Law backend. No libraries, no build tool.
// Run from inside the backend/ folder with:  java Server.java
public class Server {
    // The site lives in the folder ABOVE backend/ (your repo root). Nothing is moved.
    static final Path SITE = Paths.get(System.getenv().getOrDefault("SITE_DIR", "..")).toAbsolutePath().normalize();
    static final Path BACKEND = Paths.get("").toAbsolutePath().normalize();
    static final Path CSV = Paths.get("inquiries.csv");

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "3000"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", Server::serveStatic);
        server.createContext("/api/inquiry", Server::handleInquiry);
        server.start();
        System.out.println("Running at http://localhost:" + port);
    }

    // ---------- Serves your site unchanged (injects one script tag into index.html) ----------
    static void serveStatic(HttpExchange ex) throws IOException {
        try {
            if (!ex.getRequestMethod().equals("GET")) { send(ex, 405, "text/plain", "Method not allowed"); return; }
            String p = ex.getRequestURI().getPath();
            if (p.equals("/")) p = "/index.html";
            if (p.equals("/form.js")) { // our script lives in backend/, not in your site files
                send(ex, 200, "application/javascript", Files.readAllBytes(Paths.get("form.js")));
                return;
            }
            Path f = SITE.resolve(p.substring(1)).normalize();
            boolean blocked = !f.startsWith(SITE)
                || (!SITE.equals(BACKEND) && f.startsWith(BACKEND)) // never expose backend/
                || !Files.isRegularFile(f);
            if (!blocked) for (Path seg : SITE.relativize(f)) if (seg.toString().startsWith(".")) blocked = true; // .git etc.
            String type = blocked ? null : contentType(f.getFileName().toString());
            if (type == null) { send(ex, 404, "text/plain", "Not found"); return; }
            byte[] body = Files.readAllBytes(f);
            if (f.getFileName().toString().equals("index.html")) {
                String html = new String(body, StandardCharsets.UTF_8)
                    .replace("</body>", "<script src=\"/form.js\"></script></body>");
                body = html.getBytes(StandardCharsets.UTF_8);
            }
            send(ex, 200, type, body);
        } finally { ex.close(); }
    }

    // ---------- Contact form endpoint ----------
    static void handleInquiry(HttpExchange ex) throws IOException {
        try {
            if (!ex.getRequestMethod().equals("POST")) { send(ex, 405, "application/json", "{\"error\":\"Method not allowed\"}"); return; }
            byte[] raw = ex.getRequestBody().readNBytes(10_001);
            if (raw.length > 10_000) { send(ex, 413, "application/json", "{\"error\":\"Message too large.\"}"); return; }

            Map<String, String> f = parseForm(new String(raw, StandardCharsets.UTF_8));
            if (!f.getOrDefault("website", "").isBlank()) { send(ex, 200, "application/json", "{\"ok\":true}"); return; } // honeypot

            String name = clip(f.get("name"), 120), email = clip(f.get("email"), 200);
            String phone = clip(f.get("phone"), 40), message = clip(f.get("message"), 5000);
            if (name.isEmpty() || message.isEmpty() || !email.matches("^\\S+@\\S+\\.\\S+$")) {
                send(ex, 400, "application/json", "{\"error\":\"Please complete name, a valid email and your message.\"}");
                return;
            }
            save(name, email, phone, message);
            System.out.println("New inquiry saved from " + name);
            send(ex, 200, "application/json", "{\"ok\":true}");
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, "application/json", "{\"error\":\"Server error. Please try again.\"}");
        } finally { ex.close(); }
    }

    static synchronized void save(String... cols) throws IOException {
        StringBuilder sb = new StringBuilder();
        if (!Files.exists(CSV)) sb.append("time,name,email,phone,message\n");
        sb.append(csv(Instant.now().toString()));
        for (String c : cols) sb.append(',').append(csv(c));
        sb.append('\n');
        Files.writeString(CSV, sb.toString(), StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    // ---------- Helpers ----------
    static String csv(String s) {
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s; // block spreadsheet formulas
        return "\"" + s.replace("\"", "\"\"").replace("\r", " ") + "\"";
    }
    static String clip(String s, int max) {
        s = s == null ? "" : s.trim();
        return s.length() > max ? s.substring(0, max) : s;
    }
    static Map<String, String> parseForm(String body) {
        Map<String, String> m = new HashMap<>();
        for (String pair : body.split("&")) {
            int i = pair.indexOf('=');
            if (i < 0) continue;
            m.put(URLDecoder.decode(pair.substring(0, i), StandardCharsets.UTF_8),
                  URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8));
        }
        return m;
    }
    // Only these file types are ever served; anything else (.java, .csv, .md ...) is a 404.
    static String contentType(String n) {
        n = n.toLowerCase();
        if (n.endsWith(".html")) return "text/html; charset=utf-8";
        if (n.endsWith(".css")) return "text/css";
        if (n.endsWith(".js")) return "application/javascript";
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".svg")) return "image/svg+xml";
        if (n.endsWith(".ico")) return "image/x-icon";
        if (n.endsWith(".webp")) return "image/webp";
        return null;
    }
    static void send(HttpExchange ex, int code, String type, String body) throws IOException {
        send(ex, code, type, body.getBytes(StandardCharsets.UTF_8));
    }
    static void send(HttpExchange ex, int code, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(code, body.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(body); }
    }
}
