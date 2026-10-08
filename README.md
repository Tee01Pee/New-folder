# Backend for the inquiry form

This folder is the ONLY thing added to the repo. No existing file is changed or moved.

Run (needs Java 11+), from inside this folder:
  java Server.java
Open http://localhost:3000

- Your site files (index.html, style.css, script.js ...) are served from the folder above this one.
- The server adds one <script src="/form.js"> tag when serving index.html (the file on disk is untouched).
- Submissions are saved to backend/inquiries.csv (git-ignored).
- Only html/css/js/images are ever served; this folder and dotfiles are never exposed.
