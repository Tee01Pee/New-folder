// Connects the existing contact form to the Java backend. No HTML edits needed.
(function () {
  var form = document.querySelector('#contact form') || document.querySelector('form');
  if (!form) return;
  var q = function (s) { return form.querySelector(s); };

  var hp = document.createElement('input'); // honeypot for bots
  hp.type = 'text'; hp.name = 'website'; hp.tabIndex = -1; hp.autocomplete = 'off';
  hp.style.cssText = 'position:absolute;left:-9999px;opacity:0;height:0;width:0';
  form.appendChild(hp);

  form.addEventListener('submit', function (e) {
    e.preventDefault();
    var btn = q('button[type=submit]');
    var data = new URLSearchParams({
      name: (q('input[type=text]') || {}).value || '',
      email: (q('input[type=email]') || {}).value || '',
      phone: (q('input[type=tel]') || {}).value || '',
      message: (q('textarea') || {}).value || '',
      website: hp.value
    });
    if (btn) btn.disabled = true;
    fetch('/api/inquiry', { method: 'POST', body: data })
      .then(function (r) { return r.json().then(function (j) { return { ok: r.ok, j: j }; }); })
      .then(function (r) {
        if (r.ok) { alert('Thank you! We will contact you shortly.'); form.reset(); }
        else alert(r.j.error || 'Something went wrong. Please try again.');
      })
      .catch(function () { alert('Network error. Please try again.'); })
      .then(function () { if (btn) btn.disabled = false; });
  });
})();
