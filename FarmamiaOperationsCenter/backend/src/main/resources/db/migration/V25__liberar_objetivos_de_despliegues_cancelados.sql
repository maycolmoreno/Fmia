-- Corrige datos existentes: antes de este cambio, cancelar un despliegue (RepositorioDesplieguesJpaAdaptador.cancelar)
-- no marcaba sus objetivos como finalizados. Eso dejaba a esas farmacias bloqueadas para siempre por la
-- regla "solo 1 actualizacion activa por farmacia" (ver V24). Esta migracion libera los objetivos de
-- despliegues que ya estaban CANCELLED antes del fix en el codigo.
UPDATE deployment_targets dt
SET status = 'SKIPPED'
FROM deployments d
WHERE dt.deployment_id = d.id
  AND d.status = 'CANCELLED'
  AND dt.status NOT IN ('COMPLETED', 'FAILED', 'ROLLBACK_COMPLETED', 'ROLLBACK_FAILED', 'SKIPPED');
