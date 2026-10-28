/* Written collaboratively by Mahdi Chowdhury and OpenAI Codex.
Mahdi: product idea, requirements, and direction. Codex: generated implementation and explanations. */
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/** Serves the front end and two read-only JSON endpoints using the JDK HTTP server. */
public final class CandleServer {
    private static final MarketData market = new MarketData(System.getenv("ALPHA_VANTAGE_API_KEY"));
    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", CandleServer::handle);
        server.setExecutor(Executors.newFixedThreadPool(6));
        server.start();
        System.out.println("Candle Proportions: http://localhost:" + port);
        System.out.println("Stock lookup: " + (market.configured() ? "configured" : "optional API key not set; manual entry ready"));
    }

    private static void handle(HttpExchange exchange) throws IOException {
        try {
            if (!exchange.getRequestMethod().equals("GET")) {
                exchange.getResponseHeaders().set("Allow", "GET");
                send(exchange, 405, "application/json", "{\"error\":\"Only GET is supported.\"}"); return;
            }
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/api/status")) {
                send(exchange, 200, "application/json", "{\"lookupConfigured\":" + market.configured() + "}"); return;
            }
            if (path.equals("/api/calculate")) {
                Map<String,String> q = query(exchange.getRequestURI().getRawQuery());
                var result = CandleCalculator.calculate(CandleCalculator.price(q.get("open")), CandleCalculator.price(q.get("high")),
                    CandleCalculator.price(q.get("low")), CandleCalculator.price(q.get("close")));
                send(exchange, 200, "application/json", "{\"source\":\"Manual entry\",\"candle\":" + result.json() + "}"); return;
            }
            if (path.equals("/api/candle")) {
                Map<String,String> q = query(exchange.getRequestURI().getRawQuery());
                String symbol = q.getOrDefault("symbol", "").trim().toUpperCase(java.util.Locale.ROOT);
                if (!symbol.matches("[A-Z0-9][A-Z0-9.\\-]{0,19}")) throw new IllegalArgumentException("Enter a valid stock symbol, such as AAPL.");
                String dateText = q.getOrDefault("date", "");
                LocalDate selected = dateText.isBlank() ? null : LocalDate.parse(dateText);
                if (selected != null && selected.isAfter(LocalDate.now(ZoneId.of("America/New_York"))))
                    throw new IllegalArgumentException("Choose today or an earlier date.");
                var series = market.daily(symbol);
                if (selected == null) selected = series.candles().keySet().stream().max(LocalDate::compareTo).orElseThrow();
                var candle = series.candles().get(selected);
                if (candle == null) throw new MarketData.DataException(404, "No candle for this exact date. It may be a non-trading day or outside the latest 100 trading days.");
                send(exchange, 200, "application/json", "{\"source\":\"Alpha Vantage · daily, unadjusted\",\"symbol\":" + quote(symbol)
                    + ",\"date\":" + quote(selected.toString()) + ",\"fetchedAt\":" + quote(series.fetchedAt().toString())
                    + ",\"candle\":" + candle.json() + "}"); return;
            }
            String file = switch (path) { case "/", "/index.html" -> "index.html"; case "/styles.css" -> "styles.css"; case "/app.js" -> "app.js"; case "/explanation.js" -> "explanation.js"; default -> null; };
            if (file == null) { send(exchange, 404, "text/plain", "Not found"); return; }
            String type = file.endsWith("css") ? "text/css" : file.endsWith("js") ? "text/javascript" : "text/html";
            send(exchange, 200, type, Files.readString(Path.of("web", file)));
        } catch (MarketData.DataException e) {
            send(exchange, e.status, "application/json", "{\"error\":" + quote(e.getMessage()) + "}");
        } catch (java.time.DateTimeException e) {
            send(exchange, 400, "application/json", "{\"error\":\"Enter a valid date in YYYY-MM-DD format.\"}");
        } catch (IllegalArgumentException e) {
            send(exchange, 400, "application/json", "{\"error\":" + quote(e.getMessage()) + "}");
        } catch (Exception e) {
            send(exchange, 500, "application/json", "{\"error\":\"Something went wrong on the server. Run it from the project root and retry.\"}");
        } finally { exchange.close(); }
    }

    private static Map<String,String> query(String raw) {
        Map<String,String> values = new HashMap<>();
        if (raw == null) return values;
        if (raw.length() > 2048) throw new IllegalArgumentException("Request is too long.");
        for (String item : raw.split("&")) {
            String[] pair = item.split("=", 2);
            values.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8), pair.length < 2 ? "" : URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
        }
        return values;
    }
    private static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            if (c == '"' || c == '\\') out.append('\\').append(c);
            else if (c < 32) out.append(String.format("\\u%04x", (int)c));
            else out.append(c);
        }
        return out.append('"').toString();
    }
    private static void send(HttpExchange exchange, int code, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        var headers = exchange.getResponseHeaders();
        headers.set("Content-Type", type + "; charset=utf-8");
        headers.set("Cache-Control", "no-store");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'");
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}
