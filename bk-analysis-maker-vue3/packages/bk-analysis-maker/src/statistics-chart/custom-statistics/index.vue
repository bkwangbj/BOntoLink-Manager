<template>
  <div
    class="statistics-box"
    :style="{gap:(option.styles.gap+'px')||''}"
    style="position: relative;"
  >
    <slot name="content">
      <component
        :is="contentConfig.component"
        v-if="contentConfig && contentConfig.component"
        v-bind="contentConfig.props || {}"
      />
    </slot>
    <div
      v-for="(item,index) in realData"
      :key="index"
      class="statistics-item"
      :style="{...option.styles}"
      @click="itemClick(item)"
    >
      <div
        :style="option.image"
        style="position: absolute;"
      >
        <img
          v-show="item.url&&option.imageShow"

          :src="item.url"
          class="statistics-img"
        >
      </div>
      <div
        :style="getTextStyle(item.number)"
        style="position: absolute;"
      >
        <div
          class="statistics-text"
        >
          {{ item.text }}
          <span
            v-show="option.unitShow&&option.unitPosi==='text'"
            class="statistics-unit"
            :style="option.unit"
          >
            {{ item.unit }}
          </span>
        </div>
      </div>
      <div
        :style="getNumberStyle(item.number)"
        style="position: absolute;"
      >
        <div
          class="statistics-num"
        >
          {{ item.number }}
          <span
            v-show="option.unitShow&&option.unitPosi==='number'"
            class="statistics-unit"
            :style="option.unit"
          >
            {{ item.unit }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    option: {
      type: Object,
      default: () => {
        return {
          image: {},
          number: {},
          text: {},
          styles: {}
        }
      }
    },
    realData: {
      type: Array,
      default: () => []
    }
  },
  emits: ['itemClick'],
  data () {
    return {
      isVertical: false
    }
  },
  methods: {
    itemClick (e) {
      this.$emit('itemClick', e)
    },
    getTextStyle (number) {
      if (this.option.pieces?.length) {
        let color = ''
        this.option.pieces.forEach(item => {
          const num = Number(number)
          const gt = Number(item.gt)
          const lte = Number(item.lte)
          if (num > gt && num <= lte) {
            color = item.textColor
          }
        })
        return { ...this.option.text, color: color || this.option.text?.color }
      } else {
        return this.option.text
      }
    },
    getNumberStyle (number) {
      if (this.option.pieces?.length) {
        let color = ''
        this.option.pieces.forEach(item => {
          const num = Number(number)
          const gt = Number(item.gt)
          const lte = Number(item.lte)
          if (num > gt && num <= lte) {
            color = item.numberColor
          }
        })
        return { ...this.option.number, color: color || this.option.number?.color }
      } else {
        return this.option.number
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.statistics-box {
  display: flex;
  flex-wrap: wrap;
  width: 100%;
  height: 100%;

  .statistics-item {
    position: relative;
    box-sizing: border-box;
    overflow: hidden;
    background-position: center center;
    background-size: 100% 100%;
    border-style: solid;

    .statistics-img {
      position: relative;
      top: var(--y);
      left: var(--x);
      width: var(--width);
      height: var(--height);
    }

    .statistics-text {
      position: relative;
      top: var(--y);
      left: var(--x);
      white-space: nowrap;
    }

    .statistics-num {
      position: relative;
      top: var(--y);
      left: var(--x);
      display: flex;
    }

    .statistics-unit {
      position: relative;
      top: var(--yunit);
      left: var(--xunit);
    }
  }
}

</style>
