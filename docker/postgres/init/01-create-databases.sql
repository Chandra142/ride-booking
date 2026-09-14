-- =====================================================================
-- Ride Booking System - logical database bootstrap
-- ---------------------------------------------------------------------
-- Executed by the official postgres image on FIRST initialization only
-- (i.e. when the postgres_data volume is empty). Scripts placed in
-- /docker-entrypoint-initdb.d/ are run in alphabetical order by psql.
--
-- This file only creates LOGICAL DATABASES. Application tables are the
-- responsibility of the services (Hibernate ddl-auto) - never create
-- application tables here.
--
-- Databases created are owned by the configured database superuser
-- (POSTGRES_USER / DB_USERNAME, default: postgres), which is also the
-- user the Java services connect with.
--
-- Re-running on an existing volume: init scripts do not execute again.
-- To re-bootstrap, the volume would need to be recreated with:
--   docker compose down -v
-- (this DESTROYS all local database data).
--
-- dbserver list (target architecture):
--   ride_booking            <- created by the container via POSTGRES_DB
--   ride_booking_driver     <- created below
--   ride_service_db         <- created below
--   payment_db              <- created below
--   notification_db         <- created below
-- =====================================================================

SELECT 'CREATE DATABASE ride_booking_driver'
  WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ride_booking_driver')
\gexec

SELECT 'CREATE DATABASE ride_service_db'
  WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ride_service_db')
\gexec

SELECT 'CREATE DATABASE payment_db'
  WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'payment_db')
\gexec

SELECT 'CREATE DATABASE notification_db'
  WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'notification_db')
\gexec