<template>
  <div style="width: 100%;height: 100%;">
    <AnalysisMaker
      :oper-permission="['*.*']"
      :chart-menu-list="chartList"
      :is-basic-mode="false"
      :layout-tools="['addItem', 'addTabLayout']"
    />
  </div>
</template>

<script setup>
import { chartList, config } from './config'

</script>
