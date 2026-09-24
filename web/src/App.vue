<template>
  <div class="app-shell">
    <!-- ── Animated background ── -->
    <canvas ref="bgCanvas" class="bg-canvas" />

    <!-- ── Demo mode overlay ── -->
    <DemoMode ref="demoRef" :active="demoActive" @close="demoActive = false" />

    <!-- ── Header ── -->
    <header class="header">
      <div class="header-inner">
        <div class="logo">
          <span class="logo-icon">🔐</span>
          <div>
            <div class="logo-title">Number Theory &amp; RSA</div>
            <div class="logo-sub">Interactive Explorer</div>
          </div>
        </div>
        <div class="header-badges">
          <span class="tag">BigInt precision</span>
          <span class="tag green">Miller-Rabin primes</span>
          <button class="btn btn-demo" @click="launchDemo">▶ Run Demo</button>
        </div>
      </div>
    </header>

    <!-- ── Tab nav ── -->
    <nav class="tab-nav">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        class="tab-btn"
        :class="{ active: activeTab === tab.id }"
        @click="activeTab = tab.id"
      >
        <span class="tab-icon">{{ tab.icon }}</span>
        <span class="tab-label">{{ tab.label }}</span>
      </button>
    </nav>

    <!-- ── Main content ── -->
    <main class="main">
      <Transition name="fade" mode="out-in">
        <component :is="currentComponent" :key="activeTab" :keys="rsaKeys" @keys-updated="rsaKeys = $event" />
      </Transition>
    </main>

    <!-- ── Footer ── -->
    <footer class="footer">
      <span>Built from the math up &mdash; every step shown</span>
      <span class="dim">·</span>
      <span class="dim">Uses native <code>BigInt</code> for arbitrary precision</span>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, shallowRef, nextTick } from 'vue'
import DivisionAlgorithm from './components/DivisionAlgorithm.vue'
import EuclidGcd from './components/EuclidGcd.vue'
import ExtendedGcd from './components/ExtendedGcd.vue'
import UnitsGroup from './components/UnitsGroup.vue'
import RSAKeyGen from './components/RSAKeyGen.vue'
import EncryptDecrypt from './components/EncryptDecrypt.vue'
import DemoMode from './components/DemoMode.vue'

const tabs = [
  { id: 'division', icon: '➗', label: 'Division Algorithm' },
  { id: 'gcd',      icon: '⚙️',  label: "Euclid's GCD" },
  { id: 'extgcd',   icon: '🔁', label: 'Extended GCD' },
  { id: 'units',    icon: '🔵', label: 'Units Group' },
  { id: 'rsa',      icon: '🔑', label: 'RSA Key Gen' },
  { id: 'crypto',   icon: '🛡️',  label: 'Encrypt / Decrypt' },
]

const componentMap = {
  division: DivisionAlgorithm,
  gcd:      EuclidGcd,
  extgcd:   ExtendedGcd,
  units:    UnitsGroup,
  rsa:      RSAKeyGen,
  crypto:   EncryptDecrypt,
}

const activeTab = ref('division')
const rsaKeys   = ref(null)
const currentComponent = computed(() => componentMap[activeTab.value])

const demoActive = ref(false)
const demoRef    = ref(null)

async function launchDemo() {
  demoActive.value = true
  await nextTick()
  demoRef.value?.start()
}

// ── Particle canvas ──────────────────────────────────────────────────────────
const bgCanvas = ref(null)
let animId = null

onMounted(() => {
  const canvas = bgCanvas.value
  const ctx = canvas.getContext('2d')
  let W, H, particles

  function resize() {
    W = canvas.width  = window.innerWidth
    H = canvas.height = window.innerHeight
  }

  function initParticles() {
    particles = Array.from({ length: 60 }, () => ({
      x: Math.random() * W,
      y: Math.random() * H,
      r: Math.random() * 1.5 + 0.5,
      vx: (Math.random() - .5) * .3,
      vy: (Math.random() - .5) * .3,
      alpha: Math.random() * .5 + .1,
    }))
  }

  function draw() {
    ctx.clearRect(0, 0, W, H)
    for (const p of particles) {
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(124,58,237,${p.alpha})`
      ctx.fill()
      p.x += p.vx; p.y += p.vy
      if (p.x < 0) p.x = W; if (p.x > W) p.x = 0
      if (p.y < 0) p.y = H; if (p.y > H) p.y = 0
    }
    // draw faint connecting lines
    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x
        const dy = particles[i].y - particles[j].y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < 120) {
          ctx.beginPath()
          ctx.moveTo(particles[i].x, particles[i].y)
          ctx.lineTo(particles[j].x, particles[j].y)
          ctx.strokeStyle = `rgba(124,58,237,${(1 - dist / 120) * .12})`
          ctx.lineWidth = .5
          ctx.stroke()
        }
      }
    }
    animId = requestAnimationFrame(draw)
  }

  resize(); initParticles(); draw()
  window.addEventListener('resize', () => { resize(); initParticles() })
})

onUnmounted(() => { if (animId) cancelAnimationFrame(animId) })
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
}

/* animated background */
.bg-canvas {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

/* header */
.header {
  position: relative;
  z-index: 10;
  border-bottom: 1px solid var(--border);
  background: rgba(13,13,26,.85);
  backdrop-filter: blur(12px);
}
.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 1rem 2rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}
.logo { display: flex; align-items: center; gap: .75rem; }
.logo-icon { font-size: 2rem; }
.logo-title { font-size: 1.2rem; font-weight: 800; letter-spacing: -.03em; }
.logo-sub { font-size: .75rem; color: var(--text-dim); }
.header-badges { display: flex; gap: .5rem; flex-wrap: wrap; align-items: center; }

.btn-demo {
  background: linear-gradient(135deg, var(--primary), #9333ea);
  color: #fff;
  border: none;
  border-radius: 9999px;
  padding: .45rem 1.1rem;
  font-size: .82rem;
  font-weight: 700;
  cursor: pointer;
  letter-spacing: .03em;
  box-shadow: 0 0 16px rgba(124,58,237,.5);
  transition: all .2s;
  animation: pulse-demo 2.5s ease-in-out infinite;
}
.btn-demo:hover {
  transform: translateY(-1px) scale(1.04);
  box-shadow: 0 0 24px rgba(124,58,237,.7);
}
@keyframes pulse-demo {
  0%, 100% { box-shadow: 0 0 16px rgba(124,58,237,.5); }
  50%       { box-shadow: 0 0 28px rgba(124,58,237,.85); }
}

/* tab nav */
.tab-nav {
  position: relative;
  z-index: 10;
  display: flex;
  gap: .25rem;
  padding: .75rem 2rem;
  background: rgba(13,13,26,.7);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--border);
  overflow-x: auto;
  scrollbar-width: none;
}
.tab-nav::-webkit-scrollbar { display: none; }
.tab-btn {
  display: flex;
  align-items: center;
  gap: .4rem;
  padding: .5rem 1.1rem;
  border-radius: 9999px;
  font-size: .85rem;
  font-weight: 600;
  cursor: pointer;
  border: 1.5px solid transparent;
  background: transparent;
  color: var(--text-dim);
  white-space: nowrap;
  transition: all .2s;
}
.tab-btn:hover { color: var(--text); border-color: var(--border); }
.tab-btn.active {
  background: rgba(124,58,237,.2);
  color: var(--primary-l);
  border-color: var(--primary);
  box-shadow: 0 0 12px rgba(124,58,237,.25);
}
.tab-icon { font-size: 1rem; }

/* main */
.main {
  flex: 1;
  position: relative;
  z-index: 5;
  max-width: 1100px;
  width: 100%;
  margin: 0 auto;
  padding: 2rem;
}

/* footer */
.footer {
  position: relative;
  z-index: 10;
  border-top: 1px solid var(--border);
  padding: .75rem 2rem;
  display: flex;
  gap: .75rem;
  font-size: .78rem;
  color: var(--text-dim);
  background: rgba(13,13,26,.8);
  justify-content: center;
  flex-wrap: wrap;
}

/* fade transition */
.fade-enter-active,
.fade-leave-active { transition: opacity .2s, transform .2s; }
.fade-enter-from,
.fade-leave-to { opacity: 0; transform: translateY(6px); }
</style>
