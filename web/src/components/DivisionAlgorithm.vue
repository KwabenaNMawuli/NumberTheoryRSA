<template>
  <PanelLayout
    title="Division Algorithm"
    subtitle="For any a, b (b ≠ 0) there exist unique q, r with a = b·q + r, 0 ≤ r < |b|"
    icon="➗"
  >
    <template #theory>
      <p>The Division Algorithm is the atomic operation everything else is built from.
         Every subsequent algorithm (GCD, Extended GCD, RSA) is just repeated applications of this single step.</p>
      <div class="formula-box">
        <code>a = b · q + r &nbsp;&nbsp; where &nbsp; 0 ≤ r &lt; |b|</code>
      </div>
      <ul class="theory-list">
        <li><strong>q</strong> is the <em>quotient</em></li>
        <li><strong>r</strong> is the <em>remainder</em> — this IS modular arithmetic: <code>r = a mod b</code></li>
        <li>The remainder lives in <strong>[0, |b|)</strong>, always non-negative</li>
      </ul>
    </template>

    <template #inputs>
      <div class="input-row">
        <div class="input-group">
          <label>a (dividend)</label>
          <input class="inp" v-model="aVal" placeholder="e.g. 37" type="text" @keyup.enter="compute" />
        </div>
        <div class="input-group">
          <label>b (divisor, nonzero)</label>
          <input class="inp" v-model="bVal" placeholder="e.g. 5" type="text" @keyup.enter="compute" />
        </div>
      </div>
      <button class="btn btn-primary" @click="compute" :disabled="!canCompute">
        ⚡ Compute
      </button>
    </template>

    <template #output>
      <Transition name="slide-up">
        <div v-if="result" class="output-area">
          <div class="equation-display">
            <span class="eq-part gold">{{ result.a }}</span>
            <span class="eq-op">=</span>
            <span class="eq-part purple">{{ result.b }}</span>
            <span class="eq-op">·</span>
            <span class="eq-part green">{{ result.q }}</span>
            <span class="eq-op">+</span>
            <span class="eq-part" style="color:var(--accent-l)">{{ result.r }}</span>
          </div>
          <div class="legend-row">
            <LegendChip color="gold"   label="a" desc="Dividend" />
            <LegendChip color="purple" label="b" desc="Divisor" />
            <LegendChip color="green"  label="q" desc="Quotient" />
            <LegendChip color="accent" label="r" desc="Remainder = a mod b" />
          </div>
          <div class="result-box success">
            {{ result.a }} mod {{ result.b }} = <strong style="color:var(--accent-l)">{{ result.r }}</strong>
            &nbsp; (because {{ result.a }} = {{ result.b }} × {{ result.q }} + {{ result.r }})
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
import LegendChip from './LegendChip.vue'
import { divisionAlgorithm } from '../rsa.js'

const aVal = ref('')
const bVal = ref('')
const result = ref(null)
const error  = ref('')

const canCompute = computed(() => aVal.value.trim() !== '' && bVal.value.trim() !== '')

function compute() {
  error.value = ''; result.value = null
  try {
    const a = BigInt(aVal.value.trim())
    const b = BigInt(bVal.value.trim())
    const { q, r } = divisionAlgorithm(a, b)
    result.value = { a, b, q, r }
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
  text-align: center;
  font-size: 1.1rem;
  color: var(--primary-l);
}
.theory-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: .4rem;
}
.theory-list li::before { content: '›  '; color: var(--primary-l); }

.input-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.equation-display {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: .6rem;
  font-family: var(--mono);
  font-size: 1.6rem;
  font-weight: 700;
  flex-wrap: wrap;
  padding: 1rem;
}
.eq-part { font-size: 2rem; }
.eq-op { color: var(--text-dim); }

.legend-row {
  display: flex;
  gap: .75rem;
  flex-wrap: wrap;
  justify-content: center;
}
</style>
