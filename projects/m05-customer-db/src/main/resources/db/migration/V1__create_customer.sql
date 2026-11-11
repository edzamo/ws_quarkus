CREATE TABLE customer (
    id    BIGSERIAL    PRIMARY KEY,
    name  VARCHAR(100) NOT NULL,
    tier  VARCHAR(10)  NOT NULL DEFAULT 'BASIC',
    CONSTRAINT customer_tier_chk CHECK (tier IN ('BASIC', 'GOLD'))
);
