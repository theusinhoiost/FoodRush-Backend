-- =========================================================================
-- V3: EXPAND MOCK MENU ITEMS, SIZES, AND COMPLEMENTS
-- Adds full catalogs for both FoodRush Burger and Pizzaria Bella Napoli
-- =========================================================================

-- -------------------------------------------------------------------------
-- 1. ADDITIONAL MENU ITEMS FOR FOODRUSH BURGER (bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1)
-- -------------------------------------------------------------------------
INSERT INTO menu_items (id, restaurant_id, name, description, base_price, available)
VALUES
    ('cccccccc-cccc-cccc-cccc-ccccccccccc4', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'Bacon Cheddar Bomb', 'Blend bovino 160g recheado com queijo cheddar, coberto com muito bacon crocante e cebola caramelizada', 36.90, TRUE),
    ('cccccccc-cccc-cccc-cccc-ccccccccccc5', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'Chicken Crispy Supreme', 'Sobrecoxa de frango empanada super crocante, alface americana, picles e maionese de alho negro', 29.90, TRUE),
    ('cccccccc-cccc-cccc-cccc-ccccccccccc6', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'Veggie Falafel Burger', 'Hambúrguer artesanal de grão-de-bico e ervas frescas, tomate confit, rúcula e molho tahine', 31.50, TRUE),
    ('cccccccc-cccc-cccc-cccc-ccccccccccc7', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'Onion Rings Crocantes', 'Anéis de cebola empanados na cerveja artesanal servidos com molho barbecue', 18.00, TRUE),
    ('cccccccc-cccc-cccc-cccc-ccccccccccc8', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'Milkshake de Nutella com Ninho', 'Sorvete artesanal batido com Nutella pura e finalizado com leite Ninho e chantilly', 22.90, TRUE)
ON CONFLICT (id) DO NOTHING;

-- Sizes for new Burger items (using valid 12-char suffix hex: 000000000006 to 00000000000c)
INSERT INTO menu_item_sizes (id, menu_item_id, name, price, stock_quantity, version, available)
VALUES
    ('dddddddd-dddd-dddd-dddd-000000000006', 'cccccccc-cccc-cccc-cccc-ccccccccccc4', 'Individual (160g)', 36.90, 40, 0, TRUE),
    ('dddddddd-dddd-dddd-dddd-000000000007', 'cccccccc-cccc-cccc-cccc-ccccccccccc4', 'Duplo Recheado (320g)', 47.90, 25, 0, TRUE),
    ('dddddddd-dddd-dddd-dddd-000000000008', 'cccccccc-cccc-cccc-cccc-ccccccccccc5', 'Padrão (180g)', 29.90, 35, 0, TRUE),
    ('dddddddd-dddd-dddd-dddd-000000000009', 'cccccccc-cccc-cccc-cccc-ccccccccccc7', 'Porção Média (8 unidades)', 18.00, 50, 0, TRUE),
    ('dddddddd-dddd-dddd-dddd-00000000000a', 'cccccccc-cccc-cccc-cccc-ccccccccccc7', 'Porção Grande (14 unidades)', 26.00, 30, 0, TRUE),
    ('dddddddd-dddd-dddd-dddd-00000000000b', 'cccccccc-cccc-cccc-cccc-ccccccccccc8', '300ml', 18.90, 60, 0, TRUE),
    ('dddddddd-dddd-dddd-dddd-00000000000c', 'cccccccc-cccc-cccc-cccc-ccccccccccc8', '500ml', 22.90, 60, 0, TRUE)
ON CONFLICT (id) DO NOTHING;

-- Complement Group for Bacon Cheddar Bomb
INSERT INTO complement_groups (id, menu_item_id, name, required, min_quantity, max_quantity)
VALUES
    ('eeeeeeee-0000-0000-0000-000000000003', 'cccccccc-cccc-cccc-cccc-ccccccccccc4', 'Turbine seu Cheddar', FALSE, 0, 2)
ON CONFLICT (id) DO NOTHING;

INSERT INTO complement_items (id, group_id, name, price, available)
VALUES
    ('ffffffff-0000-0000-0000-000000000005', 'eeeeeeee-0000-0000-0000-000000000003', 'Picles Artesanal da Casa', 3.00, TRUE),
    ('ffffffff-0000-0000-0000-000000000006', 'eeeeeeee-0000-0000-0000-000000000003', 'Cebola Crispy', 4.00, TRUE)
ON CONFLICT (id) DO NOTHING;

-- -------------------------------------------------------------------------
-- 2. ADDITIONAL MENU ITEMS FOR PIZZARIA BELLA NAPOLI (bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2)
-- -------------------------------------------------------------------------
INSERT INTO menu_items (id, restaurant_id, name, description, base_price, available)
VALUES
    ('cccccccc-cccc-cccc-cccc-ccccccccccc9', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', 'Pizza Calabresa Especial', 'Molho artesanal, calabresa fatiada fina, cebola roxa, azeitonas pretas chilenas e orégano fresco', 52.00, TRUE),
    ('cccccccc-cccc-cccc-cccc-ccccccccccca', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', 'Pizza Quatro Queijos Gratinada', 'Muçarela, gorgonzola dolce, provolone curado, catupiry legítimo e parmesão ralado', 59.90, TRUE),
    ('cccccccc-cccc-cccc-cccc-cccccccccccb', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', 'Pizza Frango com Catupiry', 'Peito de frango desfiado temperado, milho verde e autêntico requeijão Catupiry', 56.00, TRUE),
    ('cccccccc-cccc-cccc-cccc-cccccccccccd', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', 'Pizza Doce de Banana com Canela', 'Banana caramelizada, açúcar mascavo, canela do ceilão e chocolate branco gratinado', 42.00, TRUE)
ON CONFLICT (id) DO NOTHING;

-- Sizes for new Pizza items
INSERT INTO menu_item_sizes (id, menu_item_id, name, price, stock_quantity, version, available)
VALUES
    ('dddddddd-dddd-dddd-dddd-00000000000d', 'cccccccc-cccc-cccc-cccc-ccccccccccc9', 'Média (6 fatias)', 44.00, 30, 0, TRUE),
    ('dddddddd-dddd-dddd-dddd-00000000000e', 'cccccccc-cccc-cccc-cccc-ccccccccccc9', 'Grande (8 fatias)', 52.00, 40, 0, TRUE),
    ('dddddddd-dddd-dddd-dddd-00000000000f', 'cccccccc-cccc-cccc-cccc-ccccccccccca', 'Grande (8 fatias)', 59.90, 35, 0, TRUE),
    ('dddddddd-dddd-dddd-dddd-000000000010', 'cccccccc-cccc-cccc-cccc-cccccccccccb', 'Média (6 fatias)', 48.00, 25, 0, TRUE),
    ('dddddddd-dddd-dddd-dddd-000000000011', 'cccccccc-cccc-cccc-cccc-cccccccccccb', 'Grande (8 fatias)', 56.00, 30, 0, TRUE),
    ('dddddddd-dddd-dddd-dddd-000000000012', 'cccccccc-cccc-cccc-cccc-cccccccccccd', 'Broto Doce (4 fatias)', 42.00, 20, 0, TRUE)
ON CONFLICT (id) DO NOTHING;

-- Complement Groups (Bordas Recheadas) for Pizzas
INSERT INTO complement_groups (id, menu_item_id, name, required, min_quantity, max_quantity)
VALUES
    ('eeeeeeee-0000-0000-0000-000000000004', 'cccccccc-cccc-cccc-cccc-ccccccccccc3', 'Borda Recheada', FALSE, 0, 1),
    ('eeeeeeee-0000-0000-0000-000000000005', 'cccccccc-cccc-cccc-cccc-ccccccccccc9', 'Borda Recheada', FALSE, 0, 1),
    ('eeeeeeee-0000-0000-0000-000000000006', 'cccccccc-cccc-cccc-cccc-ccccccccccca', 'Borda Recheada', FALSE, 0, 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO complement_items (id, group_id, name, price, available)
VALUES
    ('ffffffff-0000-0000-0000-000000000007', 'eeeeeeee-0000-0000-0000-000000000004', 'Borda de Catupiry Original', 9.50, TRUE),
    ('ffffffff-0000-0000-0000-000000000008', 'eeeeeeee-0000-0000-0000-000000000004', 'Borda de Cheddar Cremoso', 9.50, TRUE),
    ('ffffffff-0000-0000-0000-000000000009', 'eeeeeeee-0000-0000-0000-000000000005', 'Borda de Catupiry Original', 9.50, TRUE),
    ('ffffffff-0000-0000-0000-00000000000a', 'eeeeeeee-0000-0000-0000-000000000006', 'Borda Vulcão de Queijo Provolone', 12.00, TRUE)
ON CONFLICT (id) DO NOTHING;
