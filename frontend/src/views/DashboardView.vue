<script setup lang="ts">
import PageHeader from '@/components/common/PageHeader.vue';
import StatCard from '@/components/common/StatCard.vue';
</script>

<template>
  <section class="dashboard">
    <PageHeader title="Dashboard" subtitle="Resumen general de la biblioteca" />

    <div class="stats-grid">
      <StatCard icon="C" label="Clientes activos" value="-" description="Pendiente de Fase 2" />
      <StatCard icon="L" label="Libros activos" value="-" description="Pendiente de Fase 2" />
      <StatCard icon="E" label="Ejemplares disponibles" value="-" description="Pendiente de Fase 2" />
      <StatCard icon="A" label="Alquileres activos" value="-" description="Pendiente de Fase 3" />
    </div>

    <section class="workspace-section">
      <div>
        <h2>Actividad de alquileres</h2>
        <p>La arquitectura visual queda preparada. Los datos se conectaran cuando se implemente la fase correspondiente.</p>
      </div>
      <div class="activity-strip" aria-hidden="true">
        <span class="activity-strip__item activity-strip__item--active" />
        <span class="activity-strip__item activity-strip__item--finished" />
        <span class="activity-strip__item activity-strip__item--cancelled" />
      </div>
    </section>

    <section class="quick-actions">
      <h2>Acciones rapidas</h2>
      <div class="quick-actions__grid">
        <button type="button" disabled>+ Nuevo cliente</button>
        <button type="button" disabled>+ Nuevo libro</button>
        <button type="button" disabled>+ Nuevo ejemplar</button>
        <button type="button" disabled>+ Nuevo alquiler</button>
      </div>
    </section>
  </section>
</template>
