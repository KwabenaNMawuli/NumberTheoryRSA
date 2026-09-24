<template>
  <PanelLayout
    title="Euclid's GCD Algorithm"
    subtitle="gcd(a,b) = gcd(b, a mod b) — traced step by step"
    icon="⚙️"
  >
    <template #theory>
      <p>Euclid's algorithm finds the <strong>Greatest Common Divisor</strong> by repeatedly applying the division algorithm. Each row of the trace below is exactly one call to <em>a = b·q + r</em>, then we replace <em>(a, b)</em> with <em>(b, r)</em>.</p>
      <div class="formula-box">
        <code>gcd(a, b) = gcd(b, a mod b)</code>
        &nbsp;until remainder = 0
      </div>
      <p class="dim" style="font-size:.85rem">The last non-zero remainder is the GCD. This works because any divisor of both a and b also divides the remainder.</p>
    </template>

    <template #inputs>
      <div class="input-row">
        <div class="input-group">
          <label>a</label>
          <input class="inp" v-model="aVal" placeholder="e.g. 252" type="text" @keyup.enter="compute" />
        </div>
        <div class="input-group">
          <label>b</label>
          <input class="inp" v-model="bVal" placeholder="e.g. 105" type="text" @keyup.enter="compute" />
        </div>
      </div>
      <button class="btn btn-primary" @click="compute" :disabled="!canCompute">
        ⚡ Trace GCD
      </button>
    </template>

    <template #output>
      <Transition name="slide-up">
        <div v-if="steps.length" class="output-area">
          <div class="gcd-result">
            <span class="dim">gcd({{ aVal }}, {{ bVal }})</span>
            <span class="eq-arrow">→</span>
            <span class="gcd-val">{{ gcdVal }}</span>
          </div>

          <div class="trace-scroll">
            <table class="trace-table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>a</th>
                  <th>=</th>
                  <th>b</th>
                  <th>×</th>
                  <th>q</th>
                  <th>+</th>
                  <th>r</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(s, i) in steps" :key="i" :class="{ 'last-step': i === steps.length - 1 }">
                  <td><span class="step-badge">{{ i + 1 }}</span></td>
                  <td class="mono gold">{{ s.a }}</td>
                  <td class="mono dim">=</td>
                  <td class="mono purple">{{ s.b }}</td>
                  <td class="mono dim">×</td>
                  <td class="mono green">{{ s.q }}</td>
                  <td class="mono dim">+</td>
                  <td class="mono" :class="i === steps.length - 1 ? 'red' : 'gold-l'">{{ s.r }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <p class="dim" style="font-size:.82rem;margin-top:.5rem">
            ↑ Last non-zero remainder in column <em>r</em> is the GCD.
            The algorithm terminates when r = 0 (shown in red).
          </p>
        </div>
      </Transition>
      <div v-if="error" class="result-box error">{{ error }}</div>
    </template>
  </PanelLayout>
</template>

<script setup>
import { ref, computed } from 'vue'
import PanelLayout from './PanelLayout.vue'
import { euclidGcd } from '../rsa.js'

const aVal = ref(''), bVal = ref('')
const steps = ref([])
const gcdVal = ref(null)
const error = ref('')

const canCompute = computed(() => aVal.value.trim() !== '' && bVal.value.trim() !== '')

function compute() {
  error.value = ''; steps.value = []; gcdVal.value = null
  try {
    const a = BigInt(aVal.value.trim())
    const b = BigInt(bVal.value.trim())
    const res = euclidGcd(a, b)
    steps.value = res.steps
    gcdVal.value = res.gcd
    // append a final row showing last step -> 0
    if (res.steps.length > 0) {
      const last = res.steps[res.steps.length - 1]
      steps.value = [...res.steps, { a: last.b, b: last.r, q: last.b / last.r || 0n, r: 0n, _final: true }]
    }
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
  display: flex;
  align-items: center;
  gap: .75rem;
  flex-wrap: wrap;
}
.input-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}
.gcd-result {
  display: flex;
  align-items: center;
  gap: 1rem;
  font-size: 1.2rem;
  margin-bottom: 1rem;
}
.eq-arrow { color: var(--text-dim); }
.gcd-val { font-family: var(--mono); font-size: 2rem; font-weight: 700; color: var(--green); }
.trace-scroll { overflow-x: auto; }
.last-step td { background: rgba(239,68,68,.08); }
.gold-l { color: var(--accent-l); }
</style>
