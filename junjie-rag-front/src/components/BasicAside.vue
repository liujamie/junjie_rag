<template>
  <div id="basic-aside">
    <el-menu
      :default-active="defaultPath"
      class="aside-menu"
      :collapse="isCollapse"
      background-color="#0F172A"
      text-color="#CBD5E1"
      active-text-color="#FFFFFF"
    >
      <div class="menu-header" :class="{ collapsed: isCollapse }">
        <div class="logo-icon">
          <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
            <rect width="28" height="28" rx="8" fill="#0891B2"/>
            <path d="M8 14L12 18L20 10" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="menu-title" v-show="!isCollapse">
          <h1>LIU-RAG-AI</h1>
          <span class="menu-subtitle">AI 知识库问答系统</span>
        </div>
      </div>

      <div class="menu-divider" v-show="!isCollapse"></div>

      <el-menu-item
        v-for="item in menuRouterList"
        :key="item.path"
        :index="item.path"
        @click="handleSelect(item)"
        class="nav-item"
      >
        <el-icon>
          <component :is="item.meta?.icon"></component>
        </el-icon>
        <template #title>
          <span class="nav-label">{{ item.meta?.description }}</span>
        </template>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<script setup lang="ts">
import routes from "@/router/config.ts";
import router from "@/router";

const emit = defineEmits(["changeAside"]);
const isCollapse = ref(false);
const path = router.currentRoute.value.fullPath;
const defaultPath = ref(path === "/" ? "/chat" : path);

const menuRouterList = computed(() => {
  const userRole = localStorage.getItem('userRole') || 'user'
  return routes.filter((item) => {
    if (!item.meta?.isMenu) return false
    // 如果路由配置了角色限制，则校验当前用户角色
    if (item.meta?.roles && !item.meta.roles.includes(userRole)) return false
    return true
  });
});

router.afterEach((to) => {
  defaultPath.value = to.path;
});

const handleSelect = (e: any) => {
  router.push({ path: e.path });
};
</script>

<style scoped lang="less">
#basic-aside {
  height: 100%;
}

.aside-menu {
  border-right: none;
  height: 100%;
  display: flex;
  flex-direction: column;
  padding-top: 0;
}

.menu-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 24px 20px 20px;
  color: #FFFFFF;

  &.collapsed {
    justify-content: center;
    padding: 20px 0;
  }

  .menu-title {
    h1 {
      font-size: 16px;
      font-weight: 700;
      letter-spacing: 0.5px;
      color: #FFFFFF;
      margin: 0;
      line-height: 1.3;
    }

    .menu-subtitle {
      font-size: 11px;
      color: #64748B;
      display: block;
      margin-top: 2px;
    }
  }
}

.menu-divider {
  height: 1px;
  background: linear-gradient(to right, transparent, #1E293B, transparent);
  margin: 0 16px 8px;
}

.nav-item {
  margin: 2px 8px;
  border-radius: 8px;
  height: 44px;
  line-height: 44px;
  transition: all 150ms ease;

  &:hover {
    background-color: #1E293B !important;
  }

  &.is-active {
    background-color: #0891B2 !important;
    box-shadow: 0 2px 8px rgba(8, 145, 178, 0.3);

    .nav-label {
      color: #FFFFFF;
      font-weight: 500;
    }
  }

  .el-icon {
    font-size: 18px;
    margin-right: 10px;
  }
}

.nav-label {
  font-size: 14px;
  letter-spacing: 0.3px;
}

:deep(.el-menu--collapse) {
  .menu-header {
    padding: 16px 0 8px;
  }

  .nav-item {
    margin: 2px 10px;
    padding: 0;
    width: auto;
    border-radius: 8px;
    justify-content: center;

    .el-icon {
      margin-right: 0;
    }
  }
}
</style>
