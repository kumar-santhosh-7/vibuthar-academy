ALTER TABLE users
    ADD COLUMN ug_degree VARCHAR(150) NULL AFTER phone,
    ADD COLUMN pg_degree VARCHAR(150) NULL AFTER ug_degree,
    ADD COLUMN address VARCHAR(500) NULL AFTER pg_degree;
