import { createRouter, createWebHistory } from 'vue-router';

import AppLayout from '@/layouts/AppLayout.vue';
import AlquilerDetailView from '@/views/AlquilerDetailView.vue';
import AlquileresView from '@/views/AlquileresView.vue';
import AutorDetailView from '@/views/AutorDetailView.vue';
import AutorFormView from '@/views/AutorFormView.vue';
import AutoresView from '@/views/AutoresView.vue';
import CategoriaFormView from '@/views/CategoriaFormView.vue';
import CategoriasView from '@/views/CategoriasView.vue';
import ClienteDetailView from '@/views/ClienteDetailView.vue';
import ClienteFormView from '@/views/ClienteFormView.vue';
import ClientesView from '@/views/ClientesView.vue';
import DashboardView from '@/views/DashboardView.vue';
import EjemplarDetailView from '@/views/EjemplarDetailView.vue';
import EjemplarFormView from '@/views/EjemplarFormView.vue';
import EjemplaresView from '@/views/EjemplaresView.vue';
import LibroDetailView from '@/views/LibroDetailView.vue';
import LibroFormView from '@/views/LibroFormView.vue';
import LibrosView from '@/views/LibrosView.vue';
import LoginView from '@/views/LoginView.vue';
import NuevoAlquilerView from '@/views/NuevoAlquilerView.vue';
import { useAuthStore } from '@/stores/auth.store';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/dashboard' },
    { path: '/login', name: 'login', component: LoginView, meta: { guestOnly: true } },
    {
      path: '/',
      component: AppLayout,
      meta: { requiresAuth: true },
      children: [
        { path: 'dashboard', name: 'dashboard', component: DashboardView, meta: { title: '', breadcrumb: 'Inicio / Dashboard' } },
        { path: 'clientes', name: 'clientes', component: ClientesView, meta: { title: '', breadcrumb: 'Inicio / Clientes' } },
        { path: 'clientes/nuevo', name: 'cliente-nuevo', component: ClienteFormView, meta: { title: 'Nuevo cliente', breadcrumb: 'Inicio / Clientes / Nuevo' } },
        { path: 'clientes/:id', name: 'cliente-detalle', component: ClienteDetailView, meta: { title: 'Detalle de cliente', breadcrumb: 'Inicio / Clientes / Detalle' } },
        { path: 'clientes/:id/editar', name: 'cliente-editar', component: ClienteFormView, meta: { title: 'Editar cliente', breadcrumb: 'Inicio / Clientes / Editar' } },
        { path: 'autores', name: 'autores', component: AutoresView, meta: { title: '', breadcrumb: 'Inicio / Autores' } },
        { path: 'autores/nuevo', name: 'autor-nuevo', component: AutorFormView, meta: { title: 'Nuevo autor', breadcrumb: 'Inicio / Autores / Nuevo' } },
        { path: 'autores/:id', name: 'autor-detalle', component: AutorDetailView, meta: { title: 'Detalle de autor', breadcrumb: 'Inicio / Autores / Detalle' } },
        { path: 'autores/:id/editar', name: 'autor-editar', component: AutorFormView, meta: { title: 'Editar autor', breadcrumb: 'Inicio / Autores / Editar' } },
        { path: 'categorias', name: 'categorias', component: CategoriasView, meta: { title: '', breadcrumb: 'Inicio / Categorias' } },
        { path: 'categorias/nueva', name: 'categoria-nueva', component: CategoriaFormView, meta: { title: 'Nueva categoria', breadcrumb: 'Inicio / Categorias / Nueva' } },
        { path: 'categorias/:id/editar', name: 'categoria-editar', component: CategoriaFormView, meta: { title: 'Editar categoria', breadcrumb: 'Inicio / Categorias / Editar' } },
        { path: 'libros', name: 'libros', component: LibrosView, meta: { title: '', breadcrumb: 'Inicio / Libros' } },
        { path: 'libros/nuevo', name: 'libro-nuevo', component: LibroFormView, meta: { title: 'Nuevo libro', breadcrumb: 'Inicio / Libros / Nuevo' } },
        { path: 'libros/:id', name: 'libro-detalle', component: LibroDetailView, meta: { title: 'Detalle de libro', breadcrumb: 'Inicio / Libros / Detalle' } },
        { path: 'libros/:id/editar', name: 'libro-editar', component: LibroFormView, meta: { title: 'Editar libro', breadcrumb: 'Inicio / Libros / Editar' } },
        { path: 'ejemplares', name: 'ejemplares', component: EjemplaresView, meta: { title: '', breadcrumb: 'Inicio / Ejemplares' } },
        { path: 'ejemplares/nuevo', name: 'ejemplar-nuevo', component: EjemplarFormView, meta: { title: 'Nuevo ejemplar', breadcrumb: 'Inicio / Ejemplares / Nuevo' } },
        { path: 'ejemplares/:id', name: 'ejemplar-detalle', component: EjemplarDetailView, meta: { title: 'Detalle de ejemplar', breadcrumb: 'Inicio / Ejemplares / Detalle' } },
        { path: 'ejemplares/:id/editar', name: 'ejemplar-editar', component: EjemplarFormView, meta: { title: 'Editar ejemplar', breadcrumb: 'Inicio / Ejemplares / Editar' } },
        { path: 'alquileres', name: 'alquileres', component: AlquileresView, meta: { title: '', breadcrumb: 'Inicio / Alquileres' } },
        { path: 'alquileres/nuevo', name: 'alquiler-nuevo', component: NuevoAlquilerView, meta: { title: 'Nuevo alquiler', breadcrumb: 'Inicio / Alquileres / Nuevo' } },
        { path: 'alquileres/:id', name: 'alquiler-detalle', component: AlquilerDetailView, meta: { title: 'Detalle de alquiler', breadcrumb: 'Inicio / Alquileres / Detalle' } },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
  ],
});

router.beforeEach((to) => {
  const authStore = useAuthStore();

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } };
  }

  if (to.meta.guestOnly && authStore.isAuthenticated) {
    return { name: 'dashboard' };
  }

  return true;
});

export default router;
