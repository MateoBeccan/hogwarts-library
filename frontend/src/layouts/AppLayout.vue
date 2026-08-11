<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import logo from '@/assets/images/hogwarts-library-logo.png';
import alquileresIcon from '@/assets/images/hogwarts-library-alquileres-logo.png';
import adminIcon from '@/assets/images/hogwarts-library-admin-logo.png';
import autorIcon from '@/assets/images/hogwarts-library-autor-logo.png';
import categoriasIcon from '@/assets/images/hogwarts-library-categorias-logo.png';
import clientesIcon from '@/assets/images/hogwarts-library-clientes-logo.png';
import dashboardIcon from '@/assets/images/hogwarts-library-dashboard-logo.png';
import empleadoIcon from '@/assets/images/hogwarts-library-empleado-logo.png';
import ejemplaresIcon from '@/assets/images/hogwarts-library-ejemplares-logo.png';
import librosIcon from '@/assets/images/hogwarts-library-libros-logo.png';
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
  iconImage?: string;
}

interface NavigationGroup {
  label: string;
  items: NavigationItem[];
}

const navigationGroups: NavigationGroup[] = [
  {
    label: '',
    items: [{ label: 'Dashboard', path: '/dashboard', icon: 'D', iconImage: dashboardIcon }],
  },
  {
    label: 'Gestion',
    items: [
      { label: 'Clientes', path: '/clientes', icon: 'C', iconImage: clientesIcon },
      { label: 'Autores', path: '/autores', icon: 'Au', iconImage: autorIcon },
      { label: 'Categorias', path: '/categorias', icon: 'K', iconImage: categoriasIcon },
      { label: 'Libros', path: '/libros', icon: 'L', iconImage: librosIcon },
      { label: 'Ejemplares', path: '/ejemplares', icon: 'E', iconImage: ejemplaresIcon },
    ],
  },
  {
    label: 'Operaciones',
    items: [
      { label: 'Alquileres', path: '/alquileres', icon: 'A', iconImage: alquileresIcon },
    ],
  },
];

const pageTitle = computed(() => String(route.meta.title ?? 'Hogwarts Library'));
const breadcrumb = computed(() => String(route.meta.breadcrumb ?? ''));
const userIcon = computed(() => (authStore.role === 'ADMIN' ? adminIcon : empleadoIcon));

function logout(): void {
  authStore.logout();
  void router.push({ name: 'login' });
}
</script>

<template>
  <div class="app-shell" :class="{ 'app-shell--collapsed': isCollapsed }">
    <aside class="sidebar">
      <RouterLink class="sidebar__brand" to="/dashboard" aria-label="Ir al dashboard">
        <img class="brand-logo" :src="logo" alt="Hogwarts Library" />
      </RouterLink>

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
            :class="{ 'nav-link--image': item.iconImage }"
            :to="item.path"
          >
            <span class="nav-link__icon" :class="{ 'nav-link__icon--image': item.iconImage }" aria-hidden="true">
              <img v-if="item.iconImage" class="nav-link__image" :src="item.iconImage" alt="" />
              <span v-else>{{ item.icon }}</span>
            </span>
            <span>{{ item.label }}</span>
          </RouterLink>
        </section>
      </nav>

      <div class="sidebar__user">
        <div class="avatar avatar--image"><img :src="userIcon" :alt="`Usuario ${authStore.role ?? ''}`" /></div>
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
          <div class="avatar avatar--image"><img :src="userIcon" :alt="`Usuario ${authStore.role ?? ''}`" /></div>
        </div>
      </header>

      <main class="content-panel">
        <RouterView />
      </main>
    </div>
  </div>
</template>
