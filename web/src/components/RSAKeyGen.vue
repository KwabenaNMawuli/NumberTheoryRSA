<template>
  <PanelLayout
    title="RSA Key Generation"
    subtitle="Full walkthrough: n = p·q · φ(n) = (p-1)(q-1) · d = e⁻¹ mod φ(n)"
    icon="🔑"
  >
    <template #theory>
      <p>RSA key generation ties all the number theory together:</p>
      <ol class="theory-steps">
        <li>Choose two distinct primes <strong>p</strong> and <strong>q</strong></li>
        <li>Compute <code>n = p·q</code> (the modulus)</li>
        <li>Compute <code>φ(n) = (p-1)(q-1)</code> — the size of Z_n*</li>
        <li>Pick <code>e</code> with <code>gcd(e, φ(n)) = 1</code> (e is a unit in Z_φ(n)*)</li>
        <li>Find <code>d = e⁻¹ mod φ(n)</code> via Extended Euclid</li>
        <li><strong>Public key:</strong> (e, n) &nbsp;·&nbsp; <strong>Private key:</strong> (d, n)</li>
      </ol>
    </template>

    <template #inputs>
      <div class="input-row">
        <div class="input-group">
          <label>p (prime)</label>
          <div class="inp-with-btn">
            <input class="inp" v-model="pVal" placeholder="blank = random 64-bit prime" type="text" />
            <button class="btn btn-ghost btn-sm" @click="genP" :disabled="generating">🎲</button>
          </div>
        </div>
        <div class="input-group">
          <label>q (prime, distinct from p)</label>
          <div class="inp-with-btn">
            <input class="inp" v-model="qVal" placeholder="blank = random 64-bit prime" type="text" />
            <button class="btn btn-ghost btn-sm" @click="genQ" :disabled="generating">🎲</button>
          </div>
        </div>
      </div>
      <div class="input-group">
        <label>e (public exponent, blank = 65537)</label>
        <input class="inp" v-model="eVal" placeholder="65537" type="text" />
      </div>
      <div class="btn-row">
        <button class="btn btn-ghost" @click="genBoth" :disabled="generating">🎲 Random p &amp; q</button>
        <button class="btn btn-primary" @click="generate" :disabled="generating || !canGenerate">
          {{ generating ? '⏳ Generating…' : '🔑 Generate Keys' }}
        </button>
      </div>
    </template>

    <template #output>
      <Transition name="slide-up">
        <div v-if="result" class="output-area">
          <!-- Steps walkthrough -->
          <div class="steps-list">
            <div class="step-item">
              <div class="step-num">1</div>
              <div class="step-body">
                <div class="step-title">Primes</div>
                <div class="mono dim small-mono">p = <span class="gold">{{ truncate(result.p) }}</span></div>
                <div class="mono dim small-mono">q = <span class="gold">{{ truncate(result.q) }}</span></div>
              </div>
            </div>
            <div class="step-item">
              <div class="step-num">2</div>
              <div class="step-body">
                <div class="step-title">Modulus n = p·q</div>
                <div class="mono small-mono" style="color:var(--accent-l)">{{ truncate(result.n) }}</div>
              </div>
            </div>
            <div class="step-item">
              <div class="step-num">3</div>
              <div class="step-body">
                <div class="step-title">φ(n) = (p-1)(q-1) &nbsp;<span class="tag">|Z_n*|</span></div>
                <div class="mono small-mono dim">{{ truncate(result.phi) }}</div>
              </div>
            </div>
            <div class="step-item">
              <div class="step-num">4</div>
              <div class="step-body">
                <div class="step-title">Public exponent e</div>
                <div class="mono">
                  e = <span class="purple">{{ result.e }}</span>
                  &nbsp;· gcd(e, φ(n)) = <span class="green">1 ✓</span>
                </div>
              </div>
            </div>
            <div class="step-item">
              <div class="step-num">5</div>
              <div class="step-body">
                <div class="step-title">Private exponent d = e⁻¹ mod φ(n)</div>
                <div class="mono small-mono dim">{{ truncate(result.d) }}</div>
                <div class="dim" style="font-size:.78rem">e·d mod φ(n) = <span class="green">{{ result.check }}</span></div>
              </div>
            </div>
          </div>

          <!-- Key display -->
          <div class="keys-grid">
            <div class="key-card public">
              <div class="key-header">🔓 Public Key (e, n)</div>
              <div class="key-body">
                <div class="key-line">
                  <span class="klabel">e</span>
                  <span class="mono kval gold">{{ result.e }}</span>
                  <button class="copy-btn" @click="copy(result.e)" title="Copy">📋</button>
                </div>
                <div class="key-line">
                  <span class="klabel">n</span>
                  <span class="mono kval dim">{{ truncate(result.n) }}</span>
                  <button class="copy-btn" @click="copy(result.n)" title="Copy">📋</button>
                </div>
              </div>
            </div>
            <div class="key-card private">
              <div class="key-header">🔐 Private Key (d, n)</div>
              <div class="key-body">
                <div class="key-line">
                  <span class="klabel">d</span>
                  <span class="mono kval" style="color:var(--red)">{{ truncate(result.d) }}</span>
                  <button class="copy-btn" @click="copy(result.d)" title="Copy">📋</button>
                </div>
                <div class="key-line">
                  <span class="klabel">n</span>
                  <span class="mono kval dim">{{ truncate(result.n) }}</span>
                  <button class="copy-btn" @click="copy(result.n)" title="Copy">📋</button>
                </div>
              </div>
            </div>
          </div>

          <div class="result-box success">
            ✓ Keys generated. Switch to the <strong>Encrypt / Decrypt</strong> tab to test them.
          </div>
        </div>
      </Transition>
      <div v-if="error" class="result-box error">{{ error }}</div>
    </template>
  </PanelLayout>
</template>

<script setup>
import { ref, computed } from 'vue'
import PanelLayout from './PanelLayout.vue'
import { generateRSAKeys, randomPrime } from '../rsa.js'

const emit = defineEmits(['keys-updated'])

const pVal = ref(''), qVal = ref(''), eVal = ref('')
const result = ref(null)
const error = ref('')
const generating = ref(false)

const canGenerate = computed(() => pVal.value.trim() !== '' && qVal.value.trim() !== '')

function genP() {
  generating.value = true
  setTimeout(() => { pVal.value = String(randomPrime(64)); generating.value = false }, 10)
}
function genQ() {
  generating.value = true
  setTimeout(() => { qVal.value = String(randomPrime(64)); generating.value = false }, 10)
}
function genBoth() {
  generating.value = true
  setTimeout(() => {
    pVal.value = String(randomPrime(64))
    qVal.value = String(randomPrime(64))
    generating.value = false
  }, 10)
}

function generate() {
  error.value = ''; result.value = null
  if (!pVal.value.trim() || !qVal.value.trim()) {
    error.value = 'Please enter (or generate) both p and q.'; return
  }
  const res = generateRSAKeys(pVal.value, qVal.value, eVal.value)
  if (res.error) { error.value = res.error; return }
  res.check = (res.e * res.d) % res.phi
  result.value = res
  emit('keys-updated', { e: res.e, d: res.d, n: res.n, phi: res.phi })
}

function truncate(val) {
  const s = String(val)
  return s.length > 60 ? s.slice(0, 28) + '…' + s.slice(-28) : s
}

function copy(val) {
  navigator.clipboard.writeText(String(val)).catch(() => {})
}
</script>

<style scoped>
.theory-steps {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: .35rem;
  counter-reset: step;
}
.theory-steps li {
  counter-increment: step;
  padding-left: 2rem;
  position: relative;
  font-size: .9rem;
}
.theory-steps li::before {
  content: counter(step);
  position: absolute;
  left: 0;
  top: 0;
  width: 1.4rem;
  height: 1.4rem;
  background: rgba(124,58,237,.3);
  color: var(--primary-l);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: .7rem;
  font-weight: 700;
}

.input-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.inp-with-btn { display: flex; gap: .5rem; }
.btn-sm { padding: .4rem .7rem; border-radius: 9px; font-size: .9rem; }
.btn-row { display: flex; gap: .75rem; flex-wrap: wrap; }

/* Steps walkthrough */
.steps-list { display: flex; flex-direction: column; gap: .5rem; }
.step-item {
  display: flex;
  gap: .75rem;
  background: var(--surface2);
  border-radius: var(--radius);
  border: 1px solid var(--border);
  padding: .75rem 1rem;
  align-items: flex-start;
}
.step-num {
  width: 1.75rem;
  height: 1.75rem;
  min-width: 1.75rem;
  background: rgba(124,58,237,.3);
  color: var(--primary-l);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: .8rem;
  margin-top: .1rem;
}
.step-body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: .2rem; }
.step-title { font-size: .85rem; color: var(--text-dim); }
.small-mono { font-size: .78rem; word-break: break-all; }

/* Keys */
.keys-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.key-card {
  border-radius: var(--radius);
  border: 1px solid var(--border);
  overflow: hidden;
}
.key-card.public { border-color: rgba(16,185,129,.4); }
.key-card.private { border-color: rgba(239,68,68,.4); }
.key-header {
  padding: .6rem 1rem;
  font-weight: 600;
  font-size: .85rem;
  background: var(--surface2);
  border-bottom: 1px solid var(--border);
}
.key-body { padding: .75rem 1rem; display: flex; flex-direction: column; gap: .5rem; }
.key-line { display: flex; align-items: center; gap: .5rem; min-width: 0; }
.klabel { font-size: .7rem; color: var(--text-dim); text-transform: uppercase; letter-spacing: .06em; min-width: 1rem; }
.kval { font-size: .78rem; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.copy-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: .9rem;
  padding: .1rem .2rem;
  border-radius: 4px;
  opacity: .6;
  transition: opacity .15s;
}
.copy-btn:hover { opacity: 1; }

@media (max-width: 600px) {
  .keys-grid { grid-template-columns: 1fr; }
  .input-row { grid-template-columns: 1fr; }
}
</style>
