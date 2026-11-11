-- =====================================================================
-- KAMPU / TOUR MANAGER v4 - PARTICIONAMIENTO Y ROLES POR NIVEL DE USUARIO
-- Ejecutar DESPUÉS de 01_schema.sql y 02_data.sql, con el usuario dueño.
--
-- Requisitos
--   * Oracle 12.2+ con la opción Partitioning (Enterprise, XE 18c+ o 23ai Free).
--     MODIFY ... PARTITION BY ... ONLINE convierte las tablas sin perder datos.
--   * Privilegio CREATE ROLE. Si el dueño no lo tiene, un DBA ejecuta antes:
--       GRANT CREATE ROLE TO LP2;
--
-- Niveles de usuario
--   ADMIN    : todo (usuarios, configuración, auditoría, datos comerciales y operativos).
--   VENDEDOR : operación comercial (clientes, reservas, pagos, catálogo, reportes).
--              No gestiona usuarios ni flota.
--   OPERADOR : ejecución de paquetes y tráfico (proveedores, personal, vehículos,
--              operaciones). No ve montos ni crea clientes.
--   CLIENTE  : solo sus paquetes (VW_PAQUETES_CLIENTE) e informar pagos.
--
-- La aplicación se conecta con un único usuario de base de datos: dentro de ella
-- el nivel lo controla PKG_SEGURIDAD_ROLES (matriz de permisos) y las vistas.
-- Los roles de base de datos de la sección 2 sirven para cuentas que entren
-- directo a Oracle (reportes, SQL Developer) con exactamente los mismos ámbitos.
-- =====================================================================

-- =====================================================================
-- 1. PARTICIONAMIENTO POR LISTA (LIST) ALINEADO A LOS NIVELES DE USUARIO
-- =====================================================================

-- 1.1 AUDITORIA por rol de quien hizo el cambio.
--     El administrador revisa todo; cada nivel consulta solo su partición
--     (p. ej. qué cambió Ventas) sin recorrer las demás.
ALTER TABLE AUDITORIA MODIFY
    PARTITION BY LIST (rol_usuario) (
        PARTITION p_aud_admin    VALUES ('ADMIN'),
        PARTITION p_aud_ventas   VALUES ('VENDEDOR'),
        PARTITION p_aud_operador VALUES ('OPERADOR'),
        PARTITION p_aud_cliente  VALUES ('CLIENTE'),
        PARTITION p_aud_sistema  VALUES (DEFAULT)
    ) ONLINE UPDATE INDEXES;

-- 1.2 PAGOS por quién los registró.
--     Ventas trabaja con sus pagos (y los del admin); los pagos informados por
--     los clientes desde el portal quedan aparte, como cola por validar.
ALTER TABLE PAGOS MODIFY
    PARTITION BY LIST (registrado_por) (
        PARTITION p_pag_admin   VALUES ('ADMIN'),
        PARTITION p_pag_ventas  VALUES ('VENDEDOR'),
        PARTITION p_pag_cliente VALUES ('CLIENTE')
    ) ONLINE UPDATE INDEXES;

-- 1.3 OPERACIONES_TRANSPORTE por estado: la cola de trabajo del Operador
--     (por empezar / en curso) separada del histórico cerrado.
--     ROW MOVEMENT: al finalizar o cancelar, la fila pasa sola a la partición histórica.
ALTER TABLE OPERACIONES_TRANSPORTE MODIFY
    PARTITION BY LIST (estado) (
        PARTITION p_op_cola_operador VALUES ('PROGRAMADO', 'ASIGNADO', 'EMBARQUE', 'EN_RUTA'),
        PARTITION p_op_cerradas      VALUES ('FINALIZADO', 'CANCELADO')
    ) ONLINE UPDATE INDEXES;
ALTER TABLE OPERACIONES_TRANSPORTE ENABLE ROW MOVEMENT;

-- 1.4 RESERVAS por estado: las vigentes (trabajo diario de Ventas) separadas
--     de las cerradas (completadas o anuladas, solo consulta y auditoría).
ALTER TABLE RESERVAS MODIFY
    PARTITION BY LIST (estado_reserva) (
        PARTITION p_res_vigentes VALUES ('PENDIENTE', 'CONFIRMADA'),
        PARTITION p_res_cerradas VALUES ('COMPLETADA', 'ANULADA')
    ) ONLINE UPDATE INDEXES;
ALTER TABLE RESERVAS ENABLE ROW MOVEMENT;

COMMENT ON TABLE AUDITORIA IS 'Particionada por rol_usuario: ADMIN ve todo; cada nivel consulta su partición.';
COMMENT ON TABLE PAGOS IS 'Particionada por registrado_por: pagos de Ventas/Admin y pagos informados por clientes (cola por validar).';
COMMENT ON TABLE OPERACIONES_TRANSPORTE IS 'Particionada por estado: p_op_cola_operador es la cola de trabajo del Operador.';
COMMENT ON TABLE RESERVAS IS 'Particionada por estado_reserva: vigentes (Ventas) y cerradas (consulta).';

-- =====================================================================
-- 2. ROLES DE BASE DE DATOS POR NIVEL DE USUARIO
--    (se ignoran los errores si el rol ya existe al re-ejecutar)
-- =====================================================================
BEGIN
    FOR r IN (SELECT column_value AS rol
                FROM TABLE(sys.odcivarchar2list('ROL_ADMIN', 'ROL_VENTAS', 'ROL_OPERADOR', 'ROL_CLIENTE'))) LOOP
        BEGIN
            EXECUTE IMMEDIATE 'CREATE ROLE ' || r.rol;
        EXCEPTION
            WHEN OTHERS THEN
                IF SQLCODE <> -1921 THEN RAISE; END IF;   -- ORA-01921: el rol ya existe
        END;
    END LOOP;
END;
/

-- 2.1 ADMIN: acceso total
BEGIN
    FOR t IN (SELECT table_name FROM USER_TABLES
               WHERE table_name IN ('USUARIOS_SISTEMA', 'CLIENTES', 'PROVEEDORES', 'LUGARES', 'PERSONAL',
                                    'VEHICULOS', 'PAQUETES_TOUR', 'SALIDAS_PROGRAMADAS', 'RESERVAS',
                                    'DETALLE_RESERVA', 'PASAJEROS_RESERVA', 'PAGOS', 'OPERACIONES_TRANSPORTE',
                                    'HISTORIAL_OPERACIONES', 'AUDITORIA')) LOOP
        EXECUTE IMMEDIATE 'GRANT SELECT, INSERT, UPDATE, DELETE ON ' || t.table_name || ' TO ROL_ADMIN';
    END LOOP;
END;
/
GRANT SELECT ON VW_PAQUETES_CLIENTE         TO ROL_ADMIN;
GRANT SELECT ON VW_RESUMEN_PAQUETES_POR_ROL TO ROL_ADMIN;
GRANT EXECUTE ON PKG_SEGURIDAD_ROLES          TO ROL_ADMIN;
GRANT EXECUTE ON PKG_GESTION_PAQUETES_CLIENTE TO ROL_ADMIN;
GRANT EXECUTE ON FN_ESTADO_PAGO_PAQUETE       TO ROL_ADMIN;
GRANT EXECUTE ON SP_REGISTRAR_CLIENTE_EXTERNO TO ROL_ADMIN;
GRANT SELECT  ON SEQ_CODIGO_OPERACION         TO ROL_ADMIN;

-- 2.2 VENTAS: gestión comercial (sin usuarios del sistema ni control de flota)
GRANT SELECT, INSERT, UPDATE ON CLIENTES            TO ROL_VENTAS;
GRANT SELECT, INSERT, UPDATE ON RESERVAS            TO ROL_VENTAS;
GRANT SELECT, INSERT, UPDATE ON DETALLE_RESERVA     TO ROL_VENTAS;
GRANT SELECT, INSERT, UPDATE ON PASAJEROS_RESERVA   TO ROL_VENTAS;
GRANT SELECT, INSERT, UPDATE ON PAGOS               TO ROL_VENTAS;
GRANT SELECT, INSERT, UPDATE ON PAQUETES_TOUR       TO ROL_VENTAS;
GRANT SELECT, INSERT, UPDATE ON SALIDAS_PROGRAMADAS TO ROL_VENTAS;
-- elegir guías
GRANT SELECT ON PERSONAL                TO ROL_VENTAS;
GRANT SELECT ON LUGARES                 TO ROL_VENTAS;
-- consulta del tráfico de sus salidas
GRANT SELECT ON OPERACIONES_TRANSPORTE  TO ROL_VENTAS;
GRANT SELECT ON VW_RESUMEN_PAQUETES_POR_ROL TO ROL_VENTAS;
GRANT EXECUTE ON FN_ESTADO_PAGO_PAQUETE TO ROL_VENTAS;
GRANT EXECUTE ON PKG_SEGURIDAD_ROLES    TO ROL_VENTAS;

-- 2.3 OPERADOR: logística y tráfico (sin finanzas ni clientes)
GRANT SELECT, INSERT, UPDATE ON PROVEEDORES            TO ROL_OPERADOR;
GRANT SELECT, INSERT, UPDATE ON PERSONAL               TO ROL_OPERADOR;
GRANT SELECT, INSERT, UPDATE ON VEHICULOS              TO ROL_OPERADOR;
GRANT SELECT, INSERT, UPDATE ON LUGARES                TO ROL_OPERADOR;
GRANT SELECT, INSERT, UPDATE ON OPERACIONES_TRANSPORTE TO ROL_OPERADOR;
GRANT SELECT, INSERT ON HISTORIAL_OPERACIONES          TO ROL_OPERADOR;
GRANT SELECT ON SALIDAS_PROGRAMADAS TO ROL_OPERADOR;
GRANT SELECT ON PAQUETES_TOUR       TO ROL_OPERADOR;
-- manifiesto de pasajeros
GRANT SELECT ON PASAJEROS_RESERVA   TO ROL_OPERADOR;
-- sin montos para este rol
GRANT SELECT ON VW_RESUMEN_PAQUETES_POR_ROL TO ROL_OPERADOR;
GRANT SELECT ON SEQ_CODIGO_OPERACION TO ROL_OPERADOR;
GRANT EXECUTE ON PKG_SEGURIDAD_ROLES TO ROL_OPERADOR;

-- 2.4 CLIENTE: solo sus paquetes y el catálogo; ninguna tabla transaccional directa
GRANT SELECT ON VW_PAQUETES_CLIENTE TO ROL_CLIENTE;
GRANT SELECT ON PAQUETES_TOUR       TO ROL_CLIENTE;
GRANT SELECT ON SALIDAS_PROGRAMADAS TO ROL_CLIENTE;
GRANT EXECUTE ON PKG_GESTION_PAQUETES_CLIENTE TO ROL_CLIENTE;
GRANT EXECUTE ON SP_REGISTRAR_CLIENTE_EXTERNO TO ROL_CLIENTE;

-- Para dar un nivel a una cuenta de base de datos (ejemplo):
--   GRANT ROL_VENTAS TO usuario_reportes_ventas;

-- Particionar invalida los objetos que dependen de esas tablas (paquetes, triggers):
-- se recompilan aquí para que queden VALID sin esperar a su primer uso.
BEGIN
    DBMS_UTILITY.COMPILE_SCHEMA(schema => USER, compile_all => FALSE);
END;
/

-- =====================================================================
-- 3. VERIFICACIÓN
-- =====================================================================
SELECT table_name, partition_name, high_value
  FROM USER_TAB_PARTITIONS
 WHERE table_name IN ('AUDITORIA', 'PAGOS', 'OPERACIONES_TRANSPORTE', 'RESERVAS')
 ORDER BY table_name, partition_position;

SELECT grantee, COUNT(*) AS permisos
  FROM USER_TAB_PRIVS_MADE
 WHERE grantee LIKE 'ROL\_%' ESCAPE '\'
 GROUP BY grantee
 ORDER BY grantee;

SELECT object_type, object_name, status FROM USER_OBJECTS WHERE status <> 'VALID';
