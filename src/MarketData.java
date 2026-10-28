import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/** Optional Alpha Vantage adapter. The API key stays in the Java process. */
public final class MarketData {
    public static class DataException extends Exception {
        public final int status;
        public DataException(int status, String message) { super(message); this.status = status; }
    }
    public record Series(Map<LocalDate, CandleCalculator.Result> candles, Instant fetchedAt) {}
    private final String apiKey;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final Map<String, Series> cache = new LinkedHashMap<>();
    public MarketData(String apiKey) { this.apiKey = apiKey == null ? "" : apiKey.trim(); }
    public boolean configured() { return !apiKey.isEmpty(); }

    // Synchronized to prevent concurrent repeated requests consuming the provider quota.
    public synchronized Series daily(String symbol) throws DataException {
        if (!configured()) throw new DataException(503, "Stock lookup needs an ALPHA_VANTAGE_API_KEY on the server. Manual entry works without a key.");
        Series saved = cache.get(symbol);
        if (saved != null && saved.fetchedAt().plusSeconds(900).isAfter(Instant.now())) return saved;
        String url = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&outputsize=compact&datatype=csv&symbol="
            + URLEncoder.encode(symbol, StandardCharsets.UTF_8) + "&apikey=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 429) throw new DataException(429, "The data provider's rate limit was reached. Try again later or enter prices manually.");
            if (response.statusCode() != 200) throw new DataException(502, "The price provider is unavailable. Try again later or enter prices manually.");
            Series series = new Series(parseCsv(response.body()), Instant.now());
            if (cache.size() >= 30) cache.remove(cache.keySet().iterator().next());
            cache.put(symbol, series);
            return series;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataException(502, "The price request was interrupted. Please retry.");
        } catch (IOException e) {
            throw new DataException(502, "Could not reach the price provider. Check your connection or use manual entry.");
        }
    }

    static Map<LocalDate, CandleCalculator.Result> parseCsv(String csv) throws DataException {
        if (csv == null || csv.length() > 1_000_000) throw new DataException(502, "Unexpected price response.");
        String clean = csv.strip();
        if (!clean.startsWith("timestamp,open,high,low,close,volume")) {
            if (clean.contains("Information") || clean.contains("Note"))
                throw new DataException(429, "The provider rejected the request. Check your API key, plan, or daily request limit; manual entry is still available.");
            if (clean.contains("Error Message")) throw new DataException(404, "No prices found. Check the stock symbol.");
            throw new DataException(502, "The provider returned an unexpected format. Please try again later.");
        }
        Map<LocalDate, CandleCalculator.Result> result = new TreeMap<>();
        String[] lines = clean.split("\\R");
        try {
            for (int i = 1; i < lines.length; i++) {
                if (lines[i].isBlank()) continue;
                String[] fields = lines[i].split(",", -1);
                if (fields.length != 6) throw new IllegalArgumentException();
                LocalDate date = LocalDate.parse(fields[0]);
                var candle = CandleCalculator.calculate(CandleCalculator.price(fields[1]), CandleCalculator.price(fields[2]),
                    CandleCalculator.price(fields[3]), CandleCalculator.price(fields[4]));
                result.put(date, candle);
            }
        } catch (IllegalArgumentException | java.time.DateTimeException e) {
            throw new DataException(502, "The provider returned invalid candle data.");
        }
        if (result.isEmpty()) throw new DataException(404, "No daily prices are available for this symbol.");
        return result;
    }
}
