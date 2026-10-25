-- Seed de dados de teste - Guesthouse API (MySQL)
-- Carregado automaticamente ao subir a aplicação (spring.sql.init).
-- Pode ser executado várias vezes sem duplicar dados.

-- ---------------------------------------------------------------
-- Usuários  (senhas em BCrypt)
--   recepcao@teste.com  / Recepcao@123  -> RECEPTION
--   hospede@teste.com   / Hospede@123   -> GUEST
--   maria@teste.com     / Hospede@123   -> GUEST
-- ---------------------------------------------------------------
INSERT IGNORE INTO user_account (name, cpf, email, telephone, password, profile) VALUES
('Recepção Teste',  '98765432100', 'recepcao@teste.com', '+5511988880000', '$2a$10$0u6dzKuoYdWaPBpuqcc9Mes09fy3sl31fGRWNdRzALyk.B6xk1oG2', 'RECEPTION'),
('Hóspede Teste',   '12345678901', 'hospede@teste.com',  '+5511999990000', '$2a$10$Ck3c1uWGt3j2eqt0BAMbz.YVBpctHvv3VPwKp5UD0ULsVs/m.CuFm', 'GUEST'),
('Maria Oliveira',  '11122233344', 'maria@teste.com',    '+5511977770000', '$2a$10$Ck3c1uWGt3j2eqt0BAMbz.YVBpctHvv3VPwKp5UD0ULsVs/m.CuFm', 'GUEST');

-- ---------------------------------------------------------------
-- Tipos de quarto
-- ---------------------------------------------------------------
INSERT INTO room_types (name, capacity, daily_rate)
SELECT * FROM (
  SELECT 'Standard Solteiro' AS name, 1 AS capacity, 150.00 AS daily_rate UNION ALL
  SELECT 'Standard Casal',            2,             220.00 UNION ALL
  SELECT 'Superior Família',          4,             380.00 UNION ALL
  SELECT 'Suíte Master',              3,             520.00
) t
WHERE NOT EXISTS (SELECT 1 FROM room_types);

-- ---------------------------------------------------------------
-- Quartos (status AVAILABLE) // são gerados 12 como informado pela pousada
-- ---------------------------------------------------------------
INSERT IGNORE INTO rooms (number, room_type_id, status, status_updated_at)
SELECT n, (SELECT id FROM room_types WHERE name = t), 'AVAILABLE', NOW()
FROM (
  SELECT '101' AS n, 'Standard Solteiro' AS t UNION ALL
  SELECT '102', 'Standard Solteiro' UNION ALL
  SELECT '103', 'Standard Casal'    UNION ALL
  SELECT '104', 'Standard Casal'    UNION ALL
  SELECT '105', 'Standard Casal'    UNION ALL
  SELECT '106', 'Standard Solteiro' UNION ALL
  SELECT '201', 'Superior Família'  UNION ALL
  SELECT '202', 'Superior Família'  UNION ALL
  SELECT '203', 'Superior Família'  UNION ALL
  SELECT '204', 'Superior Família'  UNION ALL
  SELECT '301', 'Suíte Master'      UNION ALL
  SELECT '302', 'Suíte Master'
) q;

-- ---------------------------------------------------------------
-- Temporadas
-- ---------------------------------------------------------------

INSERT INTO seasons (name, start_date, end_date, multiplier)
SELECT t.name, t.start_date, t.end_date, t.multiplier FROM (
  SELECT 'Alta Temporada' AS name, DATE '2026-12-20' AS start_date, DATE '2027-01-05' AS end_date, 1.50 AS multiplier UNION ALL
  SELECT 'Carnaval 2027',           DATE '2027-02-05',               DATE '2027-02-10',             1.40 UNION ALL
  SELECT 'Férias de Julho 2027',    DATE '2027-07-01',               DATE '2027-07-31',             1.25
) t
WHERE NOT EXISTS (SELECT 1 FROM seasons s WHERE s.name = t.name);