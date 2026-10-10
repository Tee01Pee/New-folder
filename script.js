// Scroll-reveal animations for Prestige Law Attorneys

const targets = document.querySelectorAll(
  '.about .section-title, .about-content, ' +
  '.services .section-title, .service-card, ' +
  '.team .section-title, .team-card, ' +
  '.testimonials .section-title, .testimonial-box'
);
targets.forEach(el => el.classList.add('reveal'));

// stagger cards within each grid
document.querySelectorAll('.service-grid, .team-grid').forEach(grid => {
  [...grid.children].forEach((card, i) => card.style.setProperty('--delay', i * 0.15 + 's'));
});
document.querySelectorAll('.testimonial-box').forEach((box, i) => box.style.setProperty('--delay', i * 0.2 + 's'));

// small delay so the About text follows its title
const aboutContent = document.querySelector('.about-content');
if (aboutContent) aboutContent.style.setProperty('--delay', '0.2s');

const observer = new IntersectionObserver((entries) => {
  entries.forEach(entry => {
    if (entry.isIntersecting) {
      entry.target.classList.add('show');
      observer.unobserve(entry.target); // animate once
    }
  });
}, { threshold: 0.2 });

targets.forEach(el => observer.observe(el));
// Contact form -> Java backend
const API_URL = 'http://localhost:8080/api/contact';
const form = document.getElementById('contact-form');
const statusEl = document.getElementById('form-status');

if (form) {
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const button = form.querySelector('button[type="submit"]');
    const data = Object.fromEntries(new FormData(form));

    button.disabled = true;
    statusEl.textContent = 'Sending...';

    try {
      const response = await fetch(API_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
      });
      if (response.ok) {
        statusEl.textContent = 'Thank you, your inquiry has been sent. We will be in touch soon.';
        form.reset();
      } else {
        statusEl.textContent = 'Sorry, something went wrong. Please check your details and try again.';
      }
    } catch (error) {
      statusEl.textContent = 'Could not reach the server. Please try again later.';
    } finally {
      button.disabled = false;
    }
  });
}
