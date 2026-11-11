ALTER TABLE customer ADD COLUMN phone VARCHAR(20);

INSERT INTO customer (name, tier, phone) VALUES
    ('Ana Torres', 'GOLD', '555-1234'),
    ('Luis Quispe', 'BASIC', '555-5678'),
    ('Rosa Díaz', 'BASIC', '555-9012');

