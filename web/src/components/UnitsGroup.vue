<template>
  <PanelLayout
    title="Group of Units mod n"
    subtitle="Z_n* = { a ∈ [1, n-1] : gcd(a, n) = 1 } — under multiplication mod n"
    icon="🔵"
  >
    <template #theory>
      <p>The <strong>group of units mod n</strong> (written <code>Z_n*</code>) contains all integers in [1, n-1] that are coprime to n. Its size is <strong>φ(n)</strong> (Euler's totient function).</p>
      <div class="formula-box">
        <code>|Z_n*| = φ(n)</code>
        <span class="dim"> &mdash; If n = p·q (distinct primes): φ(n) = (p-1)(q-1)</span>
      </div>
      <p>RSA's public exponent <code>e</code> must be chosen from <code>Z_φ(n)*</code>, i.e. <code>gcd(e, φ(n)) = 1</code>.</p>
    </template>

    <template #inputs>
      <div class="input-group">
        <label>n (keep ≤ 2000 to enumerate instantly)</label>
        <input class="inp" v-model="nVal" placeholder="e.g. 15" type="text" @keyup.enter="compute" />
      </div>
      <button class="btn btn-primary" @click="compute" :disabled="!nVal.trim()">
        ⚡ Compute Z_n*
      </button>
    </template>

    <template #output>
      <Transition name="slide-up">
        <div v-if="result" class="output-area">
          <div class="stats-row">
            <StatCard icon="🔵" label="n" :value="String(result.n)" />
            <StatCard icon="📐" label="φ(n)" :value="String(result.phi)" color="green" />
            <StatCard icon="📊" label="|Z_n*|" :value="String(result.units.length)" color="purple" />
          </div>

          <!-- Closure check -->
          <div v-if="result.units.length >= 2" class="closure-box">
            <span class="tag">Closure check</span>
            <code class="mono">
              {{ result.cx }} × {{ result.cy }} mod {{ result.n }} =
              <strong :class="result.closureOk ? 'green' : 'red'">{{ result.cprod }}</strong>
              → {{ result.closureOk ? '✓ still a unit (group closure holds)' : '✗ NOT a unit (unexpected!)' }}
            </code>
          </div>

          <!-- Units grid -->
          <div class="units-header">
            <span>Z_{{ result.n }}* = <span class="dim">({{ result.units.length }} elements)</span></span>
          </div>
          <div class="units-grid">
            <span
              v-for="u in result.units"
              :key="u"
              class="unit-chip"
              :data-tooltip="`gcd(${u}, ${result.n}) = 1`"
            >{{ u }}</span>
          </div>
        </div>
      </Transition>
      <div v-if="error" class="result-box error">{{ error }}</div>
    </template>
  </PanelLayout>
</template>

<script setup>
import { ref } from 'vue'
import PanelLayout from './PanelLayout.vue'
import StatCard from './StatCard.vue'
import { unitsGroup, euclidGcd } from '../rsa.js'

const nVal  = ref('')
const result = ref(null)
const error  = ref('')

function compute() {
  error.value = ''; result.value = null
  try {
    const n = BigInt(nVal.value.trim())
    const units = unitsGroup(n)
    const phi = BigInt(units.length)
    let cx, cy, cprod, closureOk
    if (units.length >= 2) {
      cx = units[0]; cy = units[1]
      cprod = (cx * cy) % n
      closureOk = euclidGcd(cprod, n).gcd === 1n
    }
    result.value = { n, phi, units, cx, cy, cprod, closureOk }
  } catch (e) {
    error.value = e.message
  }
}
</script>

<style scoped>
.formula-box {
  background: var(--surface2);
  border: 1px solid var(--primary);
  border-radius: var(--radius);
  padding: .75rem 1.25rem;
  font-size: .95rem;
  color: var(--primary-l);
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}
.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
}
.closure-box {
  background: var(--surface2);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: .75rem 1rem;
  display: flex;
  align-items: center;
  gap: .75rem;
  flex-wrap: wrap;
  font-size: .9rem;
}
.units-header {
  font-size: .9rem;
  color: var(--text-dim);
  margin-top: .25rem;
}
.units-grid {
  display: flex;
  flex-wrap: wrap;
  gap: .4rem;
  max-height: 240px;
  overflow-y: auto;
  padding: .5rem;
  background: var(--surface2);
  border-radius: var(--radius);
  border: 1px solid var(--border);
}
.unit-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.4rem;
  height: 2.4rem;
  background: rgba(124,58,237,.15);
  border: 1px solid rgba(124,58,237,.3);
  border-radius: 8px;
  font-family: var(--mono);
  font-size: .82rem;
  color: var(--primary-l);
  cursor: default;
  transition: all .15s;
}
.unit-chip:hover {
  background: rgba(124,58,237,.35);
  color: #fff;
  transform: scale(1.1);
}
</style>
