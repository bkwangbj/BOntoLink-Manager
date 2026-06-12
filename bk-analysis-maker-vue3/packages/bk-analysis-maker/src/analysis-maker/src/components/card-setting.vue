<template>
  <div class="card-setting-wrapper">
    <div class="title">
      <span style="margin-right: 10px;">卡片设置</span>
      <el-button
        text
        size="small"
        class="am-ps-btn"
        title="复制"
        @click="copyThemeConfig"
      >
        <el-icon><CopyDocument /></el-icon>
      </el-button>
      <el-button
        v-if="!disabled"
        text
        size="small"
        class="am-ps-btn"
        title="粘贴"
        @click="pasteThemeConfig"
      >
        <el-icon><FolderChecked /></el-icon>
      </el-button>
    </div>
    <el-collapse
      v-model="activeNames"
      class="card-setting-container"
    >
      <el-collapse-item
        :disabled="!cardItem.cardStyle.isCustom"
        name="1"
      >
        <template #title>
          <div class="d-flex ai-c">
            <el-switch
              v-model="cardItem.cardStyle.isCustom"
              class="am-switch"
              :disabled="disabled"
              size="small"
              @change="changStyle"
            />
            <span>卡片样式</span>
          </div>
        </template>
        <div class="collapse-item">
          <EfForm
            ref="cardForm"
            :items="cardItems"
            :has-submit="false"
            :disabled="disabled ? disabled : !cardItem.cardStyle.isCustom"
            :has-reset="false"
            size="small"
            label-width="90px"
          />
        </div>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<script lang="jsx">
import { cardRadiusList } from '../configs'
import LineStyle from '../../../chart-common-config/components/style-config/line-style.vue'
import BackgroundStyle from '../../../chart-common-config/components/style-config/background-style.vue'
import { utils } from 'efficient-suite'
import { ElMessage } from 'element-plus'
export default {
  name: 'CardSetting',
  components: {
    // eslint-disable-next-line
    LineStyle,
    // eslint-disable-next-line
    BackgroundStyle
  },
  props: {
    theme: {
      type: Object,
      default: () => ({})
    },
    disabled: {
      type: Boolean,
      default: false
    },
    cardItem: {
      type: Object,
      default: () => ({})
    }
  },
  emits: ['change'],
  data () {
    return {
      activeNames: ['1'],
      cardRadiusList,
      switchMap: {},
      cardItems: [],
      cardBackgroundColorDisabled: false,
      preThemeData: {},
      cardBackgroundColor: '#fff',
      cardBackgroundStyleBox: null,
      cardBorderBox: null,
      cardShadowBox: null,
      cardRadiusDisabled: false,
      cardRadiusValue: 'none'
    }
  },
  watch: {
    cardItem: {
      handler () {
        this.init(this.theme)
      }
    }
  },
  created () {
  },
  mounted () {
    this.init(this.theme)
  },
  methods: {
    async init (theme) {
      if (this.cardItem.cardStyle && !this.cardItem.cardStyle.isCustom) {
        this.activeNames = []
      } else {
        this.activeNames = ['1']
      }
      this.preThemeData = theme
      this.initItems()
      await this.$nextTick()
      let style = {
        cardBackground: theme?.globalCss?.cardBackground || {},
        cardStyle: theme?.globalCss?.cardStyle || {},
        cardShadow: theme?.globalCss?.cardShadow || {},
        cardRadius: theme.globalCss.cardRadius || 'none',
        cardBackgroundColor: theme.globalCss.cardBackgroundColor || '#fff'
      }
      if (this.cardItem.cardStyle?.isCustom && this.cardItem.cardStyle?.config) {
        style = { ...utils.deepClone(style), ...utils.deepClone(this.cardItem.cardStyle?.config) }
      }
      this.cardBackgroundStyleBox.setFormData(style.cardBackground)
      this.cardBorderBox.setFormData(style.cardStyle)
      this.cardShadowBox.setFormData(style.cardShadow)
      // this.$refs.cardForm.setFormData({
      //   cardRadius: style.cardRadius
      // })
      this.cardRadiusValue = style.cardRadius
      this.cardBackgroundColor = style.cardBackgroundColor
    },
    changStyle () {
      this.init(this.theme)
      this.$emit('change')
    },
    initItems () {
      const items4 = [
        {
          label: '卡片颜色',
          field: 'cardBackgroundColor',
          itemSlots: {
            default: () => {
              return [
              <CommonColorPicker
                editAble={!this.cardBackgroundColorDisabled}
                onChange={(value) => this.publicChangeMethod('cardBackgroundColor', value)}
                vModel={this.cardBackgroundColor}
              />
              ]
            }
          }
        },
        {
          label: '卡片图片',
          field: 'cardBackground',
          itemSlots: {
            default: () => {
              return [
              <BackgroundStyle style="width: 100%;" ref={el => { this.cardBackgroundStyleBox = el }} editAble={this.switchMap.cardBackground} onBackgroundStyleChange={(event) => this.publicChangeMethod('cardBackground', event)}></BackgroundStyle>
              ]
            }
          }
        },
        {
          label: '卡片边框',
          field: 'cardStyle',
          itemSlots: {
            default: () => {
              return [
              <LineStyle style="width: 100%;" ref={el => { this.cardBorderBox = el }} editAble={this.switchMap.cardStyle} onLineStyleChange={(event) => this.publicChangeMethod('cardStyle', event)}></LineStyle>
              ]
            }
          }
        },
        {
          label: '卡片阴影',
          field: 'cardShadow',
          itemSlots: {
            default: () => {
              return [
              <ShadowStyle style="width: 100%;" ref={el => { this.cardShadowBox = el }} editAble={this.switchMap.cardShadow} onShadowStyleChange={(event) => this.publicChangeMethod('cardShadow', event)}></ShadowStyle>
              ]
            }
          }
        },
        {
          label: '卡片圆角',
          field: 'cardRadius',
          itemClass: 'card-radius-item',
          itemSlots: {
            default: () => {
              return [
              <EfRadio
                mock={this.cardRadiusList}
                isRadioButton={true}
                size="small"
                disabled={this.cardRadiusDisabled}
                onChange={(event) => { this.publicChangeMethod('cardRadius', event) }}
                vModel={this.cardRadiusValue}
              />
              ]
            }
          }
          // props: {
          //   mock: this.cardRadiusList,
          //   isRadioButton: true,
          //   size: 'small',
          //   onChange: (event) => {
          //     this.publicChangeMethod('cardRadius', event)
          //   }
          // }
        }
      ]

      const map = {}
      items4.forEach(item => {
        if (this.cardItem?.cardStyle.isCustom && this.cardItem?.cardStyle?.config && this.cardItem.cardStyle.config[item.field] !== undefined) {
          map[item.field] = !this.compareEqual(this.cardItem.cardStyle.config[item.field], this.preThemeData.globalCss[item.field])
        } else {
          map[item.field] = false
        }
        if (item.props) {
          item.props.disabled = !map[item.field]
        } else {
          if (item.field === 'cardBackgroundColor') {
            this.cardBackgroundColorDisabled = !map[item.field]
          }
          if (item.field === 'cardRadius') {
            this.cardRadiusDisabled = !map[item.field]
          }
        }
      })
      this.switchMap = map
      this.cardItems = this.formmaterLabelItems(items4)
    },
    compareEqual (obj1, obj2) {
      // 如果不是对象或类型不一致，直接返回false
      if (typeof obj1 !== 'object' || typeof obj2 !== 'object' || obj1 === null || obj2 === null) {
        return obj1 === obj2
      }
      // 获取对象的属性名
      const keys1 = Object.keys(obj1)
      const keys2 = Object.keys(obj2)
      // 如果属性数量不相等，直接返回false
      if (keys1.length !== keys2.length) {
        return false
      }
      // 遍历第一个对象的所有属性
      for (const key of keys1) {
        // 如果第二个对象没有相同的属性，或者属性值不相等，返回false
        if (!Object.prototype.hasOwnProperty.call(obj2, key) || !this.compareEqual(obj1[key], obj2[key])) {
          return false
        }
      }
      // 如果所有属性都检查过，没有发现不匹配的情况，返回true
      return true
    },
    formmaterLabelItems (items) {
      for (const item of items) {
        if (!item.itemSlots) {
          item.itemSlots = {}
        }
        item.itemSlots.label = () => {
          return [
            <div class="form-label">
              <el-switch class="am-switch" size="small" vModel={this.switchMap[item.field]} onChange={(value) => this.changeSwitchValue(item, value)}></el-switch>
              <span>{item.label}</span>
            </div>
          ]
        }
      }
      return items
    },
    publicChangeMethod (key, value) {
      if (!this.cardItem.cardStyle) {
        this.cardItem.cardStyle = {}
      }
      if (!this.cardItem.cardStyle.config) {
        this.cardItem.cardStyle.config = {}
      }
      this.cardItem.cardStyle.config[key] = value
      this.$emit('change')
    },
    changeSwitchValue (item, value) {
      item.props.disabled = !value
      if (item.field === 'cardBackgroundColor') {
        this.cardBackgroundColorDisabled = !value
      }
      if (item.field === 'cardRadius') {
        this.cardRadiusDisabled = !value
      }
      if (!value) {
        const key = item.field.slice(0, 1).toUpperCase() + item.field.slice(1)

        if (item.field === 'cardBackgroundColor') {
          this.cardBackgroundColor = this.preThemeData.globalCss.cardBackgroundColor
        }

        if (item.field === 'cardBackground') {
          this.cardBackgroundStyleBox.setFormData(this.preThemeData.globalCss.cardBackground)
        }
        if (item.field === 'cardStyle') {
          this.cardBorderBox.setFormData(this.preThemeData.globalCss.cardStyle)
        }
        if (item.field === 'cardShadow') {
          this.cardShadowBox.setFormData(this.preThemeData.globalCss.cardShadow)
        }
        const defauleValue = this.preThemeData.globalCss[item.field]
        if (item.field === 'cardRadius') {
          // this.$refs.cardForm.setFormData({ [item.field]: defauleValue })
          this.cardRadiusValue = defauleValue
        }
        if (this['change' + key]) {
          this['change' + key](defauleValue)
        } else {
          this.publicChangeMethod(item.field, value ? defauleValue : undefined)
        }
      }
    },
    copyThemeConfig () {
      const data = { type: 'cardCustomStyleConfig', data: this.cardItem.cardStyle }
      localStorage.setItem('amCopyData', JSON.stringify(data))
      ElMessage.success('复制成功')
      // componentConfigs.request.copyTextToClipboard(JSON.stringify(data))
    },
    pasteThemeConfig () {
      try {
        const data = JSON.parse(localStorage.getItem('amCopyData'))
        if (data && data.type === 'cardCustomStyleConfig') {
          this.cardItem.cardStyle = data.data
          this.$nextTick(() => {
            this.init(this.theme)
          })
          this.$emit('change')
        } else {
          ElMessage.info('请粘贴正确的卡片设置数据')
        }
      } catch (error) {
        ElMessage.info('请粘贴正确的卡片设置数据')
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.card-setting-wrapper {
  display: flex;
  flex-direction: column;
  height: 100%;

  .title {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    height: 40px;
    font-size: 12px;
    color: #1a1a1a;
    background: #fcfcfc;
    box-shadow: 0 1px 0 0 #ededed;

    &:hover {

      .am-ps-btn {
        display: inline-block;
      }
    }
  }

  .am-ps-btn {
    position: absolute;
    display: none;
    font-size: 12px;

    &:nth-child(2) {
      right: 130px;
    }

    &:nth-child(3) {
      right: 110px;
    }
  }

  .card-setting-container {
    flex: 1;
    overflow: auto;

    :deep(.el-collapse-item__header) {
      // flex-direction: row-reverse;
      justify-content: space-between;
      height: 40px;
      padding: 0 12px;
      font-size: 12px;
      line-height: 40px;

      .el-collapse-item__arrow {
        margin: 0 10px;
      }
    }

    :deep(.card-radius-item .ef-radio-wrapper) {
      width: 97%;
    }

    :deep(.el-collapse-item__content) {
      padding: 0 12px 12px;
    }

    :deep(.el-collapse) {

      .chart-collapse-item {
        font-size: 14px;

        >.el-collapse-item__wrap {

          >.el-collapse-item__content {
            padding: 0;

            .el-collapse {
              border: 0;

              .el-collapse-item__header {
                border: 0;
              }

              .el-collapse-item__wrap {
                border: 0;

                .el-input-number--small {
                  width: 100%;
                }

                .chart-config-title {
                  width: 100px !important;
                  padding-right: 12px;
                  padding-left: 0 !important;
                }

                .el-input:not(.text) .el-input__inner {
                  text-align: left;
                  background: #ededed;
                  border: none;
                  border-radius: 4px !important;
                }

                .el-input-number {
                  position: relative;
                  width: 100%;

                  .el-input-number__decrease,
                  .el-input-number__increase {
                    display: none;
                  }

                  &:hover {

                    .el-input-number__decrease,
                    .el-input-number__increase {
                      display: inline-block;
                    }
                  }

                  &.is-controls-right .el-input__inner {
                    padding-right: 10px;
                  }
                }

                .el-form-item {
                  margin-bottom: 18px;

                  &:last-child {
                    margin-bottom: 0;
                  }

                  &:first-child {
                    margin-bottom: 18px;
                  }
                }
              }
            }
          }
        }
      }
    }

    .chart-collapse-item {
      font-size: 14px;

      :deep(.el-collapse-item__wrap) {

        >.el-collapse-item__content {
          padding: 0;

          .el-collapse {
            border: 0;

            .el-collapse-item__header {
              border: 0;
            }

            .el-collapse-item__wrap {
              border: 0;

              .el-input-number--small {
                width: 100%;
              }

              .chart-config-title {
                width: 100px !important;
                padding-right: 12px;
                padding-left: 0 !important;
              }

              .el-input:not(.text) .el-input__inner {
                text-align: left;
                background: #ededed;
                border: none;
                border-radius: 4px !important;
              }

              .el-input-number {
                position: relative;
                width: 100%;

                .el-input-number__decrease,
                .el-input-number__increase {
                  display: none;
                }

                &:hover {

                  .el-input-number__decrease,
                  .el-input-number__increase {
                    display: inline-block;
                  }
                }

                &.is-controls-right .el-input__inner {
                  padding-right: 10px;
                }
              }

              .el-form-item {
                margin-bottom: 18px;

                &:last-child {
                  margin-bottom: 0;
                }

                &:first-child {
                  margin-bottom: 18px;
                }
              }
            }
          }
        }
      }
    }

    .collapse-item {

      :deep(.el-form) {
        padding: 0;
      }

      :deep(.el-input-group__append) {
        position: absolute;
        top: 50%;
        right: 0;
        padding: 0 20px 0 0;
        background: transparent;
        border: none;
        transform: translateY(-50%);
      }

      .page-width-input {
        width: 100%;
        margin-top: 12px;

        :deep(.el-input__inner) {
          padding-right: 30px;
          background: #ededed;
          border: none;
          border-radius: 4px !important;
        }
      }

      .space-custom {
        display: flex;
        padding: 10px;
        background: #f7f7f7;

        > .label {
          display: inline-block;
          width: 80px;
          padding-right: 10px;
          text-align: right;
        }

        .input-number-box {
          display: flex;
          flex: 1;
          flex-direction: column;

          .input-label {
            margin-top: 5px;
            font-size: 12px;
            color: #a3a3a3;
          }

          &:last-child {
            margin-left: 10px;
          }

          > .el-input-number {
            position: relative;
            width: 100%;

            &::after {
              position: absolute;
              top: 0;
              right: 6px;
              display: inline;
              color: #a3a3a3;
              content: "px";
            }

            :deep() {

              .el-input__inner {
                text-align: left;
                background: #ededed;
                border: none;
              }

              .el-input-number__decrease,
              .el-input-number__increase {
                display: none;
              }
            }

            &:hover {

              :deep() {

                .el-input-number__decrease,
                .el-input-number__increase {
                  display: inline-block;
                }
              }
            }
          }

          .el-input-number.is-disabled {

            :deep() {

              .el-input-number__decrease,
              .el-input-number__increase {
                display: none !important;
              }
            }
          }
        }
      }

      &.is-disabled {
        cursor: not-allowed;
      }
    }

    .collapse-item.theme-mode-container {
      display: flex;
      gap: 10px;

      .theme-item {
        position: relative;
        flex: 1;
        height: 47px;
        cursor: pointer;
        border: 1px solid #c7c7c7;
        border-radius: 4px;

        > span {
          position: absolute;
          bottom: 0;
          display: flex;
          align-items: center;
          justify-content: center;
          width: 100%;
          height: 18px;
          color: #fff;
          background: rgb(0 0 0 / 40%);
        }

        &:hover,
        &.active {

          > span {
            background: rgba($color: #1f6aff, $alpha: 70%);
          }
        }

        &.is-disabled {
          pointer-events: none;
          cursor: not-allowed;
        }

        &.is-disabled.active {
          background: #79a6ff;

          > span {
            color: #e5e5e5;
            background: rgba($color: #1f6aff, $alpha: 50%);
          }
        }
      }
    }

    :deep(.el-radio-group) {
      display: flex;
      width: 100%;
      padding: 0 3px;
      background: #ededed;
      border-radius: 4px;

      .el-radio-button--small {
        display: flex;
        flex: 1;
        align-items: center;
        padding: 3px 0;

        .el-radio-button__inner {
          width: 100%;
          height: 100%;
          padding: 4px 10px;
          color: #a3a3a3 !important;
          background-color: #ededed !important;
          border-color: #ededed !important;
          box-shadow: none !important;
        }
      }

      .el-radio-button--small.is-active {

        .el-radio-button__inner {
          font-size: 12px;
          line-height: 1em;
          color: #0a0a0a !important;
          background-color: #fff !important;
          border-color: #fff !important;
          border-radius: 4px;
        }
      }
    }

    :deep(.el-form-item__label) {
      justify-content: flex-start;
      font-size: 12px;
      text-align: left;
    }

    .form-label {
      display: flex;
      align-items: center;
      font-size: 12px;
      color: #1a1a1a;
    }
  }

  .card-setting-footer {
    height: 40px !important;
    padding: 5px 20px 0;
    text-align: center;
    background: #f8f9fa;
  }
}

.page-width-container,
.page-height-container {

  .left-label {
    position: absolute;
    bottom: 0;
    left: -20px;
    font-size: 12px;
    transform: translateX(-100%);
  }
}
</style>
