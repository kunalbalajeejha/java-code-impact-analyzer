-- Seed data loaded on every startup (H2 in-memory)

INSERT INTO users (id, name, email, tier) VALUES
  (1, 'Alice Smith',   'alice@example.com',  'GOLD'),
  (2, 'Bob Jones',     'bob@example.com',    'SILVER'),
  (3, 'Carol White',   'carol@example.com',  'BRONZE');

INSERT INTO products (id, name, price, stock) VALUES
  (1, 'Laptop Pro',    1299.99, 50),
  (2, 'Wireless Mouse',  29.99, 200),
  (3, 'USB-C Hub',       49.99, 150),
  (4, 'Mechanical Keyboard', 89.99, 75);
