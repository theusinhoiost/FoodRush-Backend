-- Migration V6: Harmonize CONFIRMED orders to PAID
UPDATE orders SET status = 'PAID' WHERE status = 'CONFIRMED';
