/*
 * Written collaboratively by Mahdi Chowdhury and OpenAI Codex.
 * Mahdi: product idea, requirements, and direction. Codex: generated implementation and explanations.
 *
 * Reading guide: price() validates text; calculate() computes sizes;
 * percent() handles division; Result carries the answer back to the server.
 * Example: O=100, H=110, L=90, C=105 -> body 25%, upper 25%, lower 50%.
 */
// Imports BigDecimal, a decimal-number class. Constructing it from text preserves the entered
// decimal value without the binary floating-point approximation of double.
import java.math.BigDecimal;
// Imports the rounding rules used when dividing; some fractions have infinitely many decimal
// places.
import java.math.RoundingMode;

/** Pure calculation: no HTTP or UI code, so it is easy to test independently. */
// Declares the class named after this file. public lets other classes use it; final prevents
// subclasses. The opening brace begins its body. This class has no main method: the server and
// tests call its methods.
public final class CandleCalculator {
    // Declares a public nested record: a data container for one candle. These first four components
    // hold the original prices. Java generates a constructor, accessors such as open(), equals(),
    // hashCode(), and toString(). Record component fields are final; BigDecimal values are
    // immutable too.
    public record Result(BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close,
                         // Continues the record's component list with the total price range and the
                         // raw sizes of the body and wicks, all in price units.
                         BigDecimal range, BigDecimal body, BigDecimal upper, BigDecimal lower,
                         // Adds the three percentage components, closes the component list, and
                         // opens the record body for our custom methods. Percentages may be null
                         // when the range is zero.
                         BigDecimal bodyPercent, BigDecimal upperPercent, BigDecimal lowerPercent) {
        // Defines a method returning a String. compareTo compares decimal values numerically:
        // positive means close > open, negative means close < open, and zero means equal. The
        // nested condition ? valueIfTrue : valueIfFalse expressions choose Bullish, then Bearish,
        // otherwise Unchanged. This describes the candle's price direction, not an investment
        // prediction.
        public String direction() { return close.compareTo(open) > 0 ? "Bullish" : close.compareTo(open) < 0 ? "Bearish" : "Unchanged"; }
        // Starts a method that converts this result into JSON text so the server can send it to the
        // browser. JSON is a data format, not a Java object.
        public String json() {
            // Begins the JSON object and appends the four prices. A backslash before a double quote
            // lets a Java string contain that quote. The + operator joins text and values here; it
            // does not add the prices. JSON property names are quoted and numeric values are not.
            return "{\"open\":" + open + ",\"high\":" + high + ",\"low\":" + low + ",\"close\":" + close
                // Continues the same return expression with the total range and the three raw
                // component sizes. Commas separate JSON properties.
                + ",\"range\":" + range + ",\"body\":" + body + ",\"upper\":" + upper + ",\"lower\":" + lower
                // Appends the body and upper-wick percentages. Concatenating a null reference
                // writes the unquoted text null, which is valid JSON and means the percentage is
                // undefined.
                + ",\"bodyPercent\":" + bodyPercent + ",\"upperPercent\":" + upperPercent
                // Appends the lower-wick percentage, calls direction(), and surrounds its text
                // result with JSON quotes. The final brace closes the JSON object; the semicolon
                // ends the return statement. All inserted values here are BigDecimal, null, or our
                // fixed direction labels; arbitrary user text would require JSON escaping.
                + ",\"lowerPercent\":" + lowerPercent + ",\"direction\":\"" + direction() + "\"}";
        } // End of the json() method.
    } // End of the Result record.

    // Starts a public helper that converts one input String into a BigDecimal. static means callers
    // use CandleCalculator.price(text) without creating a CandleCalculator object.
    public static BigDecimal price(String text) {
        // Checks whether input is missing OR fails the allowed format. || short-circuits, so
        // text.matches is not called on null. ! means NOT. The regular expression requires 1–12
        // digits, optionally followed by a decimal point and 1–8 digits. Java's doubled backslash
        // makes the regex treat the dot literally. matches checks the whole string; signs, spaces,
        // scientific notation, and commas are rejected.
        if (text == null || !text.matches("[0-9]{1,12}(\\.[0-9]{1,8})?"))
            // If that condition is true, throws an exception and stops this method. The server
            // catches it and returns an input-error message. Without braces, only this one
            // statement belongs to the preceding if.
            throw new IllegalArgumentException("Enter a positive price with up to 8 decimal places.");
        // Creates the decimal value from the validated text and stores the reference in a local
        // variable named value.
        BigDecimal value = new BigDecimal(text);
        // signum() returns -1, 0, or 1 for negative, zero, or positive numbers. Rejecting <= 0
        // enforces positive stock prices; zero passed the format check but must still be rejected.
        if (value.signum() <= 0) throw new IllegalArgumentException("Prices must be greater than zero.");
        // Returns the validated decimal number to the caller. return ends the method; the closing
        // brace below ends its definition.
        return value;
    } // End of the price() method.

    // Starts the main calculation method. It receives four decimal prices and returns one Result
    // containing the inputs and calculated values. Because it is static, callers use
    // CandleCalculator.calculate(...).
    public static Result calculate(BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close) {
        // Begins a check for any missing price. null means no object was supplied. Checking all
        // four references first makes the following signum calls safe because || stops evaluating
        // once it finds a true condition.
        if (open == null || high == null || low == null || close == null
            // Continues the same if condition: reject any nonpositive value. This repeats value
            // validation because calculate can be called directly without first calling price().
            // Format/decimal-place limits are enforced by price(), not by calculate().
            || open.signum() <= 0 || high.signum() <= 0 || low.signum() <= 0 || close.signum() <= 0)
            // Stops the calculation if any value was missing, zero, or negative; no Result is
            // returned.
            throw new IllegalArgumentException("All four prices must be greater than zero.");
        // Rejects impossible OHLC combinations: high below low, high below the larger of
        // open/close, or low above the smaller of open/close. max chooses the larger decimal and
        // min the smaller. compareTo ignores differences in decimal scale, such as 100.0 versus
        // 100.00. Equal boundaries are allowed.
        if (high.compareTo(low) < 0 || high.compareTo(open.max(close)) < 0 || low.compareTo(open.min(close)) > 0)
            // Reports the inconsistent candle to the caller instead of producing negative wick
            // sizes.
            throw new IllegalArgumentException("High must be at least Open and Close; Low must be at most Open and Close.");
        // Calculates the full high-to-low distance: range = high - low. BigDecimal methods return
        // new values; they do not modify high or low.
        BigDecimal range = high.subtract(low);
        // Subtracts open from close and takes the absolute value, so the body's size is nonnegative
        // for both bullish and bearish candles.
        BigDecimal body = close.subtract(open).abs();
        // Measures the upper wick from the higher body edge to the high: high - max(open, close).
        BigDecimal upper = high.subtract(open.max(close));
        // Measures the lower wick from the low to the lower body edge: min(open, close) - low.
        BigDecimal lower = open.min(close).subtract(low);
        // Creates the Result using the record's generated constructor. Argument order matches the
        // component declarations above; this line supplies the original prices and raw sizes.
        return new Result(open, high, low, close, range, body, upper, lower,
            // Calculates each component's percentage with our helper, supplies the final three
            // constructor arguments, and returns the completed Result.
            percent(body, range), percent(upper, range), percent(lower, range));
    } // End of the calculate() method.

    // Starts a helper accessible only inside CandleCalculator because it is private. It converts a
    // component's raw size into a percentage of the full candle range.
    private static BigDecimal percent(BigDecimal part, BigDecimal range) {
        // null is intentional: a zero-range candle has no defined proportions.
        // If the range is zero, returns null without evaluating the division. Otherwise, multiplies
        // part by 100 and divides by range. valueOf(100) creates the decimal multiplier; 6 requests
        // six decimal places in the result. HALF_UP rounds to the nearest value, with ties rounded
        // away from zero. The browser separately displays two decimal places. Example: 5 × 100 ÷ 20
        // = 25%.
        return range.signum() == 0 ? null : part.multiply(BigDecimal.valueOf(100)).divide(range, 6, RoundingMode.HALF_UP);
    } // End of the percent() method.
} // End of the CandleCalculator class.
