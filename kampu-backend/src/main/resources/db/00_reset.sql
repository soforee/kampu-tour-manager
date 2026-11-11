-- =====================================================================
-- KAMPU - Borra TODO el esquema (versiones anteriores, v3 y v4) para reinstalar.
-- Luego ejecuta 01_schema_v4.sql, 02_data.sql y 03_particionamiento_roles.sql.
-- ¡Elimina los datos! Ejecutar como script (F5) con el usuario dueño.
-- =====================================================================
BEGIN
    FOR o IN (SELECT 'VIEW' AS tipo, column_value AS nombre
                FROM TABLE(sys.odcivarchar2list(
                     'VW_PAQUETES_CLIENTE', 'VW_RESUMEN_PAQUETES_POR_ROL',            -- v4
                     'VW_RECURSOS_DISPONIBLES', 'VW_TABLERO_OPERACIONES',             -- v3
                     'VW_EMPRESA_CAPACIDAD', 'V_RESERVAS_RESUMEN', 'V_PORTAL_CLIENTE', 'V_TABLERO_FIDS'))
              UNION ALL
              SELECT 'PACKAGE', column_value
                FROM TABLE(sys.odcivarchar2list(
                     'PKG_SEGURIDAD_ROLES', 'PKG_GESTION_PAQUETES_CLIENTE',
                     'PKG_ASIGNACION', 'PKG_RUTAS', 'PKG_TRANSACCIONES'))
              UNION ALL
              SELECT 'FUNCTION', 'FN_ESTADO_PAGO_PAQUETE' FROM dual
              UNION ALL
              SELECT 'PROCEDURE', 'SP_REGISTRAR_CLIENTE_EXTERNO' FROM dual
              UNION ALL
              SELECT 'TABLE', column_value
                FROM TABLE(sys.odcivarchar2list(
                     'AUDITORIA', 'HISTORIAL_OPERACIONES', 'OPERACIONES_TRANSPORTE',          -- v4
                     'HISTORIAL_TRAFICO_OPERACION', 'OPERACION_TRANSPORTE',                   -- v3
                     'PAGOS', 'PASAJEROS_RESERVA', 'DETALLE_RESERVA', 'RESERVAS', 'SALIDAS_PROGRAMADAS',
                     'DOMINIO_TRANSICIONES', 'DOMINIO_ESTADOS', 'CONDUCTOR', 'VEHICULO', 'VEHICULOS',
                     'PERSONAL', 'RUTAS_PROVEEDOR', 'LUGARES', 'EMPRESA_TRANSPORTE', 'PROVEEDORES',
                     'CHOFERES', 'TRANSPORTE',
                     'PAQUETES_TOUR', 'GUIAS', 'CLIENTES', 'USUARIOS_SISTEMA'))
              UNION ALL
              SELECT 'SEQUENCE', 'SEQ_CODIGO_OPERACION' FROM dual) LOOP
        BEGIN
            IF o.tipo = 'TABLE' THEN
                EXECUTE IMMEDIATE 'DROP TABLE ' || o.nombre || ' CASCADE CONSTRAINTS PURGE';
            ELSE
                EXECUTE IMMEDIATE 'DROP ' || o.tipo || ' ' || o.nombre;
            END IF;
        EXCEPTION
            WHEN OTHERS THEN NULL;
        END;
    END LOOP;
END;
/
