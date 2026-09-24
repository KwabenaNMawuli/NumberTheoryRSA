import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * NumberTheoryRSA
 * ----------------
 * A teaching console app that builds RSA key generation up from its
 * number-theoretic parts, showing the work at each stage instead of
 * hiding it behind a library call:
 *
 *   1. The Division Algorithm      (a = bq + r)
 *   2. Euclid's GCD Algorithm      (repeated application of #1)
 *   3. Modular Arithmetic          (the remainders from #1 form Z_n)
 *   4. Extended Euclidean Algo     (Bezout coefficients -> modular inverse)
 *   5. Group of Units mod n        (Z_n*, and why e must live inside it)
 *
 * RSA key generation is then just: use #5 to pick e, use #4 to find its
 * inverse d, and the correctness of encryption/decryption falls out of
 * the group structure (Euler's theorem on Z_phi(n)*).
 *
 * Compile: javac NumberTheoryRSA.java
 * Run:     java NumberTheoryRSA
 */
public class NumberTheoryRSA {

    static final Scanner IN = new Scanner(System.in);
    static final BigInteger ONE = BigInteger.ONE;
    static final BigInteger ZERO = BigInteger.ZERO;

    // Keys generated in the "Generate RSA keys" step, reused by encrypt/decrypt
    static BigInteger pubE = null, pubN = null, privD = null, phiN = null;

    public static void main(String[] args) {
        boolean running = true;
        while (running) {
            printMenu();
            String choice = IN.nextLine().trim();
            try {
                switch (choice) {
                    case "1": demoDivisionAlgorithm(); break;
                    case "2": demoEuclidGcd(); break;
                    case "3": demoExtendedGcdAndInverse(); break;
                    case "4": demoUnitsGroup(); break;
                    case "5": generateRSAKeys(); break;
                    case "6": encryptDecryptDemo(); break;
                    case "0": running = false; break;
                    default: System.out.println("Not a valid option.\n");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage() + "\n");
            }
        }
        System.out.println("Bye.");
    }

    static void printMenu() {
        System.out.println("========================================");
        System.out.println(" 1. Division Algorithm            (a = bq + r)");
        System.out.println(" 2. Euclid's GCD Algorithm         (traced)");
        System.out.println(" 3. Extended Euclid / Mod Inverse  (traced)");
        System.out.println(" 4. Group of Units mod n           (Z_n*)");
        System.out.println(" 5. Generate RSA Key Pair          (full walkthrough)");
        System.out.println(" 6. Encrypt / Decrypt a message with the last keys");
        System.out.println(" 0. Exit");
        System.out.println("========================================");
        System.out.print("Choose: ");
    }

    // ------------------------------------------------------------------
    // 1. DIVISION ALGORITHM
    // For any a, b (b != 0) there exist unique q, r with a = bq + r, 0 <= r < |b|.
    // Every other algorithm here is built out of repeated calls to this.
    // ------------------------------------------------------------------
    static BigInteger[] divisionAlgorithm(BigInteger a, BigInteger b) {
        BigInteger[] qr = a.divideAndRemainder(b); // {quotient, remainder}
        if (qr[1].signum() < 0) { // normalize remainder into [0, |b|)
            qr[1] = qr[1].add(b.abs());
            qr[0] = a.subtract(qr[1]).divide(b);
        }
        return qr;
    }

    static void demoDivisionAlgorithm() {
        BigInteger a = readBigInteger("Enter a: ");
        BigInteger b = readBigInteger("Enter b (nonzero): ");
        BigInteger[] qr = divisionAlgorithm(a, b);
        System.out.println("\nDivision Algorithm: a = b*q + r");
        System.out.println(a + " = " + b + " * " + qr[0] + " + " + qr[1]);
        System.out.println("So a mod b = " + qr[1] + "  (this remainder IS modular arithmetic)\n");
    }

    // ------------------------------------------------------------------
    // 2. EUCLID'S GCD
    // gcd(a,b) = gcd(b, a mod b), bottoming out when the remainder hits 0.
    // Each line of the trace is one call to the division algorithm.
    // ------------------------------------------------------------------
    static BigInteger gcdTrace(BigInteger a, BigInteger b, boolean verbose) {
        a = a.abs(); b = b.abs();
        if (verbose) System.out.println("\nEuclid's GCD trace:");
        while (!b.equals(ZERO)) {
            BigInteger[] qr = divisionAlgorithm(a, b);
            if (verbose) System.out.println(a + " = " + b + " * " + qr[0] + " + " + qr[1]);
            a = b;
            b = qr[1];
        }
        if (verbose) System.out.println("gcd = " + a + "\n");
        return a;
    }

    static void demoEuclidGcd() {
        BigInteger a = readBigInteger("Enter a: ");
        BigInteger b = readBigInteger("Enter b: ");
        gcdTrace(a, b, true);
    }

    // ------------------------------------------------------------------
    // 3. EXTENDED EUCLIDEAN ALGORITHM
    // Finds x, y such that a*x + b*y = gcd(a,b) (Bezout's identity).
    // When gcd(a,b) = 1, x is exactly a's inverse mod b -- this is the
    // step that produces RSA's private exponent d.
    // ------------------------------------------------------------------
    static BigInteger[] extendedGcd(BigInteger a, BigInteger b) {
        BigInteger oldR = a, r = b;
        BigInteger oldS = ONE, s = ZERO;
        BigInteger oldT = ZERO, t = ONE;

        while (!r.equals(ZERO)) {
            BigInteger[] qr = divisionAlgorithm(oldR, r);
            BigInteger q = qr[0];

            BigInteger tmpR = oldR.subtract(q.multiply(r));
            oldR = r; r = tmpR;

            BigInteger tmpS = oldS.subtract(q.multiply(s));
            oldS = s; s = tmpS;

            BigInteger tmpT = oldT.subtract(q.multiply(t));
            oldT = t; t = tmpT;
        }
        // oldR = gcd(a,b), oldS/oldT = Bezout coefficients: a*oldS + b*oldT = oldR
        return new BigInteger[]{oldR, oldS, oldT};
    }

    static BigInteger modInverse(BigInteger a, BigInteger m) {
        BigInteger[] res = extendedGcd(a.mod(m), m);
        BigInteger g = res[0], x = res[1];
        if (!g.equals(ONE)) {
            throw new ArithmeticException(a + " has no inverse mod " + m
                    + " because gcd(a,m) = " + g + " != 1 (a is not a unit mod m)");
        }
        return x.mod(m); // normalize into [0, m)
    }

    static void demoExtendedGcdAndInverse() {
        BigInteger a = readBigInteger("Enter a: ");
        BigInteger m = readBigInteger("Enter m (modulus): ");
        BigInteger[] res = extendedGcd(a, m);
        System.out.println("\nExtended Euclid: a*x + m*y = gcd(a,m)");
        System.out.println(a + "*(" + res[1] + ") + " + m + "*(" + res[2] + ") = " + res[0]);
        if (res[0].equals(ONE)) {
            BigInteger inv = res[1].mod(m);
            System.out.println("gcd = 1, so a is a unit mod m.");
            System.out.println("Modular inverse of " + a + " mod " + m + " is " + inv);
            System.out.println("Check: (" + a + " * " + inv + ") mod " + m + " = "
                    + a.multiply(inv).mod(m) + "\n");
        } else {
            System.out.println("gcd != 1, so " + a + " is NOT a unit mod " + m
                    + " and has no inverse.\n");
        }
    }

    // ------------------------------------------------------------------
    // 4. GROUP OF UNITS MOD n  (Z_n*)
    // Z_n* = { a in [1, n-1] : gcd(a, n) = 1 }, a group under multiplication mod n.
    // |Z_n*| = phi(n). RSA's public exponent e must be chosen from Z_phi(n)*,
    // which is exactly the gcd(e, phi(n)) = 1 check.
    // ------------------------------------------------------------------
    static void demoUnitsGroup() {
        BigInteger n = readBigInteger("Enter n (keep it small, this lists every unit): ");
        if (n.compareTo(BigInteger.valueOf(2)) < 0) {
            System.out.println("Need n >= 2.\n");
            return;
        }
        List<BigInteger> units = new ArrayList<>();
        for (BigInteger k = ONE; k.compareTo(n) < 0; k = k.add(ONE)) {
            if (gcdTrace(k, n, false).equals(ONE)) units.add(k);
        }
        System.out.println("\nZ_" + n + "* = " + units);
        System.out.println("|Z_" + n + "*| = phi(" + n + ") = " + units.size());

        if (units.size() >= 2) {
            BigInteger x = units.get(0), y = units.get(1);
            BigInteger prod = x.multiply(y).mod(n);
            boolean prodIsUnit = gcdTrace(prod, n, false).equals(ONE);
            System.out.println("Closure check: " + x + " * " + y + " mod " + n + " = " + prod
                    + " -> " + (prodIsUnit ? "still a unit (as required for a group)" : "NOT a unit (something's wrong)"));
        }
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 5. RSA KEY GENERATION
    // Ties everything together:
    //   n = p*q
    //   phi(n) = (p-1)(q-1)            [ |Z_n*| when n = p*q, p,q distinct primes ]
    //   pick e in Z_phi(n)*            [ gcd(e, phi(n)) = 1, via Euclid's GCD ]
    //   d = e^-1 mod phi(n)            [ via Extended Euclid ]
    // ------------------------------------------------------------------
    static void generateRSAKeys() {
        System.out.println("\n-- RSA Key Generation --");
        System.out.print("Enter prime p (or blank to generate a random 512-bit prime): ");
        String pIn = IN.nextLine().trim();
        System.out.print("Enter prime q (or blank to generate a random 512-bit prime): ");
        String qIn = IN.nextLine().trim();

        java.security.SecureRandom rnd = new java.security.SecureRandom();
        BigInteger p = pIn.isEmpty() ? BigInteger.probablePrime(512, rnd) : new BigInteger(pIn);
        BigInteger q = qIn.isEmpty() ? BigInteger.probablePrime(512, rnd) : new BigInteger(qIn);

        if (!p.isProbablePrime(50) || !q.isProbablePrime(50)) {
            System.out.println("p and q must both be prime.\n");
            return;
        }
        if (p.equals(q)) {
            System.out.println("p and q must be distinct.\n");
            return;
        }

        BigInteger n = p.multiply(q);
        BigInteger phi = p.subtract(ONE).multiply(q.subtract(ONE));

        System.out.println("\np = " + p);
        System.out.println("q = " + q);
        System.out.println("n = p*q = " + n);
        System.out.println("phi(n) = (p-1)(q-1) = " + phi
                + "   [this is |Z_n*|, the size of the units group mod n]");

        System.out.print("\nEnter e, must satisfy gcd(e, phi(n)) = 1 (blank for default 65537): ");
        String eIn = IN.nextLine().trim();
        BigInteger e = eIn.isEmpty() ? BigInteger.valueOf(65537) : new BigInteger(eIn);

        BigInteger g = gcdTrace(e, phi, false);
        System.out.println("gcd(e, phi(n)) = " + g
                + (g.equals(ONE) ? "  -> e is a unit in Z_phi(n)*, valid choice" : "  -> INVALID, e is not a unit"));
        if (!g.equals(ONE)) {
            System.out.println("Pick a different e and try again.\n");
            return;
        }

        BigInteger d = modInverse(e, phi);
        System.out.println("d = e^-1 mod phi(n), found via Extended Euclid = " + d);
        System.out.println("Check: (e*d) mod phi(n) = " + e.multiply(d).mod(phi) + " (should be 1)");

        pubE = e; pubN = n; privD = d; phiN = phi;

        System.out.println("\nPublic key  (e, n) = (" + e + ", " + n + ")");
        System.out.println("Private key (d, n) = (" + d + ", " + n + ")\n");
    }

    // ------------------------------------------------------------------
    // Encrypt/decrypt demo using the last generated key pair.
    // Correctness (m^(ed) = m mod n) is Euler's theorem applied to the
    // group Z_n*, the same group idea explored in option 4.
    // ------------------------------------------------------------------
    static void encryptDecryptDemo() {
        if (pubE == null) {
            System.out.println("Generate a key pair first (option 5).\n");
            return;
        }
        BigInteger m = readBigInteger("Enter message m as an integer (0 <= m < n): ");
        if (m.signum() < 0 || m.compareTo(pubN) >= 0) {
            System.out.println("m must be in [0, n).\n");
            return;
        }
        BigInteger c = m.modPow(pubE, pubN);
        BigInteger back = c.modPow(privD, pubN);
        System.out.println("\nEncrypt: c = m^e mod n = " + c);
        System.out.println("Decrypt: m' = c^d mod n = " + back);
        System.out.println(back.equals(m) ? "Matches original message.\n" : "MISMATCH (unexpected).\n");
    }

    // ------------------------------------------------------------------
    static BigInteger readBigInteger(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = IN.nextLine().trim();
            try {
                return new BigInteger(line);
            } catch (NumberFormatException ex) {
                System.out.println("Not a valid integer, try again.");
            }
        }
    }
}