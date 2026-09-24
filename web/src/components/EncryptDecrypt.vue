<template>
  <PanelLayout
    title="Encrypt &amp; Decrypt"
    subtitle="c = mᵉ mod n  ·  m = cᵈ mod n  —  Euler's theorem in action"
    icon="🛡️"
  >
    <template #theory>
      <p>Correctness follows from <strong>Euler's theorem</strong> on <code>Z_n*</code>:</p>
      <div class="formula-box">
        <code>m^(e·d) ≡ m (mod n)</code>
        <span class="dim"> because e·d ≡ 1 (mod φ(n))</span>
      </div>
      <p>The same group structure explored in the <em>Units Group</em> tab guarantees that encryption is perfectly reversible.</p>
    </template>

    <template #inputs>
      <!-- Key status banner -->
      <div v-if="!keys" class="no-keys-banner">
        <span>⚠️</span>
        <span>No keys loaded yet — generate a key pair in the <strong>RSA Key Gen</strong> tab first.</span>
      </div>
      <div v-else class="keys-loaded-banner">
        <span class="tag green">🔑 Keys loaded</span>
        <span class="dim small">e = {{ keys.e }} &nbsp;·&nbsp; n = {{ truncate(keys.n) }}</span>
      </div>

      <!-- Manual key override -->
      <details class="manual-keys">
        <summary>Enter keys manually</summary>
        <div class="manual-grid">
          <div class="input-group">
            <label>e (public exponent)</label>
            <input class="inp" v-model="manE" placeholder="e.g. 65537" type="text" />
          </div>
          <div class="input-group">
            <label>d (private exponent)</label>
            <input class="inp" v-model="manD" placeholder="private key d" type="text" />
          </div>
          <div class="input-group" style="grid-column: 1 / -1">
            <label>n (modulus)</label>
            <input class="inp" v-model="manN" placeholder="modulus n = p·q" type="text" />
          </div>
        </div>
      </details>

      <div class="input-group">
        <label>Message m (integer, 0 ≤ m &lt; n)</label>
        <input class="inp" v-model="mVal" placeholder="e.g. 42" type="text" @keyup.enter="compute" />
      </div>

      <button class="btn btn-primary" @click="compute" :disabled="!canCompute">
        ⚡ Encrypt &amp; Decrypt
      </button>
    </template>

    <template #output>
      <Transition name="slide-up">
        <div v-if="result" class="output-area">
          <!-- Flow diagram -->
          <div class="flow-row">
            <div class="flow-node original">
              <div class="fn-label">Original m</div>
              <div class="fn-val mono gold">{{ result.m }}</div>
            </div>
            <div class="flow-arrow">
              <div class="arrow-line"></div>
              <div class="arrow-label">c = m<sup>e</sup> mod n</div>
            </div>
            <div class="flow-node ciphertext">
              <div class="fn-label">Ciphertext c</div>
              <div class="fn-val mono purple">{{ truncate(result.c) }}</div>
            </div>
            <div class="flow-arrow">
              <div class="arrow-line"></div>
              <div class="arrow-label">m = c<sup>d</sup> mod n</div>
            </div>
            <div class="flow-node recovered" :class="{ mismatch: !result.match }">
              <div class="fn-label">Recovered m'</div>
              <div class="fn-val mono" :class="result.match ? 'green' : 'red'">{{ result.back }}</div>
            </div>
          </div>

          <div class="result-box" :class="result.match ? 'success' : 'error'">
            {{ result.match
              ? '✓ Decrypted value matches original — Euler\'s theorem holds!'
              : '✗ Mismatch — message m must be in [0, n)' }}
          </div>

          <!-- Details table -->
          <details class="trace-details" open>
            <summary>Step details</summary>
            <table class="trace-table" style="margin-top:.75rem">
              <tbody>
                <tr>
                  <td class="dim">m (plaintext)</td>
                  <td class="mono gold">{{ result.m }}</td>
                </tr>
                <tr>
                  <td class="dim">e (public exponent)</td>
                  <td class="mono purple">{{ result.e }}</td>
                </tr>
                <tr>
                  <td class="dim">n (modulus)</td>
                  <td class="mono dim">{{ truncate(result.n) }}</td>
                </tr>
                <tr>
                  <td class="dim">c = m^e mod n</td>
                  <td class="mono" style="color:var(--accent-l)">{{ truncate(result.c) }}</td>
                </tr>
                <tr>
                  <td class="dim">d (private exponent)</td>
                  <td class="mono" style="color:var(--red)">{{ truncate(result.d) }}</td>
                </tr>
                <tr>
                  <td class="dim">m' = c^d mod n</td>
                  <td class="mono" :class="result.match ? 'green' : 'red'">{{ result.back }}</td>
                </tr>
              </tbody>
            </table>
          </details>
        </div>
      </Transition>
      <div v-if="error" class="result-box error">{{ error }}</div>
    </template>
  </PanelLayout>
</template>

<script setup>
import { ref, computed } from 'vue'
import PanelLayout from './PanelLayout.vue'
import { encryptMessage, decryptMessage } from '../rsa.js'

const props = defineProps({ keys: Object })

const mVal = ref('')
const manE = ref(''), manD = ref(''), manN = ref('')
const result = ref(null)
const error  = ref('')

const activeKeys = computed(() => {
  // prefer manual input if provided
  if (manE.value.trim() && manD.value.trim() && manN.value.trim()) {
    try {
      return { e: BigInt(manE.value), d: BigInt(manD.value), n: BigInt(manN.value) }
    } catch { return null }
  }
  return props.keys
})

const canCompute = computed(() => !!activeKeys.value && mVal.value.trim() !== '')

function compute() {
  error.value = ''; result.value = null
  try {
    const { e, d, n } = activeKeys.value
    const m = BigInt(mVal.value.trim())
    if (m < 0n || m >= n) throw new Error('m must be in [0, n)')
    const c    = encryptMessage(m, e, n)
    const back = decryptMessage(c, d, n)
    result.value = { m, e, d, n, c, back, match: back === m }
  } catch (err) {
    error.value = err.message
  }
}

function truncate(val) {
  const s = String(val)
  return s.length > 40 ? s.slice(0, 18) + '…' + s.slice(-18) : s
}
</script>

<style scoped>
.formula-box {
  background: var(--surface2);
  border: 1px solid var(--primary);
  border-radius: var(--radius);
  padding: .75rem 1.25rem;
  font-size: 1rem;
  color: var(--primary-l);
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}
.no-keys-banner {
  display: flex;
  gap: .5rem;
  align-items: center;
  background: rgba(245,158,11,.1);
  border: 1px solid rgba(245,158,11,.3);
  border-radius: var(--radius);
  padding: .75rem 1rem;
  font-size: .88rem;
  color: var(--accent-l);
}
.keys-loaded-banner {
  display: flex;
  align-items: center;
  gap: .75rem;
  padding: .5rem .75rem;
  background: rgba(16,185,129,.06);
  border: 1px solid rgba(16,185,129,.3);
  border-radius: var(--radius);
  flex-wrap: wrap;
}
.small { font-size: .78rem; font-family: var(--mono); word-break: break-all; }

.manual-keys {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: .5rem 1rem;
}
.manual-keys summary { cursor: pointer; color: var(--text-dim); font-size: .82rem; user-select: none; }
.manual-grid { display: grid; grid-template-columns: 1fr 1fr; gap: .75rem; margin-top: .75rem; }

/* Flow diagram */
.flow-row {
  display: flex;
  align-items: center;
  gap: .5rem;
  flex-wrap: wrap;
  justify-content: center;
  padding: 1rem 0;
}
.flow-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: .3rem;
  background: var(--surface2);
  border: 2px solid var(--border);
  border-radius: 12px;
  padding: .75rem 1.1rem;
  min-width: 120px;
  text-align: center;
}
.flow-node.original  { border-color: var(--accent); }
.flow-node.ciphertext { border-color: var(--primary); }
.flow-node.recovered  { border-color: var(--green); }
.flow-node.mismatch   { border-color: var(--red); }
.fn-label { font-size: .72rem; color: var(--text-dim); text-transform: uppercase; letter-spacing: .05em; }
.fn-val { font-size: .88rem; word-break: break-all; max-width: 150px; }

.flow-arrow {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: .2rem;
}
.arrow-line {
  width: 48px;
  height: 2px;
  background: linear-gradient(to right, var(--primary), var(--primary-l));
  position: relative;
}
.arrow-line::after {
  content: '▶';
  position: absolute;
  right: -8px;
  top: -8px;
  font-size: .7rem;
  color: var(--primary-l);
}
.arrow-label { font-size: .65rem; color: var(--text-dim); white-space: nowrap; }

.trace-details {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: .5rem 1rem;
}
.trace-details summary { cursor: pointer; color: var(--text-dim); font-size: .85rem; user-select: none; }

@media (max-width: 600px) {
  .flow-row { flex-direction: column; }
  .flow-arrow { transform: rotate(90deg); }
  .manual-grid { grid-template-columns: 1fr; }
}
</style>
