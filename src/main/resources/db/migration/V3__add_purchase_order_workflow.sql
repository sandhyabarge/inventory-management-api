ALTER TABLE stock_purchases
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN submitted_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN approved_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN approved_by_email VARCHAR(320),
    ADD COLUMN cancelled_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE stock_purchase_items
    ADD COLUMN received_quantity BIGINT NOT NULL DEFAULT 0
        CHECK (received_quantity >= 0 AND received_quantity <= quantity);

-- Purchases created by the previous immediate-receipt implementation are already in stock.
UPDATE stock_purchases SET status = 'RECEIVED';
UPDATE stock_purchase_items SET received_quantity = quantity;

ALTER TABLE stock_purchases
    ADD CONSTRAINT ck_stock_purchase_status CHECK (status IN (
        'DRAFT', 'SUBMITTED', 'APPROVED', 'PARTIALLY_RECEIVED', 'RECEIVED', 'CANCELLED'
    ));
