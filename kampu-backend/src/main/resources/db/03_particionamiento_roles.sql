-- =====================================================================
-- KAMPU / TOUR MANAGER - PARTICIONAMIENTO Y ROLES POR NIVEL DE USUARIO
-- Ejecutar DESPUÉS de 01_schema.sql y 02_data.sql, con el usuario dueño
-- (SQL Developer: "Ejecutar script" F5). Se puede ejecutar varias veces.
--
-- Requisitos
--   * Oracle 12.2+ con la opción Partitioning (XE 18c/21c, 23ai Free o Enterprise).
--   * Conectarse a la base de datos conectable (PDB), no al contenedor raíz:
--       XE 18c/21c  -> servicio XEPDB1   (jdbc:oracle:thin:@localhost:1521/XEPDB1)
--       23ai Free   -> servicio FREEPDB1
--     En el contenedor raíz (servicio XE / FREE) Oracle no permite crear roles locales.
--   * Privilegio CREATE ROLE. Si el dueño no lo tiene, un DBA ejecuta antes:
--       GRANT CREATE ROLE TO LP2;
--   Si falta algo de lo anterior, el script NO se detiene: particiona lo que pueda y
--   muestra en la salida qué no se hizo y cómo resolverlo. La aplicación funciona igual
--   (los niveles de usuario de la app los controla PKG_SEGURIDAD_ROLES).
--
-- Niveles de usuario
--   ADMIN    : todo (usuarios, configuración, auditoría, datos comerciales y operativos).
--   VENDEDOR : operación comercial (clientes, reservas, pagos, catálogo). Sin usuarios ni flota.
--   OPERADOR : ejecución de paquetes y tráfico (proveedores, personal, vehículos,
--              operaciones). No ve montos ni clientes.
--   CLIENTE  : solo sus paquetes (VW_PAQUETES_CLIENTE) e informar pagos.
-- =====================================================================

SET SERVEROUTPUT ON FORMAT WRAPPED

-- =====================================================================
-- 1. PARTICIONAMIENTO POR LISTA (LIST) ALINEADO A LOS NIVELES DE USUARIO
--    AUDITORIA              por rol de quien hizo el cambio (cada nivel consulta su partición).
--    PAGOS                  por quién lo registró (los del portal quedan como cola por validar).
--    OPERACIONES_TRANSPORTE por estado (cola del Operador / histórico cerrado).
--    RESERVAS               por estado (vigentes de Ventas / cerradas).
-- =====================================================================
DECLARE
    v_sin_particiones BOOLEAN := FALSE;

    PROCEDURE particionar (p_tabla VARCHAR2, p_esquema VARCHAR2) IS
        v_existe NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_existe FROM USER_PART_TABLES WHERE table_name = p_tabla;
        IF v_existe > 0 THEN
            DBMS_OUTPUT.PUT_LINE('  ' || RPAD(p_tabla, 24) || 'ya estaba particionada');
            RETURN;
        END IF;
        BEGIN
            -- ONLINE: convierte la tabla sin bloquearla ni perder datos
            EXECUTE IMMEDIATE 'ALTER TABLE ' || p_tabla || ' MODIFY ' || p_esquema || ' ONLINE UPDATE INDEXES';
        EXCEPTION
            WHEN OTHERS THEN
                -- ORA-00439: la edición no admite la conversión ONLINE -> se hace sin ONLINE
                IF SQLCODE <> -439 THEN RAISE; END IF;
                EXECUTE IMMEDIATE 'ALTER TABLE ' || p_tabla || ' MODIFY ' || p_esquema || ' UPDATE INDEXES';
        END;
        DBMS_OUTPUT.PUT_LINE('  ' || RPAD(p_tabla, 24) || 'particionada');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = -439 THEN
                v_sin_particiones := TRUE;
                DBMS_OUTPUT.PUT_LINE('  ' || RPAD(p_tabla, 24) || 'NO particionada: esta edición de Oracle no tiene la opción Partitioning');
            ELSE
                RAISE;
            END IF;
    END particionar;
BEGIN
    DBMS_OUTPUT.PUT_LINE('1. Particionamiento');
    particionar('AUDITORIA', q'[PARTITION BY LIST (rol_usuario) (
        PARTITION p_aud_admin    VALUES ('ADMIN'),
        PARTITION p_aud_ventas   VALUES ('VENDEDOR'),
        PARTITION p_aud_operador VALUES ('OPERADOR'),
        PARTITION p_aud_cliente  VALUES ('CLIENTE'),
        PARTITION p_aud_sistema  VALUES (DEFAULT))]');
    particionar('PAGOS', q'[PARTITION BY LIST (registrado_por) (
        PARTITION p_pag_admin   VALUES ('ADMIN'),
        PARTITION p_pag_ventas  VALUES ('VENDEDOR'),
        PARTITION p_pag_cliente VALUES ('CLIENTE'))]');
    particionar('OPERACIONES_TRANSPORTE', q'[PARTITION BY LIST (estado) (
        PARTITION p_op_cola_operador VALUES ('PROGRAMADO', 'ASIGNADO', 'EMBARQUE', 'EN_RUTA'),
        PARTITION p_op_cerradas      VALUES ('FINALIZADO', 'CANCELADO'))]');
    particionar('RESERVAS', q'[PARTITION BY LIST (estado_reserva) (
        PARTITION p_res_vigentes VALUES ('PENDIENTE', 'CONFIRMADA'),
        PARTITION p_res_cerradas VALUES ('COMPLETADA', 'ANULADA'))]');

    -- Al cambiar de estado, la fila pasa sola a la otra partición
    EXECUTE IMMEDIATE 'ALTER TABLE OPERACIONES_TRANSPORTE ENABLE ROW MOVEMENT';
    EXECUTE IMMEDIATE 'ALTER TABLE RESERVAS ENABLE ROW MOVEMENT';

    EXECUTE IMMEDIATE q'[COMMENT ON TABLE AUDITORIA IS 'Particionada por rol_usuario: ADMIN ve todo; cada nivel consulta su partición.']';
    EXECUTE IMMEDIATE q'[COMMENT ON TABLE PAGOS IS 'Particionada por registrado_por: pagos de Ventas/Admin y pagos informados por clientes (cola por validar).']';
    EXECUTE IMMEDIATE q'[COMMENT ON TABLE OPERACIONES_TRANSPORTE IS 'Particionada por estado: p_op_cola_operador es la cola de trabajo del Operador.']';
    EXECUTE IMMEDIATE q'[COMMENT ON TABLE RESERVAS IS 'Particionada por estado_reserva: vigentes (Ventas) y cerradas (consulta).']';

    IF v_sin_particiones THEN
        DBMS_OUTPUT.PUT_LINE('  -> Usa Oracle XE 18c/21c, 23ai Free o Enterprise para tener particiones.');
    END IF;
END;
/

-- =====================================================================
-- 2. ROLES DE BASE DE DATOS POR NIVEL DE USUARIO
--    Para cuentas que entren directo a Oracle (reportes, SQL Developer) con los
--    mismos ámbitos que la aplicación. Ejemplo:  GRANT ROL_VENTAS TO usuario_reportes;
-- =====================================================================
DECLARE
    TYPE t_lista IS TABLE OF VARCHAR2(300);
    v_roles   t_lista := t_lista('ROL_ADMIN', 'ROL_VENTAS', 'ROL_OPERADOR', 'ROL_CLIENTE');
    v_motivo  VARCHAR2(400);
    v_total   PLS_INTEGER := 0;
    v_permisos t_lista := t_lista(
        -- ADMIN: acceso total
        'SELECT, INSERT, UPDATE, DELETE ON USUARIOS_SISTEMA TO ROL_ADMIN',
        'SELECT, INSERT, UPDATE, DELETE ON CLIENTES TO ROL_ADMIN',
        'SELECT, INSERT, UPDATE, DELETE ON PROVEEDORES TO ROL_ADMIN',
        'SELECT, INSERT, UPDATE, DELETE ON LUGARES TO ROL_ADMIN',
        'SELECT, INSERT, UPDATE, DELETE ON PERSONAL TO ROL_ADMIN',
        'SELECT, INSERT, UPDATE, DELETE ON VEHICULOS TO ROL_ADMIN',
        'SELECT, INSERT, UPDATE, DELETE ON PAQUETES_TOUR TO ROL_ADMIN',
        'SELECT, INSERT, UPDATE, DELETE ON SALIDAS_PROGRAMADAS TO ROL_ADMIN',
        'SELECT, INSERT, UPDATE, DELETE ON RESERVAS TO ROL_ADMIN',
        'SELECT, INSERT, UPDATE, DELETE ON DETALLE_RESERVA TO ROL_ADMIN',
        'SELECT, INSERT, UPDATE, DELETE ON PASAJEROS_RESERVA TO ROL_ADMIN',
        'SELECT, INSERT, UPDATE, DELETE ON PAGOS TO ROL_ADMIN',
        'SELECT, INSERT, UPDATE, DELETE ON OPERACIONES_TRANSPORTE TO ROL_ADMIN',
        'SELECT, INSERT, UPDATE, DELETE ON HISTORIAL_OPERACIONES TO ROL_ADMIN',
        'SELECT, INSERT, UPDATE, DELETE ON AUDITORIA TO ROL_ADMIN',
        'SELECT ON VW_PAQUETES_CLIENTE TO ROL_ADMIN',
        'SELECT ON VW_RESUMEN_PAQUETES_POR_ROL TO ROL_ADMIN',
        'EXECUTE ON PKG_SEGURIDAD_ROLES TO ROL_ADMIN',
        'EXECUTE ON PKG_GESTION_PAQUETES_CLIENTE TO ROL_ADMIN',
        'EXECUTE ON FN_ESTADO_PAGO_PAQUETE TO ROL_ADMIN',
        'EXECUTE ON SP_REGISTRAR_CLIENTE_EXTERNO TO ROL_ADMIN',
        'SELECT ON SEQ_CODIGO_OPERACION TO ROL_ADMIN',
        -- VENTAS: gestión comercial (sin usuarios del sistema ni control de flota)
        'SELECT, INSERT, UPDATE ON CLIENTES TO ROL_VENTAS',
        'SELECT, INSERT, UPDATE ON RESERVAS TO ROL_VENTAS',
        'SELECT, INSERT, UPDATE ON DETALLE_RESERVA TO ROL_VENTAS',
        'SELECT, INSERT, UPDATE ON PASAJEROS_RESERVA TO ROL_VENTAS',
        'SELECT, INSERT, UPDATE ON PAGOS TO ROL_VENTAS',
        'SELECT, INSERT, UPDATE ON PAQUETES_TOUR TO ROL_VENTAS',
        'SELECT, INSERT, UPDATE ON SALIDAS_PROGRAMADAS TO ROL_VENTAS',
        'SELECT ON PERSONAL TO ROL_VENTAS',
        'SELECT ON LUGARES TO ROL_VENTAS',
        'SELECT ON OPERACIONES_TRANSPORTE TO ROL_VENTAS',
        'SELECT ON VW_RESUMEN_PAQUETES_POR_ROL TO ROL_VENTAS',
        'EXECUTE ON FN_ESTADO_PAGO_PAQUETE TO ROL_VENTAS',
        'EXECUTE ON PKG_SEGURIDAD_ROLES TO ROL_VENTAS',
        -- OPERADOR: logística y tráfico (sin finanzas ni clientes)
        'SELECT, INSERT, UPDATE ON PROVEEDORES TO ROL_OPERADOR',
        'SELECT, INSERT, UPDATE ON PERSONAL TO ROL_OPERADOR',
        'SELECT, INSERT, UPDATE ON VEHICULOS TO ROL_OPERADOR',
        'SELECT, INSERT, UPDATE ON LUGARES TO ROL_OPERADOR',
        'SELECT, INSERT, UPDATE ON OPERACIONES_TRANSPORTE TO ROL_OPERADOR',
        'SELECT, INSERT ON HISTORIAL_OPERACIONES TO ROL_OPERADOR',
        'SELECT ON SALIDAS_PROGRAMADAS TO ROL_OPERADOR',
        'SELECT ON PAQUETES_TOUR TO ROL_OPERADOR',
        'SELECT ON PASAJEROS_RESERVA TO ROL_OPERADOR',
        'SELECT ON VW_RESUMEN_PAQUETES_POR_ROL TO ROL_OPERADOR',
        'SELECT ON SEQ_CODIGO_OPERACION TO ROL_OPERADOR',
        'EXECUTE ON PKG_SEGURIDAD_ROLES TO ROL_OPERADOR',
        -- CLIENTE: solo sus paquetes y el catálogo; ninguna tabla transaccional directa
        'SELECT ON VW_PAQUETES_CLIENTE TO ROL_CLIENTE',
        'SELECT ON PAQUETES_TOUR TO ROL_CLIENTE',
        'SELECT ON SALIDAS_PROGRAMADAS TO ROL_CLIENTE',
        'EXECUTE ON PKG_GESTION_PAQUETES_CLIENTE TO ROL_CLIENTE',
        'EXECUTE ON SP_REGISTRAR_CLIENTE_EXTERNO TO ROL_CLIENTE');
BEGIN
    DBMS_OUTPUT.PUT_LINE('2. Roles por nivel de usuario');
    -- En el contenedor raíz (CDB$ROOT) Oracle no admite roles locales
    IF SYS_CONTEXT('USERENV', 'CON_NAME') = 'CDB$ROOT' THEN
        v_motivo := 'Estás conectado al contenedor raíz (servicio XE / FREE). '
                 || 'Conéctate a la PDB (XEPDB1 en XE 18c/21c, FREEPDB1 en 23ai Free) y vuelve a ejecutar los scripts.';
    END IF;
    FOR i IN 1 .. v_roles.COUNT LOOP
        EXIT WHEN v_motivo IS NOT NULL;
        BEGIN
            EXECUTE IMMEDIATE 'CREATE ROLE ' || v_roles(i);
            DBMS_OUTPUT.PUT_LINE('  ' || RPAD(v_roles(i), 24) || 'creado');
        EXCEPTION
            WHEN OTHERS THEN
                CASE SQLCODE
                    WHEN -1921 THEN   -- ORA-01921: ya existe
                        DBMS_OUTPUT.PUT_LINE('  ' || RPAD(v_roles(i), 24) || 'ya existía');
                    WHEN -1031 THEN   -- ORA-01031: privilegios insuficientes
                        v_motivo := 'El usuario ' || USER || ' no tiene el privilegio CREATE ROLE. '
                                 || 'Pide a un DBA: GRANT CREATE ROLE TO ' || USER || '; y vuelve a ejecutar este script.';
                    WHEN -65096 THEN  -- ORA-65096 / ORA-65048: conectado al contenedor raíz (CDB)
                        v_motivo := 'Estás conectado al contenedor raíz (servicio XE / FREE). '
                                 || 'Conéctate a la PDB (XEPDB1 en XE 18c/21c, FREEPDB1 en 23ai Free) y vuelve a ejecutar los scripts.';
                    WHEN -65048 THEN
                        v_motivo := 'Estás conectado al contenedor raíz (servicio XE / FREE). '
                                 || 'Conéctate a la PDB (XEPDB1 en XE 18c/21c, FREEPDB1 en 23ai Free) y vuelve a ejecutar los scripts.';
                    ELSE
                        RAISE;
                END CASE;
        END;
    END LOOP;

    IF v_motivo IS NOT NULL THEN
        DBMS_OUTPUT.PUT_LINE('  ROLES NO CREADOS: ' || v_motivo);
        DBMS_OUTPUT.PUT_LINE('  (La aplicación funciona igual: sus niveles los controla PKG_SEGURIDAD_ROLES.)');
        RETURN;
    END IF;

    FOR i IN 1 .. v_permisos.COUNT LOOP
        EXECUTE IMMEDIATE 'GRANT ' || v_permisos(i);
        v_total := v_total + 1;
    END LOOP;
    DBMS_OUTPUT.PUT_LINE('  ' || v_total || ' permisos otorgados a los 4 roles');
END;
/

-- =====================================================================
-- 3. RECOMPILAR Y VERIFICAR
--    Particionar invalida los objetos que dependen de esas tablas (paquetes,
--    triggers, vistas): se recompilan para que queden VALID.
-- =====================================================================
BEGIN
    DBMS_UTILITY.COMPILE_SCHEMA(schema => USER, compile_all => FALSE);
END;
/

SELECT table_name, partition_name
  FROM USER_TAB_PARTITIONS
 WHERE table_name IN ('AUDITORIA', 'PAGOS', 'OPERACIONES_TRANSPORTE', 'RESERVAS')
 ORDER BY table_name, partition_position;

-- Debe devolver "no rows selected": todos los objetos VALID
SELECT object_type, object_name, status FROM USER_OBJECTS WHERE status <> 'VALID';
