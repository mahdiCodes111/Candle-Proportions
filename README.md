# Candle Proportions

A personal project to calculate the real-body, upper-wick, and lower-wick percentages of a stock’s daily candlestick.

## Why I’m Building This

I use candle proportions to check whether a stock meets my investment criteria. Previously, I took and cropped screenshots from Robinhood, uploaded them to ChatGPT, and asked for the calculations. I’ve also used a calculator, but the process is repetitive and time-consuming.

## Goal

Make it quicker and easier to get consistent candle proportions without repeatedly taking screenshots or explaining the calculation.

## First Version

- Enter Open, High, Low, and Close and calculate all three proportions.
- See a scaled candlestick, the percentages, raw component sizes, and price range.
- Copy the breakdown for your analysis.
- Try an explicitly labeled illustrative candle without an account or API key.
- Optionally select a stock and trading date to retrieve daily OHLC prices through Alpha Vantage.
- Reject invalid prices and explain zero-range candles.

Manual calculation works without external services. Stock lookup requires your own Alpha Vantage API key. This is a local personal application, not a deployed website.

## Technology

- **Front end:** HTML, CSS, and vanilla JavaScript.
- **Back end:** Java 17+ using the JDK HTTP server and HTTP client.
- **Data:** Alpha Vantage daily CSV, optional. No database or third-party Java dependencies.
- **Arithmetic:** `BigDecimal` preserves decimal input for subtraction and division. Percentages are calculated to six decimal places and displayed to two.

## Run in VS Code

Install a **JDK 17 or later**. VS Code’s Extension Pack for Java is helpful for editing and debugging, but the terminal commands do not need it.

Clone this repository, or run `git pull` if you already cloned it. Open the project folder in VS Code. From its terminal:

**Windows PowerShell or Command Prompt:**

```powershell
.\run.bat
```

**macOS or Linux:**

```sh
sh run.sh
```

Open **http://localhost:8080** in your browser. Click **Try an example candle**, or enter your own OHLC prices. Stop the server with Ctrl+C.

The scripts compile the Java source into `out/` and then start `CandleServer`. They invoke the JDK compiler module through `java`, so a separate `javac` executable on PATH is unnecessary. If Java reports that `jdk.compiler` is missing, install a full JDK.

You can also compile and run directly from the project root:

```sh
javac -d out src/CandleCalculator.java src/MarketData.java src/CandleServer.java
java -cp out CandleServer
```

With the Java extension installed, the included `.vscode/launch.json` also supports **Run Candle Proportions** in Run and Debug. Run scripts read any API key from the terminal's environment; restart VS Code from that environment if using its debugger instead.

## Enable stock lookup (optional)

1. Obtain a key from [Alpha Vantage](https://www.alphavantage.co/support/#api-key).
2. Stop the server, set the environment variable in your terminal, and start it again.

**Windows PowerShell:**

```powershell
$env:ALPHA_VANTAGE_API_KEY="YOUR_KEY"
.\run.bat
```

**macOS or Linux:**

```sh
export ALPHA_VANTAGE_API_KEY="YOUR_KEY"
sh run.sh
```

Keep your actual key out of source code and Git. It is used only by the Java server and is never returned to the browser. `.env` files are ignored by Git but are **not automatically loaded** by this application.

Select **Stock lookup**, enter a symbol, and optionally select a date. A blank date selects the latest available candle and shows its actual date. A specified date is matched exactly; weekends or missing dates are never silently replaced.

### Data limitations

This uses `TIME_SERIES_DAILY`, `outputsize=compact`, `datatype=csv`: daily **unadjusted** prices for the latest 100 trading days. It is not a real-time quote. Data may lag and differ from another chart’s session or adjustment settings. API plan and usage limits apply. See the [provider documentation](https://www.alphavantage.co/documentation/#daily).

Successful responses are cached in memory for 15 minutes, up to 30 symbols. Restarting the server clears that cache. Selecting another date for an already-cached symbol does not make another provider request. Errors and missing dates appear explicitly; the application does not substitute demo data for failed lookups.

The provider adapter has been checked with CSV fixtures and error fixtures. A live lookup with a personal key still needs verification.

## Calculations

```text
Total range = High − Low
Real body   = |Close − Open|
Upper wick  = High − max(Open, Close)
Lower wick  = min(Open, Close) − Low
Percentage  = (Component ÷ Total range) × 100
```

Example: Open 100, High 110, Low 90, Close 105 gives **25% body, 25% upper wick, 50% lower wick**.

If all prices are equal, the range is zero and percentages are undefined (`null` in the API, `N/A` in the UI). A doji with a nonzero range has a 0% body. Rounded percentages can total 99.99% or 100.01%.

Prices must be positive, with up to 12 digits before the decimal and 8 after it. High must be at least both Open and Close; Low must be at most both. Prices are in the instrument’s quoted units; no currency conversion is performed.

## How the code fits together

| File | Responsibility |
|---|---|
| `src/CandleCalculator.java` | Validates prices and calculates candle parts; start reading here. |
| `src/CandleServer.java` | Serves the page and exposes JSON endpoints. |
| `src/MarketData.java` | Fetches, parses, and caches optional daily market data. |
| `web/index.html` | Page structure and accessible input forms. |
| `web/styles.css` | Responsive layout and appearance. |
| `web/app.js` | Sends requests to Java and renders results. |
| `tests/CandleTests.java` | Arithmetic, validation, randomized invariants, and provider-parser tests. |
| `tests/http_smoke.py` | Optional running-server checks using Python’s standard library. |

The browser sends OHLC values to Java, Java calculates the result, and JavaScript updates the page. Editing inputs clears previous results so an old result is not mistaken for a new calculation.

## API

All endpoints use GET and return JSON. Errors return an appropriate non-200 HTTP status and an `error` string.

- `/api/status` — whether a stock-data key is configured (not a validation of the key).
- `/api/calculate?open=100&high=110&low=90&close=105` — manual calculation.
- `/api/candle?symbol=AAPL&date=2026-10-23` — exact trading-date lookup; omit date for latest available.

The server listens only on `127.0.0.1` for local use. It has no login, trade execution, or persistent portfolio storage. Do not expose it publicly without adding an appropriate hosting/security design. To change the default port, set `PORT` before starting the server.

## Test

**Windows:** `test.bat`

**macOS/Linux:** `sh test.sh`

Tests cover bullish and bearish candles, dojis, zero range, full-body candles, decimal prices, invalid OHLC, 500 generated valid candles, provider CSV parsing, provider errors, and missing API keys.

For optional HTTP checks, start the server **without an API key** on port 8080, then run `python tests/http_smoke.py` in another terminal.

## Status and next steps

First implementation complete. Java compilation, calculation/parser tests, JavaScript syntax, and local HTTP integration checks pass. Browser visual/interaction verification and a live provider lookup are still to be completed.

Suggested learning order: understand `CandleCalculator`, run its tests, follow a manual request through `CandleServer` and `web/app.js`, then connect your stock-data key. Automatic candle-pattern classifications and trading signals are intentionally outside this first version.
