# Written collaboratively by Mahdi Chowdhury and OpenAI Codex.
# Mahdi: product idea, requirements, and direction. Codex: generated implementation and explanations.
"""Optional HTTP integration checks. Start the server without an API key first."""
import json
from urllib.request import urlopen, Request
from urllib.error import HTTPError

BASE = "http://127.0.0.1:8080"
def request(path, expected=200, method="GET"):
    try:
        with urlopen(Request(BASE + path, method=method), timeout=5) as response:
            status, body = response.status, response.read().decode()
    except HTTPError as error:
        status, body = error.code, error.read().decode()
    assert status == expected, (path, status, body)
    return body

for asset in ["/", "/styles.css", "/app.js", "/explanation.js"]:
    assert request(asset)
assert json.loads(request("/api/status"))["lookupConfigured"] is False
result = json.loads(request("/api/calculate?open=100&high=110&low=90&close=105"))["candle"]
assert [result[k] for k in ("bodyPercent", "upperPercent", "lowerPercent")] == [25, 25, 50]
flat = json.loads(request("/api/calculate?open=100&high=100&low=100&close=100"))["candle"]
assert flat["bodyPercent"] is None
for query in ["open=100&high=99&low=90&close=95", "open=NaN&high=110&low=90&close=105", "", "open=100&high=110&low=101&close=105"]:
    assert "error" in json.loads(request("/api/calculate?" + query, 400))
request("/api/candle?symbol=AAPL&date=2026-99-99", 400)
request("/api/candle?symbol=AAPL&date=2999-01-01", 400)
request("/api/candle?symbol=AAPL", 503)
request("/api/candle?symbol=%3Cscript%3E", 400)
request("/api/calculate", 405, "POST")
request("/src/CandleServer.java", 404)
print("Passed HTTP integration checks: assets, calculations, zero range, invalid input, dates, missing key, method and route restrictions.")
