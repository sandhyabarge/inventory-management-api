CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE suppliers (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE warehouses (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE stock_purchases (
    id BIGSERIAL PRIMARY KEY,
    reference VARCHAR(80) NOT NULL UNIQUE,
    supplier_id BIGINT NOT NULL REFERENCES suppliers(id),
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    purchased_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by_email VARCHAR(320) NOT NULL,
    total_cost NUMERIC(19, 2) NOT NULL CHECK (total_cost >= 0)
);

CREATE TABLE stock_purchase_items (
    id BIGSERIAL PRIMARY KEY,
    purchase_id BIGINT NOT NULL REFERENCES stock_purchases(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity BIGINT NOT NULL CHECK (quantity > 0),
    unit_cost NUMERIC(19, 2) NOT NULL CHECK (unit_cost > 0),
    UNIQUE (purchase_id, product_id)
);

CREATE TABLE inventory_stocks (
    id BIGSERIAL PRIMARY KEY,
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity BIGINT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (warehouse_id, product_id)
);

INSERT INTO products (sku, name) VALUES
    ('SKU-LAPTOP-STAND', 'Adjustable Laptop Stand'),
    ('SKU-WIRELESS-MOUSE', 'Wireless Mouse'),
    ('SKU-KEYBOARD', 'Mechanical Keyboard'),
    ('SKU-USB-C-HUB', 'USB-C Hub'),
    ('SKU-MONITOR-24', '24-inch Monitor');

INSERT INTO suppliers (code, name) VALUES
    ('SUP-TECHSOURCE', 'TechSource Supplies'),
    ('SUP-OFFICEPRO', 'OfficePro Distribution'),
    ('SUP-GLOBAL', 'Global Components');

INSERT INTO warehouses (code, name) VALUES
    ('WH-NORTH', 'North Warehouse'),
    ('WH-CENTRAL', 'Central Warehouse'),
    ('WH-SOUTH', 'South Warehouse');
