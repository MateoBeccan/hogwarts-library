<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import AppButton from '@/components/common/AppButton.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import { useAuthStore } from '@/stores/auth.store';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const isCollapsed = ref(false);

interface NavigationItem {
  label: string;
  path: string;
  icon: string;
  disabled?: boolean;
}

interface NavigationGroup {
  label: string;
  items: NavigationItem[];
}

const navigationGroups: NavigationGroup[] = [
  {
    label: '',
    items: [{ label: 'Dashboard', path: '/dashboard', icon: 'D' }],
  },
  {
    label: 'Gestion',
    items: [
      { label: 'Clientes', path: '#', icon: 'C', disabled: true },
      { label: 'Categorias', path: '#', icon: 'K', disabled: true },
      { label: 'Libros', path: '#', icon: 'L', disabled: true },
      { label: 'Ejemplares', path: '#', icon: 'E', disabled: true },
    ],
  },
  {
    label: 'Operaciones',
    items: [
      { label: 'Alquileres', path: '#', icon: 'A', disabled: true },
      { label: 'Pagos', path: '#', icon: 'P', disabled: true },
    ],
  },
];

const pageTitle = computed(() => String(route.meta.title ?? 'Hogwarts Library'));
const breadcrumb = computed(() => String(route.meta.breadcrumb ?? ''));

function logout(): void {
  authStore.logout();
  void router.push({ name: 'login' });
}

function handleDisabledNavigation(event: MouseEvent, disabled?: boolean): void {
  if (disabled) {
    event.preventDefault();
  }
}
</script>

<template>
  <div class="app-shell" :class="{ 'app-shell--collapsed': isCollapsed }">
    <aside class="sidebar">
      <div class="sidebar__brand">
        <div class="brand-mark">HL</div>
        <div class="brand-copy">
          <strong>Hogwarts</strong>
          <span>Library Management</span>
        </div>
      </div>

      <button class="sidebar__collapse" type="button" @click="isCollapsed = !isCollapsed">
        <span aria-hidden="true">{{ isCollapsed ? '>' : '<' }}</span>
        <span>Colapsar</span>
      </button>

      <nav class="sidebar__nav" aria-label="Navegacion principal">
        <section v-for="group in navigationGroups" :key="group.label || 'main'" class="nav-group">
          <p v-if="group.label" class="nav-group__label">{{ group.label }}</p>
          <RouterLink
            v-for="item in group.items"
            :key="item.label"
            class="nav-link"
            :class="{ 'nav-link--disabled': item.disabled }"
            :to="item.path"
            @click="handleDisabledNavigation($event, item.disabled)"
          >
            <span class="nav-link__icon" aria-hidden="true">{{ item.icon }}</span>
            <span>{{ item.label }}</span>
          </RouterLink>
        </section>
      </nav>

      <div class="sidebar__user">
        <div class="avatar">{{ authStore.initials }}</div>
        <div class="user-copy">
          <strong>{{ authStore.username }}</strong>
          <StatusBadge tone="warning">{{ authStore.role }}</StatusBadge>
        </div>
        <AppButton variant="ghost" @click="logout">Salir</AppButton>
      </div>
    </aside>

    <div class="app-main">
      <header class="topbar">
        <div>
          <p v-if="breadcrumb" class="topbar__breadcrumb">{{ breadcrumb }}</p>
          <h1>{{ pageTitle }}</h1>
        </div>
        <div class="topbar__user">
          <div class="topbar__identity">
            <strong>{{ authStore.username }}</strong>
            <span>{{ authStore.role }}</span>
          </div>
          <div class="avatar">{{ authStore.initials }}</div>
        </div>
      </header>

      <main class="content-panel">
        <RouterView />
      </main>
    </div>
  </div>
</template>





