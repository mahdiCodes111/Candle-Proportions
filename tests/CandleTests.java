/* Written collaboratively by Mahdi Chowdhury and OpenAI Codex.
Mahdi: product idea, requirements, and direction. Codex: generated implementation and explanations. */
import java.math.BigDecimal;
import java.util.Random;

/** Run with java -cp out CandleTests. No external testing dependencies. */
public final class CandleTests {
    private static int checks;
    static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    static CandleCalculator.Result candle(String o, String h, String l, String c) {
        return CandleCalculator.calculate(CandleCalculator.price(o), CandleCalculator.price(h), CandleCalculator.price(l), CandleCalculator.price(c));
    }
    static void near(BigDecimal actual, String expected) {
        check(actual.subtract(new BigDecimal(expected)).abs().compareTo(new BigDecimal("0.000002")) < 0, actual + " != " + expected);
    }
    static void invalid(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("Expected invalid input to be rejected");
    }
    public static void main(String[] args) throws Exception {
        var bullish = candle("100", "110", "90", "105");
        near(bullish.bodyPercent(), "25"); near(bullish.upperPercent(), "25"); near(bullish.lowerPercent(), "50");
        check(bullish.direction().equals("Bullish"), "Bullish direction");
        var bearish = candle("105", "110", "90", "100");
        near(bearish.bodyPercent(), "25"); near(bearish.upperPercent(), "25"); near(bearish.lowerPercent(), "50");
        check(bearish.direction().equals("Bearish"), "Bearish direction");
        var doji = candle("100", "110", "90", "100");
        near(doji.bodyPercent(), "0"); near(doji.upperPercent(), "50"); near(doji.lowerPercent(), "50");
        var flat = candle("100", "100", "100", "100");
        check(flat.bodyPercent() == null && flat.upperPercent() == null && flat.lowerPercent() == null, "Zero range is undefined");
        near(candle("90", "110", "90", "110").bodyPercent(), "100");
        near(candle("0.10", "0.30", "0.10", "0.20").bodyPercent(), "50");
        invalid(() -> candle("100", "99", "90", "95"));
        invalid(() -> candle("100", "110", "101", "105"));
        invalid(() -> candle("100", "90", "110", "100"));
        for (String bad : new String[]{"", "-1", "0", "NaN", "Infinity", "1e309", "1.000000001"}) invalid(() -> CandleCalculator.price(bad));
        invalid(() -> CandleCalculator.price(null));
        Random random = new Random(15);
        for (int i = 0; i < 500; i++) {
            int low = 1 + random.nextInt(10000), width = 1 + random.nextInt(1000);
            var r = candle("" + (low + random.nextInt(width + 1)), "" + (low + width), "" + low, "" + (low + random.nextInt(width + 1)));
            check(r.body().add(r.upper()).add(r.lower()).compareTo(r.range()) == 0, "Parts must sum to range");
            near(r.bodyPercent().add(r.upperPercent()).add(r.lowerPercent()), "100");
        }
        String csv = "timestamp,open,high,low,close,volume\r\n2026-10-23,100,110,90,105,1000\r\n2026-10-22,105,110,90,100,2000\r\n";
        var series = MarketData.parseCsv(csv);
        check(series.size() == 2, "CSV candle count");
        near(series.get(java.time.LocalDate.parse("2026-10-23")).bodyPercent(), "25");
        for (String bad : new String[]{"{\"Note\":\"Rate limit\"}", "{\"Information\":\"Invalid key\"}", "{\"Error Message\":\"Invalid symbol\"}", "garbage", "timestamp,open,high,low,close,volume\n2026-10-22,100,99,90,95,0"}) {
            try { MarketData.parseCsv(bad); throw new AssertionError("Expected rejected provider response"); }
            catch (MarketData.DataException expected) { checks++; }
        }
        check(!new MarketData("").configured(), "Missing key status");
        try { new MarketData("").daily("AAPL"); throw new AssertionError("Expected missing key rejection"); }
        catch (MarketData.DataException expected) { check(expected.status == 503, "Missing key HTTP status"); }
        System.out.println("Passed " + checks + " checks (calculations, validation, invariants, provider parsing).");
    }
}
