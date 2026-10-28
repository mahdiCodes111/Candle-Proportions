/* Written collaboratively by Mahdi Chowdhury and OpenAI Codex.
Mahdi: product idea, requirements, and direction. Codex: generated implementation and explanations. */
'use strict';
const CandleChatUI = (() => {
  const get = id => document.getElementById(id);
  let configured = false, candle = null, history = [], active = null, revision = 0;
  const input = get('chat-question'), send = get('chat-send'), status = get('chat-status');
  const enabled = () => { input.disabled = !configured || !candle || !!active; send.disabled = input.disabled; };
  function reset() {
    revision++; active?.abort(); active = null; candle = null; history = [];
    get('chat-messages').replaceChildren(); input.value = ''; status.textContent = ''; enabled();
  }
  function addMessage(role, content) {
    const wrapper = document.createElement('div'); wrapper.className = `chat-message ${role}`;
    const label = document.createElement('strong'); label.textContent = role === 'user' ? 'You' : 'AI assistant';
    const text = document.createElement('p'); text.textContent = content;
    wrapper.append(label, text); get('chat-messages').append(wrapper); return wrapper;
  }
  get('chat-clear').addEventListener('click', () => { const selected = candle; reset(); candle = selected; enabled(); });
  get('chat-form').addEventListener('submit', async event => {
    event.preventDefault();
    const question = input.value.trim();
    if (!configured || !candle || active || !question) return;
    const version = revision;
    const request = new AbortController(); active = request; enabled();
    const timer = setTimeout(() => request.abort(), 65000);
    const userMessage = addMessage('user', question);
    status.textContent = 'Generating an answer…';
    try {
      // Send only the four prices. Java validates them and recalculates the proportions.
      const prices = Object.fromEntries(['open','high','low','close'].map(name => [name, candle[name].toFixed(8).replace(/\.?0+$/, '')]));
      const response = await fetch('/api/chat', {method:'POST', headers:{'Content-Type':'application/json','X-Candle-Chat':'1'}, body:JSON.stringify({question, candle:prices, history:history.slice(-4)}), signal:request.signal});
      const data = await response.json();
      if (version !== revision) return;
      if (!response.ok) throw new Error(data.error || 'Chat is unavailable. Try again later.');
      addMessage('assistant', data.answer);
      history.push({role:'user',content:question},{role:'assistant',content:data.answer});
      history = history.slice(-4); input.value = ''; status.textContent = '';
    } catch(error) {
      if (version === revision) {
        userMessage.remove();
        status.textContent = error.name === 'AbortError' ? 'The answer timed out. Retry when ready; a request already sent may still incur usage.' : error.message;
      }
    } finally {
      clearTimeout(timer);
      if (version === revision) { active = null; enabled(); input.focus(); }
    }
  });
  fetch('/api/status').then(r => { if (!r.ok) throw new Error(); return r.json(); }).then(data => {
    configured = data.chatConfigured === true;
    get('chat-setup').textContent = configured ? 'Chat is configured. Ask a question below.' : 'To enable chat, set OPENAI_API_KEY on the Java server and restart it. See README → AI candle chat.';
    enabled();
  }).catch(() => { get('chat-setup').textContent = 'Could not check chat availability. Restart the Java server and refresh.'; });
  return {reset, setCandle(value) { candle = value; enabled(); }};
})();
