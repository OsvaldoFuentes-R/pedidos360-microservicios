-- Un esquema por microservicio dentro de la misma base (database-per-service lógico).
-- Hibernate crea las tablas solas (ddl-auto=update), pero NO crea los esquemas.
CREATE SCHEMA IF NOT EXISTS productos;
CREATE SCHEMA IF NOT EXISTS clientes;
CREATE SCHEMA IF NOT EXISTS pedidos;
CREATE SCHEMA IF NOT EXISTS notificaciones;
CREATE SCHEMA IF NOT EXISTS despacho;
CREATE SCHEMA IF NOT EXISTS analitica;
