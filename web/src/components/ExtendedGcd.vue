<template>
  <PanelLayout
    title="Extended Euclidean Algorithm"
    subtitle="Find Bézout coefficients x, y — and the modular inverse"
    icon="🔁"
  >
    <template #theory>
      <p><strong>Bézout's Identity:</strong> for any a, m, there exist integers x, y such that:</p>
      <div class="formula-box">
        <code>a·x + m·y = gcd(a, m)</code>
      </div>
      <p>When <code>gcd(a, m) = 1</code>, then <code>a·x ≡ 1 (mod m)</code> — meaning <strong>x is the modular inverse of a</strong>. This is exactly how RSA's private exponent <code>d</code> is computed from <code>e</code>.</p>
    </template>

    <template #inputs>
      <div class="input-row">
        <div class="input-group">
          <label>a</label>
          <input class="inp" v-model="aVal" placeholder="e.g. 3" type="text" @keyup.enter="compute" />
        </div>
        <div class="input-group">
          <label>m (modulus)</label>
          <input class="inp" v-model="mVal" placeholder="e.g. 11" type="text" @keyup.enter="compute" />
        </div>
      </div>
      <button class="btn btn-primary" @click="compute" :disabled="!canCompute">
        ⚡ Compute
      </button>
    </template>

    <template #output>
      <Transition name="slide-up">
        <div v-if="result" class="output-area">
          <!-- Bézout identity display -->
          <div class="bezout-display">
            <span class="mono gold">{{ result.a }}</span>
            <span class="dim">·</span>
            <span class="mono purple">({{ result.x }})</span>
            <span class="dim">+</span>
            <span class="mono gold">{{ result.m }}</span>
            <span class="dim">·</span>
            <span class="mono purple">({{ result.y }})</span>
            <span class="dim">=</span>
            <span class="mono green">{{ result.gcd }}</span>
          </div>

          <!-- Inverse result -->
          <div v-if="result.gcd === 1n" class="inverse-card card-glow">
            <div class="inv-header">
              <span class="tag green">gcd = 1 → unit exists</span>
            </div>
            <div class="inv-equation">
              <code>{{ result.a }}⁻¹ mod {{ result.m }} = <strong class="gold">{{ result.inv }}</strong></code>
            </div>
            <div class="check-row">
              <span class="dim">Verification:</span>
              <code class="mono">
                ({{ result.a }} × {{ result.inv }}) mod {{ result.m }} =
                <strong class="green">{{ result.check }}</strong>
                ✓
              </code>
            </div>
          </div>
          <div v-else class="result-box error">
            gcd({{ result.a }}, {{ result.m }}) = {{ result.gcd }} ≠ 1 →
            {{ result.a }} is NOT a unit mod {{ result.m }} and has no inverse.
          </div>

          <!-- Trace table -->
          <details class="trace-details">
            <summary>Show extended Euclid trace ({{ result.steps.length }} steps)</summary>
            <div class="trace-scroll">
              <table class="trace-table" style="margin-top:.75rem">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>oldR</th>
                    <th>r</th>
                    <th>q</th>
                    <th>oldS (x)</th>
                    <th>s</th>
                    <th>oldT (y)</th>
                    <th>t</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(s, i) in result.steps" :key="i">
                    <td><span class="step-badge">{{ i + 1 }}</span></td>
                    <td class="mono gold">{{ s.oldR }}</td>
                    <td class="mono">{{ s.r }}</td>
                    <td class="mono green">{{ s.q }}</td>
                    <td class="mono purple">{{ s.oldS }}</td>
                    <td class="mono">{{ s.s }}</td>
                    <td class="mono">{{ s.oldT }}</td>
                    <td class="mono">{{ s.t }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
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
import { extendedGcd } from '../rsa.js'

const aVal = ref(''), mVal = ref('')
const result = ref(null)
const error  = ref('')
const canCompute = computed(() => aVal.value.trim() !== '' && mVal.value.trim() !== '')

function compute() {
  error.value = ''; result.value = null
  try {
    const a = BigInt(aVal.value.trim())
    const m = BigInt(mVal.value.trim())
    const { gcd, x, y, steps } = extendedGcd(a, m)
    const inv = gcd === 1n ? ((x % m) + m) % m : null
    const check = inv !== null ? (a * inv) % m : null
    result.value = { a, m, gcd, x, y, steps, inv, check }
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
  font-size: 1rem;
  color: var(--primary-l);
}
.input-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.bezout-display {
  display: flex;
  align-items: center;
  gap: .5rem;
  flex-wrap: wrap;
  font-size: 1.3rem;
  font-family: var(--mono);
  padding: .75rem;
  background: var(--surface2);
  border-radius: var(--radius);
  border: 1px solid var(--border);
  margin-bottom: .75rem;
}
.inverse-card {
  background: rgba(16,185,129,.06);
  border: 1px solid var(--green);
  border-radius: var(--radius);
  padding: 1rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: .6rem;
  margin-bottom: .75rem;
}
.inv-equation {
  font-size: 1.15rem;
  font-family: var(--mono);
}
.check-row {
  display: flex;
  gap: .5rem;
  align-items: center;
  font-size: .9rem;
  flex-wrap: wrap;
}
.trace-details {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: .5rem 1rem;
}
.trace-details summary {
  cursor: pointer;
  color: var(--text-dim);
  font-size: .85rem;
  padding: .25rem 0;
  user-select: none;
}
.trace-details summary:hover { color: var(--text); }
.trace-scroll { overflow-x: auto; }
</style>
