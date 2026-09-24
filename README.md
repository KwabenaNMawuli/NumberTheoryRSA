# 🔐 Number Theory & RSA Explorer

An interactive web application that teaches RSA cryptography from first principles — building every concept up from the **Division Algorithm** through **Euler's theorem**, showing all the work at each step.

## Live Demo
> Deploy via Vercel — run `vercel` inside the `web/` folder.

## Features

| Module | What it teaches |
|--------|----------------|
| ➗ Division Algorithm | `a = b·q + r` — the atomic operation everything else is built from |
| ⚙️ Euclid's GCD | Step-by-step trace table of repeated division |
| 🔁 Extended GCD | Bézout coefficients and modular inverse computation |
| 🔵 Group of Units | `Z_n*` visualised as a chip grid, with φ(n) and closure check |
| 🔑 RSA Key Generation | Full walkthrough: pick primes → compute n, φ(n), e, d |
| 🛡️ Encrypt / Decrypt | `c = mᵉ mod n`, `m = cᵈ mod n` with visual flow diagram |
| 🎬 Demo Mode | Auto-running guided tour ending with a cascading credits reveal |

## Tech Stack
- **Vue 3** + **Vite** — component-based SPA, no backend
- Native **BigInt** — arbitrary-precision arithmetic, no external math library
- **Miller-Rabin** primality testing — fast, deterministic for numbers up to 3.3 × 10²⁴
- **crypto.getRandomValues** — cryptographically secure random prime generation

## Project Structure
```
NumberTheoryRSA/
├── src/
│   └── NumberTheoryRSA.java   ← original terminal app
└── web/                       ← Vue + Vite web app
    ├── index.html
    └── src/
        ├── rsa.js             ← pure-JS number theory library
        ├── App.vue            ← shell, tabs, particle canvas
        ├── style.css          ← design system (dark indigo/violet/gold)
        └── components/
            ├── DemoMode.vue
            ├── DivisionAlgorithm.vue
            ├── EuclidGcd.vue
            ├── ExtendedGcd.vue
            ├── UnitsGroup.vue
            ├── RSAKeyGen.vue
            └── EncryptDecrypt.vue
```

## Running Locally
```bash
cd web
npm install
npm run dev       # → http://localhost:5173
```

## Group Members
| # | Name | ID |
|---|------|----|
| 1 | Kwabena Nyamekye Mawuli | 11296675 |
| 2 | Afful Nana Kwasi Obeng | 11091069 |
| 3 | Justine Addo | 11170480 |
| 4 | Raphael Ayeh Mensah | 11016071 |
| 5 | Yeboah Eugene Amaning | 11163402 |
| 6 | Edusei Derrick Akuamoah | 11115682 |
| 7 | Boateng Stanley Kofi | 11253377 |
| 8 | Caleb Danquah | 11024572 |
| 9 | Kelly Buabeng | 11180171 |
| 10 | Nathaniel Addo | 11352845 |
