-- Database-per-service on one Postgres instance (keeps the laptop/CI footprint small).
-- learning-service owns "learning" (created by POSTGRES_DB); insights-service owns "insights".
CREATE DATABASE insights;
