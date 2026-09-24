<template>
  <div class="stat-card" :class="color">
    <div class="sc-icon">{{ icon }}</div>
    <div class="sc-body">
      <div class="sc-label">{{ label }}</div>
      <div class="sc-value mono">{{ value }}</div>
    </div>
  </div>
</template>

<script setup>
defineProps({ icon: String, label: String, value: String, color: String })
</script>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  gap: .75rem;
  background: var(--surface2);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: .75rem 1rem;
}
.sc-icon { font-size: 1.5rem; }
.sc-label { font-size: .7rem; color: var(--text-dim); text-transform: uppercase; letter-spacing: .06em; }
.sc-value { font-size: 1.4rem; font-weight: 700; word-break: break-all; }

.stat-card.green  .sc-value { color: var(--green); }
.stat-card.purple .sc-value { color: var(--primary-l); }
.stat-card.gold   .sc-value { color: var(--accent); }
</style>
