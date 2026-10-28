/* Written collaboratively by Mahdi Chowdhury and OpenAI Codex.
Mahdi: product idea, requirements, and direction. Codex: generated implementation and explanations. */
'use strict';
const $ = id => document.getElementById(id);
const manual = $('manual-form');
const lookup = $('lookup-form');
let mode = 'manual';
let current = null;
let generation = 0;
let controller = null;
const number = value => Number(value).toLocaleString('en-US', {maximumFractionDigits: 8});
const pct = value => value === null ? 'N/A' : `${value.toFixed(2)}%`;

function clearResult() {
  generation++;
  if (controller) controller.abort();
  document.querySelectorAll('button[type=submit]').forEach(button => button.disabled = false);
  current = null;
  $('result').hidden = true;
  $('empty').hidden = false;
  $('direction').textContent = 'READY WHEN YOU ARE';
  $('message').textContent = '';
}
function setMode(next) {
  clearResult(); mode = next;
  manual.hidden = next !== 'manual'; lookup.hidden = next !== 'lookup';
  for (const name of ['manual', 'lookup']) {
    $(`${name}-tab`).classList.toggle('active', name === next);
    $(`${name}-tab`).setAttribute('aria-pressed', String(name === next));
  }
}
$('manual-tab').addEventListener('click', () => setMode('manual'));
$('lookup-tab').addEventListener('click', () => setMode('lookup'));
manual.addEventListener('input', clearResult);
lookup.addEventListener('input', clearResult);

async function calculate(event, example = false) {
  event?.preventDefault();
  const form = mode === 'manual' ? manual : lookup;
  if (!form.reportValidity()) return;
  clearResult();
  const version = generation;
  const button = form.querySelector('button[type=submit]');
  controller = new AbortController();
  const requestController = controller;
  const timeout = setTimeout(() => requestController.abort(), 25000);
  button.disabled = true; $('message').textContent = 'Calculating…';
  try {
    const params = new URLSearchParams(new FormData(form));
    const response = await fetch(`/api/${mode === 'manual' ? 'calculate' : 'candle'}?${params}`, {signal: requestController.signal});
    const data = await response.json();
    if (version !== generation) return;
    if (!response.ok) throw new Error(data.error || 'Unable to calculate. Please retry.');
    current = data;
    current.example = example;
    render(data);
    $('message').textContent = example ? 'Example only — these are illustrative prices, not stock data.' : '';
  } catch (error) {
    if (version === generation) $('message').textContent = error.name === 'AbortError' ? 'Request timed out. Try again or use manual entry.' : error.message;
  } finally {
    clearTimeout(timeout);
    if (version === generation) button.disabled = false;
  }
}
manual.addEventListener('submit', calculate);
lookup.addEventListener('submit', calculate);
$('example').addEventListener('click', () => {
  setMode('manual');
  Object.entries({open:100, high:110, low:90, close:105}).forEach(([name,value]) => manual.elements[name].value = value);
  calculate(null, true);
});
function render(data) {
  const candle = data.candle;
  $('empty').hidden = true; $('result').hidden = false;
  $('direction').textContent = candle.direction.toUpperCase();
  $('result-label').textContent = data.example ? 'Example candle' : data.symbol ? `${data.symbol} · ${data.date}` : 'Manual candle';
  $('source').textContent = data.example ? 'Illustrative prices' : data.source;
  ['upper', 'body', 'lower'].forEach(part => {
    $(`${part}-percent`).textContent = pct(candle[`${part}Percent`]);
    $(`${part}-value`).textContent = `${number(candle[part])} price units`;
  });
  $('range').textContent = number(candle.range);
  $('range-note').textContent = candle.range === 0 ? 'Zero range · percentages undefined' : '100% of the candle';
  $('prices').replaceChildren();
  for (const name of ['open', 'high', 'low', 'close']) {
    const group = document.createElement('div'); const term = document.createElement('dt'); const value = document.createElement('dd');
    term.textContent = name.toUpperCase(); value.textContent = number(candle[name]);
    group.append(term, value); $('prices').append(group);
  }
  const y = price => candle.range === 0 ? 150 : 24 + (candle.high - price) / candle.range * 252;
  const top = y(Math.max(candle.open, candle.close)); const bottom = y(Math.min(candle.open, candle.close));
  $('upper-line').setAttribute('y1', y(candle.high)); $('upper-line').setAttribute('y2', top);
  $('lower-line').setAttribute('y1', bottom); $('lower-line').setAttribute('y2', y(candle.low));
  $('candle-body').setAttribute('y', top); $('candle-body').setAttribute('height', Math.max(1, bottom - top));
  $('candle-body').classList.toggle('bearish', candle.direction === 'Bearish');
  $('candle-svg').setAttribute('aria-label', `${candle.direction} candle: body ${pct(candle.bodyPercent)}, upper wick ${pct(candle.upperPercent)}, lower wick ${pct(candle.lowerPercent)}.`);
  $('data-note').textContent = data.fetchedAt
    ? `Retrieved ${new Date(data.fetchedAt).toLocaleString()}. Cached for 15 minutes. Daily data may lag; check the displayed trading date.`
    : 'Percentages use the full high-to-low range. Rounded values may total 99.99% or 100.01%.';
}
$('copy').addEventListener('click', async () => {
  if (!current) return;
  const c = current.candle;
  const text = `${$('result-label').textContent}\nOpen ${c.open} | High ${c.high} | Low ${c.low} | Close ${c.close}\nBody ${pct(c.bodyPercent)} | Upper wick ${pct(c.upperPercent)} | Lower wick ${pct(c.lowerPercent)}\nSource: ${$('source').textContent}`;
  try { await navigator.clipboard.writeText(text); $('message').textContent = 'Breakdown copied.'; }
  catch { $('message').textContent = 'Clipboard is unavailable. Select the displayed values to copy them.'; }
});
fetch('/api/status').then(response => { if (!response.ok) throw new Error(); return response.json(); }).then(status => {
  $('lookup-status').textContent = status.lookupConfigured
    ? 'Daily lookup is configured. Provider usage limits apply.'
    : 'To enable lookup, set ALPHA_VANTAGE_API_KEY before starting Java (see README). Manual entry is ready now.';
}).catch(() => $('lookup-status').textContent = 'Could not reach the Java server. Start it from the project folder, then refresh.');
