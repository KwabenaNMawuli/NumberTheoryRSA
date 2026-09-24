/**
 * rsa.js – pure-JS port of NumberTheoryRSA.java
 * All arithmetic is done with native BigInt so no precision is lost.
 */

// ─── Division Algorithm ────────────────────────────────────────────────────
/**
 * Returns { q, r } such that a = b*q + r, 0 <= r < |b|
 */
export function divisionAlgorithm(a, b) {
  if (b === 0n) throw new Error('Divisor b must be non-zero');
  let q = a / b;
  let r = a % b;
  if (r < 0n) {
    r += b < 0n ? -b : b;   // normalise into [0, |b|)
    q = (a - r) / b;
  }
  return { q, r };
}

// ─── Euclid's GCD ──────────────────────────────────────────────────────────
/**
 * Returns { gcd, steps } where steps is an array of trace lines.
 */
export function euclidGcd(a, b) {
  a = a < 0n ? -a : a;
  b = b < 0n ? -b : b;
  const steps = [];
  while (b !== 0n) {
    const { q, r } = divisionAlgorithm(a, b);
    steps.push({ a, b, q, r });
    a = b;
    b = r;
  }
  return { gcd: a, steps };
}

// ─── Extended GCD ──────────────────────────────────────────────────────────
/**
 * Returns { gcd, x, y, steps } such that a*x + b*y = gcd.
 * steps is an array suitable for a trace table.
 */
export function extendedGcd(a, b) {
  let oldR = a, r = b;
  let oldS = 1n, s = 0n;
  let oldT = 0n, t = 1n;
  const steps = [];

  while (r !== 0n) {
    const { q } = divisionAlgorithm(oldR, r);
    steps.push({ oldR, r, q, oldS, s, oldT, t });

    const tmpR = oldR - q * r;
    oldR = r; r = tmpR;
    const tmpS = oldS - q * s;
    oldS = s; s = tmpS;
    const tmpT = oldT - q * t;
    oldT = t; t = tmpT;
  }
  return { gcd: oldR, x: oldS, y: oldT, steps };
}

/**
 * Modular inverse of a mod m.  Throws if gcd != 1.
 */
export function modInverse(a, m) {
  const norm = ((a % m) + m) % m;
  const { gcd, x } = extendedGcd(norm, m);
  if (gcd !== 1n) throw new Error(`${a} has no inverse mod ${m} — gcd = ${gcd}`);
  return ((x % m) + m) % m;
}

// ─── Group of Units ────────────────────────────────────────────────────────
/**
 * Returns the list of units in Z_n* (all k in [1, n-1] with gcd(k,n)=1).
 * Throws if n > 500 to avoid freezing the browser.
 */
export function unitsGroup(n) {
  if (n < 2n) throw new Error('n must be >= 2');
  if (n > 2000n) throw new Error('n is too large to enumerate (keep it ≤ 2000)');
  const units = [];
  for (let k = 1n; k < n; k++) {
    if (euclidGcd(k, n).gcd === 1n) units.push(k);
  }
  return units;
}

// ─── RSA Key Generation ────────────────────────────────────────────────────
/**
 * Generate RSA keys given p, q, e (all BigInt or strings).
 * Returns { p, q, n, phi, e, d, valid, error }
 */
export function generateRSAKeys(pStr, qStr, eStr) {
  try {
    let p = BigInt(pStr.trim());
    let q = BigInt(qStr.trim());

    if (!isProbablyPrime(p)) return { error: 'p does not appear to be prime.' };
    if (!isProbablyPrime(q)) return { error: 'q does not appear to be prime.' };
    if (p === q) return { error: 'p and q must be distinct.' };

    const n = p * q;
    const phi = (p - 1n) * (q - 1n);

    const e = eStr.trim() === '' ? 65537n : BigInt(eStr.trim());
    const { gcd: g } = euclidGcd(e, phi);
    if (g !== 1n) return { error: `gcd(e, phi(n)) = ${g} ≠ 1 — e is not a unit mod phi(n). Choose a different e.` };

    const d = modInverse(e, phi);

    return { p, q, n, phi, e, d, valid: true };
  } catch (err) {
    return { error: err.message };
  }
}

// ─── Encrypt / Decrypt ─────────────────────────────────────────────────────
export function encryptMessage(m, e, n) {
  return modPow(m, e, n);
}

export function decryptMessage(c, d, n) {
  return modPow(c, d, n);
}

// ─── Helpers ───────────────────────────────────────────────────────────────
/**
 * Fast modular exponentiation via BigInt (square-and-multiply).
 */
export function modPow(base, exp, mod) {
  if (mod === 1n) return 0n;
  let result = 1n;
  base = base % mod;
  while (exp > 0n) {
    if (exp % 2n === 1n) result = result * base % mod;
    exp = exp / 2n;
    base = base * base % mod;
  }
  return result;
}

/**
 * Miller-Rabin primality test (deterministic for numbers < 3,317,044,064,679,887,385,961,981).
 */
export function isProbablyPrime(n) {
  if (n < 2n) return false;
  if (n === 2n || n === 3n || n === 5n || n === 7n) return true;
  if (n % 2n === 0n) return false;

  // Write n-1 as 2^r * d
  let d = n - 1n, r = 0n;
  while (d % 2n === 0n) { d /= 2n; r++; }

  // Deterministic witnesses sufficient up to 3.3 × 10^24
  const witnesses = [2n, 3n, 5n, 7n, 11n, 13n, 17n, 19n, 23n, 29n, 31n, 37n];
  for (const a of witnesses) {
    if (a >= n) continue;
    let x = modPow(a, d, n);
    if (x === 1n || x === n - 1n) continue;
    let composite = true;
    for (let i = 0n; i < r - 1n; i++) {
      x = x * x % n;
      if (x === n - 1n) { composite = false; break; }
    }
    if (composite) return false;
  }
  return true;
}

/**
 * Generate a random probable prime of approximately `bits` bits.
 * Uses crypto.getRandomValues for secure randomness.
 */
export function randomPrime(bits = 64) {
  const bytes = Math.ceil(bits / 8);
  while (true) {
    const buf = new Uint8Array(bytes);
    crypto.getRandomValues(buf);
    buf[0] |= 0x80;          // ensure top bit set (full bit-length)
    buf[bytes - 1] |= 0x01;  // ensure odd
    const candidate = BigInt('0x' + [...buf].map(b => b.toString(16).padStart(2, '0')).join(''));
    if (isProbablyPrime(candidate)) return candidate;
  }
}
