<template>
  <span :class="['mini-sw', checked && 'is-on', disabled && 'is-disabled']" @click.stop="onClick">
    <span class="mini-sw-dot"></span>
  </span>
</template>

<script setup>
const props = defineProps({
  checked: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false }
})
const emit = defineEmits(['change'])
function onClick() { if (!props.disabled) emit('change', !props.checked) }
</script>

<style scoped>
.mini-sw {
  display: inline-block; width: 28px; height: 14px;
  border-radius: 8px; background: #c9cdd4;
  position: relative; cursor: pointer;
  transition: background-color .15s;
  vertical-align: middle;
}
.mini-sw.is-on { background: var(--bl-primary); }
.mini-sw.is-disabled { opacity: .55; cursor: default; }
.mini-sw-dot {
  position: absolute; left: 2px; top: 2px;
  width: 10px; height: 10px; border-radius: 50%;
  background: var(--bl-bg-1); transition: left .15s;
  box-shadow: 0 1px 2px rgba(0,0,0,.2);
}
.mini-sw.is-on .mini-sw-dot { left: 16px; }
</style>
