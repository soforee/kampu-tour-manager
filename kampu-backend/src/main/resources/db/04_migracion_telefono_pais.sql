-- =====================================================================
-- Migración para una base de datos que YA existe (después de 03_...).
-- Si instalas desde cero no hace falta: 01 y 02 ya incluyen estos cambios.
--   1) Los teléfonos de clientes e invitados pasan al formato con código de país: '+51 987654321'.
--   2) En CLIENTES el correo y el teléfono pasan a ser obligatorios.
-- =====================================================================

-- Teléfonos sin código de país → se asume Perú (+51)
UPDATE CLIENTES SET telefono = '+51 ' || REGEXP_REPLACE(telefono, '[^0-9]', '')
 WHERE telefono IS NOT NULL AND telefono NOT LIKE '+%';
UPDATE RESERVAS SET contacto_telefono = '+51 ' || REGEXP_REPLACE(contacto_telefono, '[^0-9]', '')
 WHERE contacto_telefono IS NOT NULL AND contacto_telefono NOT LIKE '+%';

-- Con código pero sin espacio, de EE. UU./Canadá (+1) o Perú (+51)
UPDATE CLIENTES SET telefono = '+1 ' || SUBSTR(REGEXP_REPLACE(telefono, '[^0-9]', ''), 2)
 WHERE REGEXP_LIKE(REGEXP_REPLACE(telefono, '[^0-9+]', ''), '^\+1[0-9]{10}$');
UPDATE CLIENTES SET telefono = '+51 ' || SUBSTR(REGEXP_REPLACE(telefono, '[^0-9]', ''), 3)
 WHERE REGEXP_LIKE(REGEXP_REPLACE(telefono, '[^0-9+]', ''), '^\+51[0-9]{9}$');
UPDATE RESERVAS SET contacto_telefono = '+51 ' || SUBSTR(REGEXP_REPLACE(contacto_telefono, '[^0-9]', ''), 3)
 WHERE REGEXP_LIKE(REGEXP_REPLACE(contacto_telefono, '[^0-9+]', ''), '^\+51[0-9]{9}$');

COMMIT;

-- Revisa que esta consulta no devuelva filas antes del ALTER: son clientes sin correo o sin teléfono,
-- o con un teléfono que no se pudo convertir. Corrígelos desde la pantalla Clientes o con un UPDATE.
SELECT cliente_id, nombre_completo, email, telefono
  FROM CLIENTES
 WHERE email IS NULL OR telefono IS NULL
    OR NOT REGEXP_LIKE(telefono, '^\+[0-9]{1,3} [0-9]{7,11}$');

-- Si la consulta anterior no devolvió filas:
ALTER TABLE CLIENTES MODIFY (email NOT NULL, telefono NOT NULL);
