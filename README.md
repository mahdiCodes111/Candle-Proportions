# Candle Proportions

A personal project to calculate the real-body, upper-wick, and lower-wick percentages of a stock’s daily candlestick.

## Why I’m Building This

I use candle proportions to check whether a stock meets my investment criteria. Currently, I take and crop screenshots from Robinhood, upload them to ChatGPT, and ask for the calculations. I’ve also used a calculator, but the process is repetitive and time-consuming.

## Goal

Make it quicker and easier to get consistent candle proportions without repeatedly taking screenshots or explaining the calculation.

## Planned First Version

- Select a stock and trading date.
- Retrieve its open, high, low, and close prices.
- Display the real-body, upper-wick, and lower-wick percentages of the candle’s total range.

## Calculations

Total range = High − Low

Real body = |Close − Open|
Upper wick = High − max(Open, Close)
Lower wick = min(Open, Close) − Low

Each percentage = (Component ÷ Total range) × 100

If High equals Low, percentages are undefined because the total range is zero.

## Technology

To be decided.

## Status

Planning and early development.
