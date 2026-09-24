<template>
  <div class="panel">
    <!-- Panel header -->
    <div class="panel-header">
      <div class="panel-icon">{{ icon }}</div>
      <div>
        <h2 class="panel-title">{{ title }}</h2>
        <p class="panel-sub">{{ subtitle }}</p>
      </div>
    </div>

    <!-- Two-column layout: theory + interactive -->
    <div class="panel-body">
      <!-- Left: theory box -->
      <div class="theory-col">
        <div class="section-label">📖 Theory</div>
        <div class="theory-box card">
          <slot name="theory" />
        </div>
      </div>

      <!-- Right: interactive -->
      <div class="interactive-col">
        <div class="section-label">⚡ Interactive</div>
        <div class="interactive-box card card-glow">
          <div class="inputs-area">
            <slot name="inputs" />
          </div>
          <div class="output-area-wrap" v-if="$slots.output">
            <div class="divider"></div>
            <slot name="output" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({ title: String, subtitle: String, icon: String })
</script>

<style scoped>
.panel { display: flex; flex-direction: column; gap: 1.5rem; }

.panel-header {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
}
.panel-icon { font-size: 2.5rem; line-height: 1; margin-top: .1rem; }
.panel-title { font-size: 1.55rem; font-weight: 800; letter-spacing: -.03em; }
.panel-sub   { color: var(--text-dim); font-size: .88rem; margin-top: .2rem; }

.panel-body {
  display: grid;
  grid-template-columns: 1fr 1.4fr;
  gap: 1.5rem;
  align-items: start;
}

.section-label {
  font-size: .72rem;
  text-transform: uppercase;
  letter-spacing: .1em;
  color: var(--text-dim);
  margin-bottom: .5rem;
}

.theory-box {
  display: flex;
  flex-direction: column;
  gap: .75rem;
  font-size: .9rem;
  line-height: 1.65;
  color: var(--text-dim);
}
.theory-box :deep(strong), .theory-box :deep(code) { color: var(--text); }
.theory-box :deep(code) { font-family: var(--mono); }

.interactive-box {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.inputs-area { display: flex; flex-direction: column; gap: .75rem; }
.divider { height: 1px; background: var(--border); }
.output-area-wrap { display: flex; flex-direction: column; gap: .75rem; }

/* slide-up transition (used by children) */
:deep(.slide-up-enter-active),
:deep(.slide-up-leave-active) { transition: opacity .25s, transform .25s; }
:deep(.slide-up-enter-from),
:deep(.slide-up-leave-to)  { opacity: 0; transform: translateY(8px); }

:deep(.output-area) { display: flex; flex-direction: column; gap: .75rem; }

@media (max-width: 860px) {
  .panel-body { grid-template-columns: 1fr; }
}
</style>
