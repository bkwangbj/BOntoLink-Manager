<template>
  <CollapseItem
    v-model="form.show"
    name="内边距"
    @change="$emit('chartChange')"
  >
    <div class="padding-config">
      <el-form
        size="small"
        label-width="80px"
        :disabled="disabled"
      >
        <el-form-item label="边距">
          <el-row>
            <el-col :span="24">
              <PaddingBox
                ref="paddingBoxRef"
                :disabled="disabled"
                @click.stop
                @change="girdPositionChange"
              />
            </el-col>
          </el-row>
        </el-form-item>
      </el-form>
    </div>
  </CollapseItem>
</template>

<script>
import PaddingBox from '../../analysis-maker/src/components/padding-box.vue'
import { utils } from 'efficient-suite'
export default {

  name: 'CardPaddingConfig',
  components: {
    PaddingBox

  },
  inject: ['getSaveAble'],
  emits: ['chartChange'],
  data () {
    return {
      form: {
        show: false
      },
      position: {}
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    },
    disabled () {
      return !this.form.show || !this.saveAble
    }
  },
  methods: {
    girdPositionChange (e) {
      this.position = { top: e.paddingTop, bottom: e.paddingBottom, left: e.paddingLeft, right: e.paddingRight }
      this.$emit('chartChange')
    },
    setFormData (e) {
      Object.assign(this.$data, this.$options.data())
      if (Object.keys(e).length !== 0) {
        this.form = { ...e, show: true }
        const obj = utils.deepClone(e)
        delete obj.show
        if (Object.keys(obj).length !== 0) {
          this.$refs.paddingBoxRef.setPadding({ paddingTop: e.top, paddingBottom: e.bottom, paddingLeft: e.left, paddingRight: e.right })
        } else {
          this.$refs.paddingBoxRef.setPadding(null)
        }
      } else {
        this.$refs.paddingBoxRef.setPadding(null)
      }
    },
    saveFormData () {
      if (this.form.show) {
        return { ...this.form, ...this.position, show: true }
      } else {
        return {}
      }
    }
  }
}
</script>
<style lang="scss" scoped>
.padding-config {
  display: flex;
  // margin-bottom: 10px;
}
</style>
