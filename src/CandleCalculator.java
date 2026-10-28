import java.math.BigDecimal;
import java.math.RoundingMode;

/** Pure calculation: no HTTP or UI code, so it is easy to test independently. */
public final class CandleCalculator {
    public record Result(BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close,
                         BigDecimal range, BigDecimal body, BigDecimal upper, BigDecimal lower,
                         BigDecimal bodyPercent, BigDecimal upperPercent, BigDecimal lowerPercent) {
        public String direction() { return close.compareTo(open) > 0 ? "Bullish" : close.compareTo(open) < 0 ? "Bearish" : "Unchanged"; }
        public String json() {
            return "{\"open\":" + open + ",\"high\":" + high + ",\"low\":" + low + ",\"close\":" + close
                + ",\"range\":" + range + ",\"body\":" + body + ",\"upper\":" + upper + ",\"lower\":" + lower
                + ",\"bodyPercent\":" + bodyPercent + ",\"upperPercent\":" + upperPercent
                + ",\"lowerPercent\":" + lowerPercent + ",\"direction\":\"" + direction() + "\"}";
        }
    }

    public static BigDecimal price(String text) {
        if (text == null || !text.matches("[0-9]{1,12}(\\.[0-9]{1,8})?"))
            throw new IllegalArgumentException("Enter a positive price with up to 8 decimal places.");
        BigDecimal value = new BigDecimal(text);
        if (value.signum() <= 0) throw new IllegalArgumentException("Prices must be greater than zero.");
        return value;
    }

    public static Result calculate(BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close) {
        if (open == null || high == null || low == null || close == null
            || open.signum() <= 0 || high.signum() <= 0 || low.signum() <= 0 || close.signum() <= 0)
            throw new IllegalArgumentException("All four prices must be greater than zero.");
        if (high.compareTo(low) < 0 || high.compareTo(open.max(close)) < 0 || low.compareTo(open.min(close)) > 0)
            throw new IllegalArgumentException("High must be at least Open and Close; Low must be at most Open and Close.");
        BigDecimal range = high.subtract(low);
        BigDecimal body = close.subtract(open).abs();
        BigDecimal upper = high.subtract(open.max(close));
        BigDecimal lower = open.min(close).subtract(low);
        return new Result(open, high, low, close, range, body, upper, lower,
            percent(body, range), percent(upper, range), percent(lower, range));
    }

    private static BigDecimal percent(BigDecimal part, BigDecimal range) {
        // null is intentional: a zero-range candle has no defined proportions.
        return range.signum() == 0 ? null : part.multiply(BigDecimal.valueOf(100)).divide(range, 6, RoundingMode.HALF_UP);
    }
}
