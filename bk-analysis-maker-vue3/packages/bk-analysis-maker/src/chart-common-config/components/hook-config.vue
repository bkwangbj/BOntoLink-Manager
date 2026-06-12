<template>
  <CollapseItem
    v-model="form.show"
    name="Hook配置"
    @change="$emit('chartChange')"
  >
    <div class="hook-config">
      <el-form
        size="small"
        label-width="80px"
        :disabled="!form.show||!saveAble"
        class="inline-col-form hook-form"
      >
        <el-form-item label="脚本">
          <BKCodeCom
            ref="editor"
            v-model="form.script"
            style="width: 100%;height: 200px;"
            language="javascript"
            :readonly="!saveAble"
            @blur="$emit('chartChange')"
          />
        </el-form-item>
      </el-form>
    </div>
  </CollapseItem>
</template>

<script>

export default {
  name: 'HookConfig',
  inject: ['getSaveAble'],
  emits: ['chartChange'],
  data () {
    return {
      form: {
        show: false,
        script: `
onMount(function () {
  console.log('组件已挂载');

  // 获取当前组件
  var myImage = stage.get('#hookId#');
  console.log(myImage);
  if (myImage) {

    // 触发自定义事件
    myImage.emit('custom-event', {
      message: '来自Hook的自定义事件'
    });
  }
})


// 数据变化时
onDataChange(function (newData) {
  console.log('数据已更新:', newData);

});

// 组件更新时
onUpdate(function () {
  console.log('组件已更新');
});

// 组件销毁时
onDestroy(function () {
  console.log('组件即将销毁');
});`
      },
      editorCfg: {
        minimap: { enabled: false }, theme: 'vs-dark', overviewRulerBorder: false
      }
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }
  },
  mounted () {
  },
  methods: {
    setFormData (form) {
      this.form = { ...this.$options.data().form, ...form }
    },
    saveFormData () {
      return { ...this.form }
    }
  }
}
</script>
<style lang="scss" scoped>
.hook-form {

  >:deep(.el-form-item) {
    margin-bottom: 10px !important;
  }
}
</style>
