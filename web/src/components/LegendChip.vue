<template>
  <div class="legend-chip" :class="color">
    <span class="lc-label">{{ label }}</span>
    <span class="lc-desc">{{ desc }}</span>
  </div>
</template>

<script setup>
defineProps({ label: String, desc: String, color: String })
</script>

<style scoped>
.legend-chip {
  display: inline-flex;
  align-items: center;
  gap: .35rem;
  background: var(--surface2);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: .3rem .65rem;
  font-size: .78rem;
}
.lc-label {
  font-family: var(--mono);
  font-weight: 700;
  font-size: .85rem;
}
.lc-desc { color: var(--text-dim); }

.gold   .lc-label { color: var(--accent); }
.purple .lc-label { color: var(--primary-l); }
.green  .lc-label { color: var(--green); }
.accent .lc-label { color: var(--accent-l); }
</style>
