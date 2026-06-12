<template>
  <el-collapse
    v-model="show"
    class=""
  >
    <el-collapse-item
      name="1"
    >
      <template #title>
        <div
          class="series-header"
          style=" display: flex;justify-content: space-between;width: 100%;"
        >
          <div>
            <span class="axis-item-title">
              {{ name }} </span>
            <el-button
              v-show="show.length"
              :disabled="!saveAble"
              size="small"
              type="primary"
              text
              class="add-seris-btn"
              @click.stop="addSeries"
            >
              <el-icon><CirclePlus /></el-icon>
            </el-button>
          </div>
          <div
            v-show="show.length"
            style="margin-right: 5px;"
          >
            <slot name="header-button" />
            <el-button
              text
              class="type-icon"
              :class="{'is-active':layoutType==='horizontal'}"
              style="margin-right: 2px;"
              size="small"
              @click.stop="layoutType='horizontal'"
            >
              <el-icon><List /></el-icon>
            </el-button>
            <el-button
              text
              class="type-icon"
              size="small"
              :class="{'is-active':layoutType==='vertical'}"
              @click.stop="layoutType='vertical'"
            >
              <i-ri-layout-left-fill />
            </el-button>
          </div>
        </div>
      </template>

      <el-collapse v-show="layoutType==='horizontal'">
        <draggable
          :list="seriesList"
          :disabled="!saveAble"
          item-key="basic.dataId"
          @end="draggableEnd"
        >
          <template #item="{element:series}">
            <el-collapse-item>
              <template #title>
                <div
                  class="series-header flex-between"
                  style="width: 100%;"
                >
                  <slot
                    name="title"
                    :index="seriesList.indexOf(series)"
                  />
                  <div>
                    <el-button
                      size="small"
                      :disabled="!saveAble"
                      text
                      class="add-seris-btn"
                      type="primary"
                      @click.stop="copySeries(series)"
                    >
                      <i-ri-file-copy-2-fill />
                    </el-button>
                    <el-button
                      size="small"
                      :disabled="!saveAble"
                      text
                      class="del-seris-btn"
                      @click.stop="delSeries(seriesList.indexOf(series))"
                    >
                      <el-icon><DeleteFilled /></el-icon>
                    </el-button>
                  </div>
                </div>
              </template>
              <slot
                v-if="series"
                name="content"
                :series="series"
                :index="seriesList.indexOf(series)"
              />
            </el-collapse-item>
          </template>
        </draggable>
      </el-collapse>
      <div
        v-show="layoutType==='vertical'"
        style="position: relative;width: 100%;"
      >
        <div class="tabs-series">
          <draggable
            :list="seriesList"
            :disabled="!saveAble"
            item-key="basic.dataId"
            class="tab-header"
            @end="draggableEnd"
          >
            <template #item="{element:series}">
              <div
                :key="seriesList.indexOf(series)"
                class="tab-header-item"
                :class="{'active':seriesList[currentTab]===series}"
                :name="seriesList.indexOf(series).toString()"
                @click="currentTab=seriesList.indexOf(series)"
              >
                <slot
                  name="title"
                  :index="seriesList.indexOf(series)"
                />
              </div>
            </template>
          </draggable>

          <div
            v-if="seriesList[Number(currentTab)]"
            class="tabs-series-content"
          >
            <keep-alive>
              <slot
                name="content"

                :series="seriesList[Number(currentTab)]"
                :index="currentTab"
              />
            </keep-alive>
          </div>
        </div>
        <div class="series-header icon-viewer">
          <el-button
            size="small"
            :disabled="!saveAble"
            text
            class="add-seris-btn"
            @click.stop="copySeries(seriesList[Number(currentTab)])"
          >
            <i-ri-file-copy-2-fill />
          </el-button>
          <el-button
            size="small"
            :disabled="!saveAble"
            text
            class="del-seris-btn"
            @click.stop="delSeries(Number(currentTab))"
          >
            <el-icon><DeleteFilled /></el-icon>
          </el-button>
        </div>
      </div>
    </el-collapse-item>
  </el-collapse>
</template>

<script>
import draggable from 'vuedraggable'
export default {
  name: 'SeriesPlane',
  components: {
    draggable
  },
  inject: ['getSaveAble'],

  props: {
    name: {
      type: String,
      default: ''
    },
    seriesList: {
      type: Array,
      default: () => []
    }
  },
  emits: ['addSeries', 'seriesChange', 'copySeries', 'delSeries'],
  data () {
    return {
      show: ['1'],
      currentTab: 0,
      layoutType: 'horizontal'

    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    },
    dragOptions () {
      return {
        animation: 150,
        disabled: this.saveAble

      }
    }
  },
  methods: {
    addSeries () {
      this.$emit('addSeries')
      this.$nextTick(() => {
        if (this.layoutType === 'vertical') {
          this.currentTab = (this.seriesList.length - 1).toString()
        }
      })
    },
    draggableEnd (e) {
      this.$emit('seriesChange')
    },
    copySeries (series) {
      this.$emit('copySeries', series)
    },
    delSeries (index) {
      if ((index - 1) >= 0) {
        this.currentTab = index - 1
      } else {
        this.currentTab = 0
      }

      this.$emit('delSeries', index)
    }
  }
}
</script>

<style lang="scss" scoped>
/* stylelint-disable-next-line scss/at-import-partial-extension */
@import "../../styles/index.scss";

.flex-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

:deep() {

  .add-seris-btn {
    margin-bottom: 2px;
  }

  .del-seris-btn {
    margin-bottom: 2px;
    margin-left: 2px;
  }

  .el-button  .el-icon {
    font-size: 12px;
  }

  .el-button  svg {
    width: 12px;
    height: 12px;
  }

  .tooltip-text {
    padding: 5px;
    font-size: 12px;
    color: #a3a3a3;
  }

  .popover-click {
    background: transparent;
    border: 0;
  }

  .tabs-series {
    display: flex;
    flex-direction: column;

    .tab-header {
      display: flex;
      align-items: center;
      margin-bottom: 10px;
      border: 0;
      border-bottom: 1px solid #e4e7ed;
      widows: 100%;

      .tab-header-item {
        height: 28px;
        padding: 0 10px;
        font-size: 12px;
        line-height: 28px;
        color: #1a1a1a;
        border: 0;
      }

      .tab-header-item.active {
        margin: 5px;
        color: #1f6aff;
        background: #f0f5ff;
        border: 0;
      }

      .tab-header-item:last-child {
        padding-right: 10px;
      }
    }
  }
}

.el-button.type-icon {
  color: #a3a3a3;
  border: 0;

  &.is-active {
    color: #1f6aff;
    border: 0;
  }
}

.icon-viewer {
  position: absolute;
  top: 0;
  right: 0;
  height: 38px;
  padding-left: 10px;
  margin-bottom: 15px;

  &::before {
    position: absolute;
    top: 10px;
    left: 0;
    display: block;
    width: 1px;
    height: 20px;
    content: "";
    background: #e4e7ed;
  }
}
</style>
