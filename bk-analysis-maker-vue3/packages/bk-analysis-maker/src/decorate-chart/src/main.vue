<script lang="jsx">

export default {
  name: 'DecorateChart',
  props: {
    content: {
      type: Function,
      default: () => {}
    }
  },
  setup (props, { slots }) {
    return () => {
      return [<div class="decorate-chart-panel"> {props.content && props.content()}</div>]
    }
  }
}
</script>

<style lang="scss" scoped>

.decorate-chart-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

</style>
