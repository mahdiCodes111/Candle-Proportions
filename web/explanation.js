/* Written collaboratively by Mahdi Chowdhury and OpenAI Codex.
Mahdi: product idea, requirements, and direction. Codex: generated implementation and explanations. */
'use strict';

// Describe the observed OHLC geometry, without inferring the order of intraday moves.
// Keeping this function independent of the page makes its explanations easy to test.
function explainCandle(candle) {
  const percent = value => `${value.toFixed(2)}%`;
  const units = value => Number(value).toLocaleString('en-US', {maximumFractionDigits: 8});
  const context = 'One candle alone does not establish a trend, a reversal, or a buy/sell signal. Compare it with surrounding candles, volume, and your own criteria.';
  if (candle.range === 0) {
    return {
      title: 'Zero-range candle',
      points: ['Open, high, low, and close are all equal. The supplied prices show no trading range.',
        'The body and both wicks have zero size. Percentages are undefined because there is no range to divide by.'],
      context
    };
  }

  const bullish = candle.direction === 'Bullish';
  const unchanged = candle.direction === 'Unchanged';
  const title = unchanged ? 'Equal open and close' : `${candle.direction} candle`;
  const points = [];
  points.push(unchanged
    ? `The session opened and closed at the same price, despite a high-to-low range of ${units(candle.range)}. There was no net change from open to close.`
    : `The close finished ${bullish ? 'above' : 'below'} the open by ${units(candle.body)} price units. This is a ${bullish ? 'bullish' : 'bearish'} candle for this session.`);

  let bodyDescription;
  if (candle.body === 0) bodyDescription = 'There is no real body; the full range consists of wicks.';
  else if (candle.upper === 0 && candle.lower === 0) bodyDescription = 'The body covers the entire range, with no upper or lower wick.';
  else if (candle.body > candle.upper + candle.lower) bodyDescription = 'The body is larger than both wicks combined, so the open-to-close move accounts for most of the range.';
  else if (candle.body < candle.upper + candle.lower) bodyDescription = 'Both wicks combined are larger than the body, so the open-to-close move accounts for less than half the range.';
  else bodyDescription = 'The body and the combined wicks each account for half of the range.';
  points.push(`Real body: ${percent(candle.bodyPercent)} of the total range. ${bodyDescription}`);

  points.push(candle.upper === 0
    ? 'No upper wick: the high equals the higher of the open and close.'
    : `Upper wick: ${percent(candle.upperPercent)} of the range. The high extended ${units(candle.upper)} price units above the higher of the open and close.`);
  points.push(candle.lower === 0
    ? 'No lower wick: the low equals the lower of the open and close.'
    : `Lower wick: ${percent(candle.lowerPercent)} of the range. The low extended ${units(candle.lower)} price units below the lower of the open and close.`);

  // Use distance from the appropriate end of the body to avoid subtracting large prices.
  const closeFromLow = candle.lower + (bullish ? candle.body : 0);
  const closePosition = closeFromLow / candle.range * 100;
  const location = closeFromLow === 0 ? 'at the session low'
    : closeFromLow === candle.range ? 'at the session high'
    : closeFromLow < candle.range / 2 ? 'in the lower half of the range'
    : closeFromLow > candle.range / 2 ? 'in the upper half of the range' : 'at the midpoint of the range';
  points.push(`The close is ${location} (${percent(closePosition)} of the way from low to high).`);
  return {title, points, context};
}

// The browser uses this file before app.js; Node uses this export for optional tests.
if (typeof module !== 'undefined' && module.exports) module.exports = {explainCandle};
