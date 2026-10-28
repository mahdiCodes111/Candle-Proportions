/* Written collaboratively by Mahdi Chowdhury and OpenAI Codex.
Mahdi: product idea, requirements, and direction. Codex: generated implementation and explanations. */
'use strict';
const assert = require('node:assert/strict');
const {explainCandle} = require('../web/explanation.js');
function candle(open, high, low, close) {
  const range = high-low, body=Math.abs(close-open), upper=high-Math.max(open,close), lower=Math.min(open,close)-low;
  return {open,high,low,close,range,body,upper,lower,bodyPercent:range?body/range*100:null,upperPercent:range?upper/range*100:null,lowerPercent:range?lower/range*100:null,direction:close>open?'Bullish':close<open?'Bearish':'Unchanged'};
}
const text = c => explainCandle(c).points.join(' ');
assert.match(text(candle(100,110,90,105)), /above.*5 price units/);
assert.match(text(candle(100,110,90,105)), /75.00% of the way/);
assert.match(text(candle(105,110,90,100)), /below.*5 price units/);
assert.match(text(candle(105,110,90,100)), /midpoint.*50.00%/);
assert.match(text(candle(100,110,90,100)), /no real body/);
assert.match(text(candle(100,110,90,100)), /0.00% of the total/);
assert.match(text(candle(90,110,90,110)), /body covers the entire range/);
assert.match(text(candle(90,110,90,110)), /at the session high/);
assert.match(text(candle(110,110,90,90)), /at the session low/);
assert.match(text(candle(91,110,90,108)), /body is larger than both wicks/);
assert.match(text(candle(95,110,90,105)), /each account for half/);
assert.match(text(candle(100,110,90,105)), /wicks combined are larger/);
assert.match(text(candle(100,100,100,100)), /Percentages are undefined/);
assert.doesNotMatch(text(candle(100,100,100,100)), /NaN|Infinity|\d+%/);
assert.match(text(candle(99,110,90,91)), /lower half/);
assert.match(text(candle(0.1,0.3,0.1,0.2)), /50.00% of the way/);
assert.match(explainCandle(candle(100,110,90,105)).context,/does not establish a trend/);
console.log('Passed 17 candle explanation checks.');
