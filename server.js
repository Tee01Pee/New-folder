require('dotenv').config();
const express = require('express');
const path = require('path');
const nodemailer = require('nodemailer');
const rateLimit = require('express-rate-limit');

const app = express();
app.use(express.json({ limit: '10kb' }));

// Serve only the homepage (keeps server.js and .env from being exposed)
app.get('/', (req, res) => res.sendFile(path.join(__dirname, 'index.html')));

const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST,
  port: Number(process.env.SMTP_PORT) || 587,
  secure: Number(process.env.SMTP_PORT) === 465,
  auth: { user: process.env.SMTP_USER, pass: process.env.SMTP_PASS },
});

// Max 5 submissions per IP per 15 minutes
const limiter = rateLimit({ windowMs: 15 * 60 * 1000, max: 5 });

const clean = (v, max = 2000) => String(v || '').trim().slice(0, max);

app.post('/api/contact', limiter, async (req, res) => {
  // Hidden "website" field is a honeypot: real people leave it empty
  if (req.body.website) return res.json({ ok: true });

  const name = clean(req.body.name, 100).replace(/[\r\n]/g, ' ');
  const email = clean(req.body.email, 200);
  const phone = clean(req.body.phone, 50);
  const message = clean(req.body.message);

  if (!name || !/^\S+@\S+\.\S+$/.test(email) || !message) {
    return res.status(400).json({ ok: false, error: 'Please fill in name, a valid email and your message.' });
  }

  try {
    await transporter.sendMail({
      from: process.env.SMTP_USER,
      to: process.env.NOTIFY_TO, // comma-separate to notify several people
      replyTo: email,
      subject: `New enquiry from ${name}`,
      text: `Name: ${name}\nEmail: ${email}\nPhone: ${phone || '-'}\n\n${message}`,
    });
    res.json({ ok: true });
  } catch (err) {
    console.error('Mail error:', err.message);
    res.status(500).json({ ok: false, error: 'Could not send right now. Please try again later.' });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`Running on http://localhost:${PORT}`));
