-- Run once as a PostgreSQL superuser, e.g.:  sudo -u postgres psql -f db/setup.sql
CREATE ROLE vibelang WITH LOGIN PASSWORD 'vibelang';
CREATE DATABASE vibelang OWNER vibelang;
