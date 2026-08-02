CREATE TABLE stock_receipts (
    id BIGSERIAL PRIMARY KEY,
    purchase_id BIGINT NOT NULL REFERENCES stock_purchases(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity BIGINT NOT NULL CHECK (quantity > 0),
    received_by_email VARCHAR(320) NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_stock_receipts_purchase ON stock_receipts(purchase_id, received_at);
