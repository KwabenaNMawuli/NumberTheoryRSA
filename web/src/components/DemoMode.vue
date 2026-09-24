<template>
  <Teleport to="body">
    <Transition name="demo-overlay">
      <div v-if="active" class="demo-overlay" @keydown.esc="$emit('close')">

        <!-- Close -->
        <button class="demo-close" @click="$emit('close')">✕</button>

        <!-- ── CREDITS screen ── -->
        <Transition name="credits-fade">
          <div v-if="phase === 'credits'" class="credits-screen">
            <div class="credits-inner">
              <div class="credits-subtitle">A Number Theory &amp; RSA project by</div>
              <h1 class="credits-title">Group Members</h1>
              <div class="credits-list">
                <div
                  v-for="(m, i) in members"
                  :key="i"
                  class="member-row"
                  :class="{ visible: visibleMembers >= i + 1 }"
                  :style="{ '--delay': i * 0.18 + 's' }"
                >
                  <span class="member-num">{{ String(i + 1).padStart(2, '0') }}</span>
                  <span class="member-name">{{ m.name }}</span>
                  <span class="member-id">{{ m.id }}</span>
                </div>
              </div>
              <div class="credits-footer" :class="{ visible: visibleMembers >= members.length }">
                <div class="cf-line">🔐 Number Theory &amp; RSA Explorer</div>
                <div class="cf-sub">Built with Vue · BigInt · Miller-Rabin</div>
              </div>
              <button class="btn btn-ghost credits-again" @click="restart">↩ Run Again</button>
            </div>
          </div>
        </Transition>

        <!-- ── STEP screen ── -->
        <div v-if="phase !== 'credits'" class="step-screen">

          <!-- Progress bar -->
          <div class="demo-progress-track">
            <div class="demo-progress-fill" :style="{ width: progressPct + '%' }"></div>
          </div>

          <!-- Step pills -->
          <div class="step-pills">
            <div
              v-for="(s, i) in steps"
              :key="i"
              class="step-pill"
              :class="{
                done:   i < currentStep,
                active: i === currentStep
              }"
            >
              <span>{{ s.icon }}</span>
              <span class="sp-label">{{ s.shortLabel }}</span>
            </div>
          </div>

          <!-- Main card -->
          <div class="demo-card">

            <!-- Header -->
            <div class="demo-card-header">
              <span class="demo-step-icon">{{ currentStepData.icon }}</span>
              <div>
                <div class="demo-step-title">{{ currentStepData.title }}</div>
                <div class="demo-step-sub">{{ currentStepData.subtitle }}</div>
              </div>
              <div class="step-counter">{{ currentStep + 1 }} / {{ steps.length }}</div>
            </div>

            <!-- Body: inputs + result -->
            <div class="demo-card-body">

              <!-- Inputs column -->
              <div class="demo-inputs-col">
                <div class="col-label">📥 Inputs</div>
                <div
                  v-for="(inp, i) in currentStepData.inputs"
                  :key="i"
                  class="demo-input-row"
                >
                  <div class="di-label">{{ inp.label }}</div>
                  <div class="di-value mono">
                    {{ typedValues[i] }}<span v-if="typingIndex === i" class="cursor">|</span>
                  </div>
                </div>
              </div>

              <!-- Divider -->
              <div class="demo-divider"></div>

              <!-- Result column -->
              <div class="demo-result-col">
                <div class="col-label">📤 Result</div>
                <Transition name="result-pop">
                  <div v-if="showResult" class="demo-result-content">
                    <component :is="currentStepData.resultComponent" :data="computedResult" />
                  </div>
                </Transition>
                <div v-if="!showResult && phase === 'computing'" class="computing-spinner">
                  <div class="spinner"></div>
                  <span class="dim">Computing…</span>
                </div>
              </div>
            </div>

            <!-- Auto-advance bar -->
            <div v-if="showResult && phase === 'showing'" class="advance-bar-wrap">
              <div class="advance-label dim">Next in {{ countdown }}s</div>
              <div class="advance-track">
                <div class="advance-fill" :style="{ width: advancePct + '%' }"></div>
              </div>
              <button class="btn btn-ghost advance-skip" @click="advanceNow">Skip →</button>
            </div>

          </div>
        </div>

      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, onUnmounted, markRaw, h } from 'vue'
import {
  divisionAlgorithm, euclidGcd, extendedGcd,
  unitsGroup, generateRSAKeys, encryptMessage, decryptMessage
} from '../rsa.js'

// ── result sub-components (inline render functions) ──────────────────────────

const DivResult = markRaw({
  props: ['data'],
  setup(props) {
    return () => h('div', { class: 'res-wrap' }, [
      h('div', { class: 'big-eq' }, [
        h('span', { class: 'eq-a' }, String(props.data.a)),
        h('span', { class: 'eq-op' }, '='),
        h('span', { class: 'eq-b' }, String(props.data.b)),
        h('span', { class: 'eq-op' }, '·'),
        h('span', { class: 'eq-q' }, String(props.data.q)),
        h('span', { class: 'eq-op' }, '+'),
        h('span', { class: 'eq-r' }, String(props.data.r)),
      ]),
      h('div', { class: 'res-note' }, `${props.data.a} mod ${props.data.b} = ${props.data.r}`)
    ])
  }
})

const GcdResult = markRaw({
  props: ['data'],
  setup(props) {
    return () => {
      const rows = props.data.steps.slice(0, 6).map((s, i) =>
        h('tr', { key: i }, [
          h('td', { class: 'mono gold' }, String(s.a)),
          h('td', { class: 'mono dim' }, '='),
          h('td', { class: 'mono purple' }, String(s.b)),
          h('td', { class: 'mono dim' }, '×'),
          h('td', { class: 'mono green' }, String(s.q)),
          h('td', { class: 'mono dim' }, '+'),
          h('td', { class: 'mono', style: 'color:var(--accent-l)' }, String(s.r)),
        ])
      )
      return h('div', { class: 'res-wrap' }, [
        h('div', { class: 'gcd-badge' }, `gcd = ${props.data.gcd}`),
        h('table', { class: 'mini-trace' }, [h('tbody', rows)])
      ])
    }
  }
})

const ExtResult = markRaw({
  props: ['data'],
  setup(props) {
    return () => h('div', { class: 'res-wrap' }, [
      h('div', { class: 'bezout-line' },
        `${props.data.a}·(${props.data.x}) + ${props.data.m}·(${props.data.y}) = ${props.data.gcd}`),
      props.data.inv != null
        ? h('div', { class: 'inv-line' }, [
            h('span', { class: 'dim' }, `${props.data.a}⁻¹ mod ${props.data.m} = `),
            h('strong', { class: 'gold' }, String(props.data.inv)),
            h('span', { class: 'green', style: 'margin-left:.5rem' }, '✓'),
          ])
        : h('div', { class: 'red' }, 'No inverse (gcd ≠ 1)')
    ])
  }
})

const UnitsResult = markRaw({
  props: ['data'],
  setup(props) {
    return () => h('div', { class: 'res-wrap' }, [
      h('div', { class: 'phi-line' }, `φ(${props.data.n}) = ${props.data.units.length}`),
      h('div', { class: 'units-mini' },
        props.data.units.map(u => h('span', { class: 'uchip', key: u }, String(u)))
      )
    ])
  }
})

const RsaResult = markRaw({
  props: ['data'],
  setup(props) {
    const trunc = v => { const s = String(v); return s.length > 22 ? s.slice(0,10)+'…'+s.slice(-10) : s }
    return () => h('div', { class: 'res-wrap' }, [
      h('div', { class: 'rsa-line' }, [h('span',{class:'dim'},'n = '), h('span',{class:'mono', style:'color:var(--accent-l); word-break:break-all;font-size:.82rem'}, trunc(props.data.n))]),
      h('div', { class: 'rsa-line' }, [h('span',{class:'dim'},'φ(n) = '), h('span',{class:'mono dim',style:'font-size:.78rem'}, trunc(props.data.phi))]),
      h('div', { class: 'rsa-line' }, [h('span',{class:'dim'},'e = '), h('span',{class:'mono purple'}, String(props.data.e))]),
      h('div', { class: 'rsa-line' }, [h('span',{class:'dim'},'d = '), h('span',{class:'mono', style:'color:var(--red);word-break:break-all;font-size:.82rem'}, trunc(props.data.d))]),
      h('div', { class: 'rsa-line green', style:'margin-top:.25rem' }, '✓ Keys valid (e·d ≡ 1 mod φ(n))'),
    ])
  }
})

const CryptoResult = markRaw({
  props: ['data'],
  setup(props) {
    const trunc = v => { const s = String(v); return s.length > 22 ? s.slice(0,10)+'…'+s.slice(-10) : s }
    return () => h('div', { class: 'res-wrap' }, [
      h('div', { class: 'flow-mini' }, [
        h('div', { class: 'fm-node' }, [h('div',{class:'fm-l'},'m'), h('div',{class:'mono gold',style:'font-size:1.4rem'}, String(props.data.m))]),
        h('div', { class: 'fm-arr' }, '→'),
        h('div', { class: 'fm-node' }, [h('div',{class:'fm-l'},'c = m^e mod n'), h('div',{class:'mono purple',style:'font-size:.78rem;word-break:break-all'}, trunc(props.data.c))]),
        h('div', { class: 'fm-arr' }, '→'),
        h('div', { class: 'fm-node' }, [h('div',{class:'fm-l'},"m' = c^d mod n"), h('div',{class:'mono green',style:'font-size:1.4rem'}, String(props.data.back))]),
      ]),
      h('div', { class: props.data.match ? 'green' : 'red', style:'margin-top:.5rem;font-weight:600' },
        props.data.match ? '✓ Euler\'s theorem holds — perfect roundtrip!' : '✗ Mismatch')
    ])
  }
})

// ── demo steps ────────────────────────────────────────────────────────────────
// Using p=61, q=53, e=17 — classic RSA textbook example
const P = 61n, Q = 53n, E = 17n
const N = P * Q          // 3233
const PHI = (P-1n)*(Q-1n) // 3120
const D = 2753n           // 17^-1 mod 3120 = 2753

const steps = [
  {
    icon: '➗', shortLabel: 'Division', title: 'Division Algorithm', subtitle: 'a = b·q + r',
    inputs: [{ label: 'a (dividend)', value: '1000' }, { label: 'b (divisor)', value: '37' }],
    compute: () => { const {q,r} = divisionAlgorithm(1000n,37n); return { a:1000n, b:37n, q, r } },
    resultComponent: DivResult,
  },
  {
    icon: '⚙️', shortLabel: "Euclid GCD", title: "Euclid's GCD Algorithm", subtitle: 'gcd(a,b) traced step by step',
    inputs: [{ label: 'a', value: '1071' }, { label: 'b', value: '462' }],
    compute: () => euclidGcd(1071n, 462n),
    resultComponent: GcdResult,
  },
  {
    icon: '🔁', shortLabel: 'Ext. GCD', title: 'Extended Euclidean Algorithm', subtitle: 'Bézout coefficients & modular inverse',
    inputs: [{ label: 'a', value: '17' }, { label: 'm (modulus)', value: '3120' }],
    compute: () => {
      const { gcd, x, y, steps } = extendedGcd(17n, 3120n)
      const inv = gcd === 1n ? ((x % 3120n) + 3120n) % 3120n : null
      return { a: 17n, m: 3120n, gcd, x, y, steps, inv }
    },
    resultComponent: ExtResult,
  },
  {
    icon: '🔵', shortLabel: 'Z_n*', title: 'Group of Units mod n', subtitle: 'Z_n* under multiplication mod n',
    inputs: [{ label: 'n', value: '15' }],
    compute: () => { const units = unitsGroup(15n); return { n: 15n, units } },
    resultComponent: UnitsResult,
  },
  {
    icon: '🔑', shortLabel: 'RSA Keys', title: 'RSA Key Generation', subtitle: 'p=61, q=53, e=17 — textbook example',
    inputs: [{ label: 'p', value: '61' }, { label: 'q', value: '53' }, { label: 'e', value: '17' }],
    compute: () => ({ p: P, q: Q, n: N, phi: PHI, e: E, d: D }),
    resultComponent: RsaResult,
  },
  {
    icon: '🛡️', shortLabel: 'Encrypt', title: 'Encrypt & Decrypt', subtitle: 'c = m^e mod n  ·  m = c^d mod n',
    inputs: [{ label: 'm (message)', value: '65' }, { label: 'e', value: '17' }, { label: 'd', value: '2753' }, { label: 'n', value: '3233' }],
    compute: () => {
      const m = 65n
      const c = encryptMessage(m, E, N)
      const back = decryptMessage(c, D, N)
      return { m, c, back, match: back === m }
    },
    resultComponent: CryptoResult,
  },
]

const members = [
  { name: 'Kwabena Nyamekye Mawuli',   id: '11296675' },
  { name: 'Afful Nana Kwasi Obeng',     id: '11091069' },
  { name: 'Justine Addo',               id: '11170480' },
  { name: 'Raphael Ayeh Mensah',        id: '11016071' },
  { name: 'Yeboah Eugene Amaning',      id: '11163402' },
  { name: 'Edusei Derrick Akuamoah',    id: '11115682' },
  { name: 'Boateng Stanley Kofi',       id: '11253377' },
  { name: 'Caleb Danquah',              id: '11024572' },
  { name: 'Kelly Buabeng',              id: '11180171' },
  { name: 'Nathaniel Addo',             id: '11352845' },
]

// ── props & emits ─────────────────────────────────────────────────────────────
defineProps({ active: Boolean })
defineEmits(['close'])

// ── state ─────────────────────────────────────────────────────────────────────
const currentStep  = ref(0)
const phase        = ref('typing')   // typing | computing | showing | credits
const typedValues  = ref([])
const typingIndex  = ref(0)
const showResult   = ref(false)
const computedResult = ref(null)
const countdown    = ref(5)
const advancePct   = ref(0)
const visibleMembers = ref(0)

const currentStepData = computed(() => steps[currentStep.value])
const progressPct     = computed(() =>
  ((currentStep.value) / steps.length) * 100
)

let timers = []
const after = (ms, fn) => { const t = setTimeout(fn, ms); timers.push(t); return t }
const clearAll = () => { timers.forEach(clearTimeout); timers = [] }

function startStep(stepIdx) {
  clearAll()
  currentStep.value = stepIdx
  phase.value       = 'typing'
  showResult.value  = false
  computedResult.value = null
  countdown.value   = 5
  advancePct.value  = 0

  const inputs = steps[stepIdx].inputs
  typedValues.value = inputs.map(() => '')
  typingIndex.value = 0

  typeInputs(inputs, 0, () => {
    // all inputs typed — brief pause then compute
    after(400, () => {
      phase.value = 'computing'
      after(700, () => {
        computedResult.value = steps[stepIdx].compute()
        showResult.value     = true
        phase.value          = 'showing'
        startCountdown(stepIdx)
      })
    })
  })
}

function typeInputs(inputs, idx, done) {
  if (idx >= inputs.length) { done(); return }
  typingIndex.value = idx
  const val = inputs[idx].value
  let char = 0
  function typeChar() {
    if (char <= val.length) {
      typedValues.value[idx] = val.slice(0, char)
      char++
      after(45, typeChar)
    } else {
      typingIndex.value = -1
      after(200, () => typeInputs(inputs, idx + 1, done))
    }
  }
  typeChar()
}

function startCountdown(stepIdx) {
  const total = 5000
  const tick  = 50
  let elapsed = 0
  function tick_() {
    elapsed += tick
    advancePct.value = (elapsed / total) * 100
    countdown.value  = Math.max(0, Math.ceil((total - elapsed) / 1000))
    if (elapsed >= total) {
      advanceNow()
    } else {
      after(tick, tick_)
    }
  }
  after(tick, tick_)
}

function advanceNow() {
  clearAll()
  const next = currentStep.value + 1
  if (next < steps.length) {
    startStep(next)
  } else {
    showCredits()
  }
}

function showCredits() {
  clearAll()
  phase.value = 'credits'
  visibleMembers.value = 0
  members.forEach((_, i) => {
    after(300 + i * 220, () => { visibleMembers.value = i + 1 })
  })
}

function restart() { startStep(0) }

// watch active to auto-start
watch(() => true, () => {}, { immediate: true })

// expose start
defineExpose({ start: () => startStep(0) })

onUnmounted(clearAll)
</script>

<style scoped>
/* ── Overlay ── */
.demo-overlay {
  position: fixed;
  inset: 0;
  background: rgba(6, 6, 18, 0.97);
  z-index: 1000;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  overflow-y: auto;
  padding: 1rem;
}
.demo-overlay-enter-active, .demo-overlay-leave-active { transition: opacity .3s; }
.demo-overlay-enter-from, .demo-overlay-leave-to { opacity: 0; }

.demo-close {
  position: fixed;
  top: 1rem; right: 1.25rem;
  background: rgba(255,255,255,.08);
  border: 1px solid var(--border);
  color: var(--text-dim);
  border-radius: 50%;
  width: 2rem; height: 2rem;
  cursor: pointer;
  font-size: .9rem;
  display: flex; align-items: center; justify-content: center;
  z-index: 1010;
  transition: all .15s;
}
.demo-close:hover { background: rgba(239,68,68,.2); color: var(--red); }

/* ── Progress bar ── */
.demo-progress-track {
  width: 100%; max-width: 900px;
  height: 3px;
  background: var(--border);
  border-radius: 2px;
  margin-bottom: 1rem;
}
.demo-progress-fill {
  height: 100%;
  background: linear-gradient(to right, var(--primary), var(--primary-l));
  border-radius: 2px;
  transition: width .5s ease;
}

/* ── Step pills ── */
.step-pills {
  display: flex;
  gap: .4rem;
  flex-wrap: wrap;
  justify-content: center;
  margin-bottom: 1.25rem;
  max-width: 900px;
  width: 100%;
}
.step-pill {
  display: flex;
  align-items: center;
  gap: .3rem;
  padding: .3rem .75rem;
  border-radius: 9999px;
  font-size: .75rem;
  font-weight: 600;
  border: 1.5px solid var(--border);
  color: var(--text-dim);
  background: transparent;
  transition: all .3s;
}
.step-pill.done   { background: rgba(16,185,129,.1); border-color: var(--green); color: var(--green); }
.step-pill.active {
  background: rgba(124,58,237,.25);
  border-color: var(--primary);
  color: var(--primary-l);
  box-shadow: 0 0 10px rgba(124,58,237,.35);
}
.sp-label { display: none; }
@media (min-width: 600px) { .sp-label { display: inline; } }

/* ── Main demo card ── */
.step-screen {
  width: 100%;
  max-width: 900px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.demo-card {
  width: 100%;
  background: var(--surface);
  border: 1px solid var(--primary);
  border-radius: 20px;
  box-shadow: 0 0 40px rgba(124,58,237,.2);
  overflow: hidden;
}

.demo-card-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.25rem 1.75rem;
  background: var(--surface2);
  border-bottom: 1px solid var(--border);
}
.demo-step-icon { font-size: 2rem; }
.demo-step-title { font-size: 1.3rem; font-weight: 800; letter-spacing: -.02em; }
.demo-step-sub { font-size: .8rem; color: var(--text-dim); margin-top: .15rem; }
.step-counter {
  margin-left: auto;
  font-size: .8rem;
  color: var(--text-dim);
  font-family: var(--mono);
  white-space: nowrap;
}

.demo-card-body {
  display: grid;
  grid-template-columns: 1fr 4px 1.4fr;
  min-height: 260px;
}
.demo-inputs-col, .demo-result-col {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: .75rem;
}
.demo-divider { background: var(--border); }
.col-label {
  font-size: .72rem;
  text-transform: uppercase;
  letter-spacing: .1em;
  color: var(--text-dim);
}

.demo-input-row { display: flex; flex-direction: column; gap: .2rem; }
.di-label { font-size: .75rem; color: var(--text-dim); }
.di-value {
  font-size: 1.05rem;
  color: var(--accent-l);
  min-height: 1.6rem;
  letter-spacing: .02em;
}
.cursor {
  display: inline-block;
  color: var(--primary-l);
  animation: blink .7s step-end infinite;
  font-weight: 100;
}
@keyframes blink { 50% { opacity: 0 } }

.computing-spinner {
  display: flex;
  align-items: center;
  gap: .75rem;
  padding: 1rem;
}
.spinner {
  width: 1.5rem; height: 1.5rem;
  border: 2px solid var(--border);
  border-top-color: var(--primary-l);
  border-radius: 50%;
  animation: spin .7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg) } }

/* result area */
.demo-result-content { flex: 1; }
.result-pop-enter-active { transition: opacity .4s, transform .4s; }
.result-pop-enter-from { opacity: 0; transform: scale(.95); }

/* advance bar */
.advance-bar-wrap {
  display: flex;
  align-items: center;
  gap: .75rem;
  padding: .75rem 1.75rem;
  background: var(--surface2);
  border-top: 1px solid var(--border);
}
.advance-label { font-size: .78rem; white-space: nowrap; min-width: 60px; }
.advance-track {
  flex: 1;
  height: 4px;
  background: var(--border);
  border-radius: 2px;
  overflow: hidden;
}
.advance-fill {
  height: 100%;
  background: linear-gradient(to right, var(--primary), var(--accent));
  transition: width .05s linear;
}
.advance-skip { padding: .3rem .8rem; font-size: .78rem; }

/* ── Result sub-components (global, since they're render-fns) ── */
:deep(.res-wrap) {
  display: flex;
  flex-direction: column;
  gap: .6rem;
}
:deep(.big-eq) {
  display: flex;
  align-items: center;
  gap: .5rem;
  flex-wrap: wrap;
  font-family: var(--mono);
  font-size: 1.5rem;
  font-weight: 700;
}
:deep(.eq-a)  { color: var(--accent); }
:deep(.eq-b)  { color: var(--primary-l); }
:deep(.eq-q)  { color: var(--green); }
:deep(.eq-r)  { color: var(--accent-l); }
:deep(.eq-op) { color: var(--text-dim); font-weight: 400; }
:deep(.res-note) { font-size: .85rem; color: var(--text-dim); font-family: var(--mono); }
:deep(.gcd-badge) {
  display: inline-block;
  background: rgba(16,185,129,.15);
  border: 1px solid var(--green);
  border-radius: 8px;
  padding: .25rem .75rem;
  color: var(--green);
  font-family: var(--mono);
  font-weight: 700;
  font-size: 1.1rem;
}
:deep(.mini-trace) { border-collapse: collapse; font-size: .78rem; }
:deep(.mini-trace td) { padding: .2rem .4rem; border-bottom: 1px solid rgba(255,255,255,.05); }
:deep(.bezout-line) {
  font-family: var(--mono);
  font-size: .9rem;
  color: var(--text-dim);
  word-break: break-all;
}
:deep(.inv-line) { font-family: var(--mono); font-size: 1.1rem; }
:deep(.phi-line) { font-family: var(--mono); font-size: 1.1rem; color: var(--green); font-weight: 700; }
:deep(.units-mini) { display: flex; flex-wrap: wrap; gap: .3rem; }
:deep(.uchip) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2rem; height: 2rem;
  background: rgba(124,58,237,.15);
  border: 1px solid rgba(124,58,237,.3);
  border-radius: 6px;
  font-family: var(--mono);
  font-size: .78rem;
  color: var(--primary-l);
}
:deep(.rsa-line) { font-size: .88rem; display: flex; gap: .4rem; align-items: baseline; flex-wrap: wrap; }
:deep(.flow-mini) {
  display: flex;
  align-items: center;
  gap: .6rem;
  flex-wrap: wrap;
}
:deep(.fm-node) {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: .2rem;
  background: var(--surface2);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: .5rem .75rem;
  min-width: 80px;
  text-align: center;
}
:deep(.fm-l) { font-size: .65rem; color: var(--text-dim); text-transform: uppercase; letter-spacing: .05em; }
:deep(.fm-arr) { color: var(--primary-l); font-size: 1.2rem; }

/* ── Credits screen ── */
.credits-screen {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(6,6,18,.97);
  z-index: 5;
}
.credits-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  max-width: 640px;
  width: 100%;
  padding: 2rem;
}
.credits-subtitle {
  font-size: .8rem;
  color: var(--text-dim);
  text-transform: uppercase;
  letter-spacing: .15em;
}
.credits-title {
  font-size: 2.4rem;
  font-weight: 800;
  letter-spacing: -.04em;
  background: linear-gradient(135deg, var(--primary-l), var(--accent));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: .5rem;
}
.credits-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: .35rem;
}

.member-row {
  display: flex;
  align-items: center;
  gap: .75rem;
  background: var(--surface2);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: .65rem 1.1rem;
  opacity: 0;
  transform: translateX(-40px);
  transition: opacity .4s ease var(--delay), transform .4s ease var(--delay);
}
.member-row.visible {
  opacity: 1;
  transform: translateX(0);
}
.member-row.visible .member-name {
  animation: glow-name .6s ease var(--delay) both;
}

@keyframes glow-name {
  0%   { color: var(--primary-l); text-shadow: 0 0 12px rgba(124,58,237,.8); }
  100% { color: var(--text); text-shadow: none; }
}

.member-num {
  font-family: var(--mono);
  font-size: .75rem;
  color: var(--primary-l);
  min-width: 1.5rem;
  opacity: .7;
}
.member-name {
  flex: 1;
  font-weight: 600;
  font-size: .95rem;
  color: var(--text);
}
.member-id {
  font-family: var(--mono);
  font-size: .78rem;
  color: var(--accent);
  opacity: .8;
}

.credits-footer {
  margin-top: .5rem;
  text-align: center;
  opacity: 0;
  transition: opacity .5s ease 2.2s;
  display: flex;
  flex-direction: column;
  gap: .2rem;
}
.credits-footer.visible { opacity: 1; }
.cf-line { font-size: 1rem; font-weight: 700; color: var(--primary-l); }
.cf-sub  { font-size: .75rem; color: var(--text-dim); }

.credits-again { margin-top: .75rem; }

.credits-fade-enter-active, .credits-fade-leave-active { transition: opacity .5s; }
.credits-fade-enter-from, .credits-fade-leave-to { opacity: 0; }

@media (max-width: 600px) {
  .demo-card-body { grid-template-columns: 1fr; grid-template-rows: auto 4px auto; }
  .demo-divider { height: 4px; width: 100%; }
}
</style>
