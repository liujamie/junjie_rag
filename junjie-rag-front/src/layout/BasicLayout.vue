<template>
  <div id="basic-layout" class="basic-layout">
    <el-container>
      <el-aside :width="asideWidth" class="layout-aside">
        <BasicAside @change-aside="changeAside" />
      </el-aside>
      <el-main class="layout-main">
        <RouterView />
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
const isCollapse = ref(false);

const asideWidth = computed(() => (isCollapse.value ? "65px" : "var(--sidebar-width)"));

const changeAside = (collapse: boolean) => {
  isCollapse.value = collapse;
};
</script>

<style scoped lang="less">
.basic-layout {
  height: 100vh;
  overflow: hidden;

  .layout-aside {
    height: 100vh;
    overflow: hidden;
    background: var(--bg-sidebar);
    transition: width var(--transition-base);
    z-index: 10;
  }

  .layout-main {
    height: 100vh;
    padding: 0;
    overflow: hidden;
    background: var(--bg-body);
  }
}
</style>
