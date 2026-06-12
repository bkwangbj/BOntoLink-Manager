import CommonColorPicker from './common-color-picker.vue'
import ColorsPicker from './colors-picker.vue'
import CollapseItem from './collapse-item.vue'
import DataFormatSelect from './data-format-select.vue'
import PercPxInput from './perc-px-input.vue'
import PositionStyle from './style-config/position-style.vue'
import TextStyle from './style-config/text-style.vue'
import ShadowStyle from './style-config/shadow-style.vue'
import LineStyle from './style-config/line-style.vue'
CommonColorPicker.install = function (Vue) {
  Vue.component(CommonColorPicker.name, CommonColorPicker)
}
ColorsPicker.install = function (Vue) {
  Vue.component(ColorsPicker.name, ColorsPicker)
}
PercPxInput.install = function (Vue) {
  Vue.component(PercPxInput.name, PercPxInput)
}
CollapseItem.install = function (Vue) {
  Vue.component(CollapseItem.name, CollapseItem)
}
DataFormatSelect.install = function (Vue) {
  Vue.component(DataFormatSelect.name, DataFormatSelect)
}
PositionStyle.install = function (Vue) {
  Vue.component(PositionStyle.name, PositionStyle)
}
TextStyle.install = function (Vue) {
  Vue.component(TextStyle.name, TextStyle)
}
LineStyle.install = function (Vue) {
  Vue.component(LineStyle.name, LineStyle)
}
ShadowStyle.install = function (Vue) {
  Vue.component(ShadowStyle.name, ShadowStyle)
}
const list = [CommonColorPicker, ShadowStyle, ColorsPicker, CollapseItem, DataFormatSelect, PercPxInput, PositionStyle, TextStyle, LineStyle]
export const chartComponents = { install: (Vue) => { list.forEach((item) => { item.install(Vue) }) } }
