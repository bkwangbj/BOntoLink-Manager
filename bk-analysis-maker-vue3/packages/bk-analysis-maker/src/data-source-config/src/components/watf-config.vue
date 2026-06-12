<template>
  <EfModal
    v-model="modalVisible"
    width="800px"
    height="600px"
    title="选取数据"
    @close="$emit('close')"
  >
    <div class="watf-panel">
      <div class="watf-content">
        <div class="watf-header">
          <EfSelect
            v-model="bzCode"
            size="small"
            :clearable="false"
            style="margin-right: 10px;"
            dictionary-key="WATF_B_M_BUSINESS"
            @change="loadData(false)"
          />
          <EfSelect
            v-model="appId"
            size="small"
            style="margin-right: 10px;"
            :clearable="false"
            dictionary-key="WATF_B_M_APP"
          />
          <el-switch
            v-model="dataType"
            active-text="视图"
            inactive-text="指标集"
            :disabled="!setMode"
            class="am-switch"
            size="small"
            @change="loadData(false)"
          />
        </div>
        <div class="watf-main">
          <EfTree
            ref="tree"
            node-key="id"
            :props="props"
            :mock="dataList"
            default-expand-all
            :lazy="false"
            has-line
          >
            <template #default="{ node }">
              <div
                @dblclick="setMode ? save() : undefined"
              >
                <span>
                  {{ node.label }}
                </span>
              </div>
            </template>
          </EfTree>
        </div>
      </div>
      <div class="analysis-modal-footer">
        <el-button
          type="primary"
          size="small"
          :disabled="!setMode"
          @click="save"
        >
          保存
        </el-button>
        <el-button
          size="small"
          class="analysis-modal-close-button"
          @click="$emit('close')"
        >
          关闭
        </el-button>
      </div>
    </div>
  </EfModal>
</template>
<script>
import { utils } from 'efficient-suite'
import { ElMessage } from 'element-plus'
import { componentConfigs } from '../../../configs'
export default {
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    setMode: {
      type: Boolean,
      default: true
    },
    watfConfig: {
      type: String,
      default: ''
    }
  },
  emits: ['close', 'save'],
  data () {
    return {
      props: {
        label: 'label',
        children: 'children',
        isLeaf: 'isLeaf'
      },
      appId: 'watf',
      bzCode: 'WATF',
      dataType: false,
      dataList: [],
      defaultIndex: '',
      configObject: {},
      modalVisible: false
    }
  },
  watch: {
    visible:
    {
      handler () {
        this.$nextTick(() => {
          this.modalVisible = this.visible
          if (this.modalVisible) {
            this.$nextTick(() => {
              this.init()
            })
          }
        })
      },
      immediate: true
    }
  },
  created () {

  },
  mounted () {

  },
  methods: {
    init () {
      try {
        const config = JSON.parse(this.watfConfig)
        if (config) {
          this.configObject = config
          if (config.appId) {
            this.appId = config.appId
          }
          if (config.bzCode) {
            this.bzCode = config.bzCode
          }
          if (config.viewCode) {
            this.dataType = true
            this.defaultIndex = config.viewCode
          } else {
            this.defaultIndex = config.metaSet
          }
        }
      } catch (error) {

      }
      this.loadData(true)
    },
    async loadData (isDefault) {
      const treeData = await componentConfigs.request.post('/tree/data', {
        appId: 'watf',
        bzCode: 'WATF',
        treeConfigId: this.dataType ? 'WATF_VIEW_TREE' : 'WATF_B_M_META',
        node: 'ROOT',
        regExp: '',
        queryParams: {
          bzCode: this.bzCode
        }
      })
      this.dataList = treeData
      const flattenList = []
      utils.flattenList(treeData, flattenList)
      if (isDefault && this.defaultIndex && flattenList.find(c => c.id === this.defaultIndex)) {
        this.$nextTick(() => {
          this.$refs.tree.setCurrentKey(this.defaultIndex)
        })
      }
    },
    save () {
      if (!this.setMode) {
        return
      }
      const node = this.$refs.tree.getCurrentNode()
      if (!node) {
        return ElMessage.error('请选择数据')
      }
      if (!node.isLeaf) {
        return ElMessage.error('请选择叶子节点')
      }
      this.configObject = {
        ...this.configObject,
        url: '/grid/getTableData',
        appId: this.appId,
        bzCode: this.bzCode,
        metaSet: node.id,
        module: node.label,
        viewCode: ''
      }
      if (this.dataType) {
        this.configObject.viewCode = node.id
        this.configObject.url = '/view/getTableData'
      }
      this.$emit('save', this.configObject)
      this.$emit('close')
    }
  }
}

</script>

<style lang="scss" scoped>
.watf-panel {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;

  .watf-content {
    position: relative;
    display: flex;
    flex: 1;
    flex-direction: column;
    padding: 10px 0 0;
    overflow: hidden;

    .watf-header {
      display: flex;
      align-items: center;
      padding-bottom: 10px;
      padding-left: 5px;
      border-bottom: 1px solid #e8eaec;
    }

    .watf-main {
      flex: 1;
      overflow: auto;
    }
  }
}
</style>
