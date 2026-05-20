<template>
  <div class="page">
    <PageHeader title="权限安全" subtitle="Security · 角色 / 资源访问控制（占位）">
      <template #actions>
        <button class="bl-btn bl-btn-primary">新建角色</button>
      </template>
    </PageHeader>
    <div class="body">
      <div class="bl-card">
        <table class="bl-table">
          <thead><tr><th>角色</th><th>资源范围</th><th>操作权限</th><th>启用</th></tr></thead>
          <tbody>
            <tr v-for="r in roles" :key="r.id">
              <td>{{ r.name }}</td>
              <td><span class="bl-tag">{{ r.scope }}</span></td>
              <td><span v-for="p in r.perms" :key="p" class="bl-tag bl-tag-primary" style="margin-right:6px">{{ p }}</span></td>
              <td><span class="bl-tag bl-tag-success">已启用</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import PageHeader from '@/components/PageHeader.vue'
const roles = [
  { id: 1, name: '本体管理员', scope: '全部领域',     perms: ['创建','编辑','发布','审阅'] },
  { id: 2, name: '业务建模师', scope: 'w_wtr',         perms: ['创建','编辑'] },
  { id: 3, name: '只读用户',   scope: 'w_common',      perms: ['只读'] }
]
</script>
<style scoped>
.page { display: flex; flex-direction: column; height: 100%; }
.body { padding: 12px; flex: 1; overflow: auto; }
</style>
