-- =====================================================================
-- KAMPU / TOUR MANAGER v4 - Datos de demostración
-- Ejecutar DESPUÉS de 01_schema.sql, como script (F5), con el usuario dueño.
--
-- Las fechas son relativas al día en que se ejecuta (TRUNC(SYSDATE) + n): así
-- el portal del cliente siempre muestra paquetes "Por empezar", "En curso" y
-- "Terminado".
--
-- Cuentas
--   Personal interno : admin / admin123 · ventas / ventas123 · operador / operador123
--   Portal de cliente: ana.ramirez@mail.com / cliente123 · john.miller@mail.com / cliente123
--   (los demás clientes los creó Ventas: aún no tienen cuenta web; pueden
--    activarla con "Registrarse" usando su mismo documento y correo)
-- =====================================================================

-- Quién hace los cambios (lo registra TRG_AUDITORIA_ESTADO_PAQUETE)
EXEC PKG_SEGURIDAD_ROLES.iniciar_sesion('admin', 'ADMIN');

-- ---------------------------------------------------------------------
-- Usuarios internos (contraseña en texto plano o hash BCrypt: el backend acepta ambos)
-- ---------------------------------------------------------------------
INSERT INTO USUARIOS_SISTEMA (usuario_id, username, password_hash, rol, estado) VALUES (1, 'admin', 'admin123', 'ADMIN', 'ACTIVO');
INSERT INTO USUARIOS_SISTEMA (usuario_id, username, password_hash, rol, estado) VALUES (2, 'ventas', 'ventas123', 'VENDEDOR', 'ACTIVO');
INSERT INTO USUARIOS_SISTEMA (usuario_id, username, password_hash, rol, estado) VALUES (3, 'operador', 'operador123', 'OPERADOR', 'ACTIVO');

-- ---------------------------------------------------------------------
-- Clientes (password_hash = BCrypt de 'cliente123' para los que tienen cuenta web)
-- ---------------------------------------------------------------------
INSERT INTO CLIENTES (cliente_id, nombre_completo, tipo_documento, numero_documento, email, telefono, password_hash, estado_cuenta)
VALUES (1, 'Ana Ramírez Soto', 'DNI', '45871236', 'ana.ramirez@mail.com', '+51 987654321', '$2a$10$pVkHXziWsxkOHAduDWapIu1uuCCBncNyR.OlCRJBzk/CmbH.Tb5Wu', 'ACTIVO');
INSERT INTO CLIENTES (cliente_id, nombre_completo, tipo_documento, numero_documento, email, telefono, password_hash, estado_cuenta)
VALUES (2, 'Lucía Fernández Cruz', 'DNI', '71452368', 'lucia.fernandez@mail.com', '+51 956874123', NULL, 'ACTIVO');
INSERT INTO CLIENTES (cliente_id, nombre_completo, tipo_documento, numero_documento, email, telefono, password_hash, estado_cuenta)
VALUES (3, 'John Miller', 'PASAPORTE', 'X4587126', 'john.miller@mail.com', '+1 3055550142', '$2a$10$pVkHXziWsxkOHAduDWapIu1uuCCBncNyR.OlCRJBzk/CmbH.Tb5Wu', 'ACTIVO');
INSERT INTO CLIENTES (cliente_id, nombre_completo, tipo_documento, numero_documento, email, telefono, password_hash, estado_cuenta)
VALUES (4, 'Sofía Castillo Rojas', 'CE', '001245789', 'sofia.castillo@mail.com', '+51 945123789', NULL, 'ACTIVO');
INSERT INTO CLIENTES (cliente_id, nombre_completo, tipo_documento, numero_documento, email, telefono, password_hash, estado_cuenta)
VALUES (5, 'Diego Salazar Luna', 'DNI', '43698521', 'diego.salazar@mail.com', '+51 965478123', NULL, 'ACTIVO');
INSERT INTO CLIENTES (cliente_id, nombre_completo, tipo_documento, numero_documento, email, telefono, password_hash, estado_cuenta)
VALUES (6, 'Pedro Huertas Vela', 'DNI', '42587136', 'pedro.huertas@mail.com', '+51 934521876', NULL, 'ACTIVO');
INSERT INTO CLIENTES (cliente_id, nombre_completo, tipo_documento, numero_documento, email, telefono, password_hash, estado_cuenta)
VALUES (7, 'Luis Paredes Gil', 'DNI', '47125836', 'luis.paredes@mail.com', '+51 912008745', NULL, 'ACTIVO');
INSERT INTO CLIENTES (cliente_id, nombre_completo, tipo_documento, numero_documento, email, telefono, password_hash, estado_cuenta)
VALUES (8, 'Andrea Campos Ruiz', 'DNI', '70418529', 'andrea.campos@mail.com', '+51 999120334', NULL, 'ACTIVO');
INSERT INTO CLIENTES (cliente_id, nombre_completo, tipo_documento, numero_documento, email, telefono, password_hash, estado_cuenta)
VALUES (9, 'Viajes Andinos SAC', 'RUC', '20556677889', 'reservas@viajesandinos.pe', '+51 014445566', NULL, 'ACTIVO');

-- ---------------------------------------------------------------------
-- Proveedores: Kampu Tours (propio) y terceros
-- ---------------------------------------------------------------------
INSERT INTO PROVEEDORES (proveedor_id, nombre_comercial, ruc, telefono, es_propio, estado, direccion, email, contacto, servicios, zonas, fecha_afiliacion) VALUES (1, 'Kampu Tours SAC', '20601122334', '+51 084223344', 1, 'ACTIVO', 'Av. El Sol 520, Cusco', 'operaciones@kampu.pe', 'Centro de operaciones Kampu', 'TRASLADO,TOUR,INTERPROVINCIAL', NULL, ADD_MONTHS(TRUNC(SYSDATE), -36));
INSERT INTO PROVEEDORES (proveedor_id, nombre_comercial, ruc, telefono, es_propio, estado, direccion, email, contacto, servicios, zonas, fecha_afiliacion) VALUES (2, 'Transportes Inka Express SAC', '20512345678', '+51 984100200', 0, 'ACTIVO', 'Av. La Cultura 1240, Cusco', 'reservas@inkaexpress.pe', 'Karina Huillca', 'TRASLADO,TOUR,INTERPROVINCIAL', 'Cusco,Ollantaytambo,Puno', ADD_MONTHS(TRUNC(SYSDATE), -24));
INSERT INTO PROVEEDORES (proveedor_id, nombre_comercial, ruc, telefono, es_propio, estado, direccion, email, contacto, servicios, zonas, fecha_afiliacion) VALUES (3, 'Lima Tours Movilidad SAC', '20601234567', '+51 014223344', 0, 'ACTIVO', 'Av. Larco 345, Miraflores, Lima', 'operaciones@limamovilidad.pe', 'Jorge Salas', 'TRASLADO,TOUR,INTERPROVINCIAL', 'Lima,Paracas,Ica', ADD_MONTHS(TRUNC(SYSDATE), -18));
INSERT INTO PROVEEDORES (proveedor_id, nombre_comercial, ruc, telefono, es_propio, estado, direccion, email, contacto, servicios, zonas, fecha_afiliacion) VALUES (4, 'Colca Bus EIRL', '20498765432', '+51 959887766', 0, 'ACTIVO', 'Calle Mercaderes 210, Arequipa', 'contacto@colcabus.pe', 'Ana Zevallos', 'TRASLADO,TOUR', 'Arequipa,Caylloma', ADD_MONTHS(TRUNC(SYSDATE), -12));
INSERT INTO PROVEEDORES (proveedor_id, nombre_comercial, ruc, telefono, es_propio, estado, direccion, email, contacto, servicios, zonas, fecha_afiliacion) VALUES (5, 'Selva Trans SRL', '20187654321', '+51 982554433', 0, 'SUSPENDIDO', 'Jr. Loreto 180, Puerto Maldonado', 'selvatrans@correo.pe', 'Raúl Shapiama', 'TRASLADO', 'Puerto Maldonado', ADD_MONTHS(TRUNC(SYSDATE), -10));
INSERT INTO PROVEEDORES (proveedor_id, nombre_comercial, ruc, telefono, es_propio, estado, direccion, email, contacto, servicios, zonas, fecha_afiliacion) VALUES (6, 'Andes Movilidad Turística EIRL', '20611223344', '+51 984777100', 0, 'PENDIENTE', 'Av. Garcilaso 455, Cusco', 'afiliacion@andesmovilidad.pe', 'Pedro Ccahuana', 'TRASLADO,TOUR', 'Cusco,Ollantaytambo', TRUNC(SYSDATE) - 2);

-- Cuentas de las empresas proveedoras (rol PROVEEDOR): administran su flota y responden los servicios
INSERT INTO USUARIOS_SISTEMA (usuario_id, username, password_hash, rol, estado, proveedor_id) VALUES (4, 'inkaexpress', 'inka123', 'PROVEEDOR', 'ACTIVO', 2);
INSERT INTO USUARIOS_SISTEMA (usuario_id, username, password_hash, rol, estado, proveedor_id) VALUES (5, 'limamovilidad', 'lima123', 'PROVEEDOR', 'ACTIVO', 3);
INSERT INTO USUARIOS_SISTEMA (usuario_id, username, password_hash, rol, estado, proveedor_id) VALUES (6, 'andesmovilidad', 'andes123', 'PROVEEDOR', 'ACTIVO', 6);

-- ---------------------------------------------------------------------
-- Lugares con coordenadas reales (para calcular duraciones)
-- ---------------------------------------------------------------------
INSERT INTO LUGARES (lugar_id, nombre, tipo_lugar, ciudad, latitud, longitud) VALUES (1, 'Aeropuerto Alejandro Velasco Astete', 'AEROPUERTO', 'Cusco', -13.535700, -71.938800);
INSERT INTO LUGARES (lugar_id, nombre, tipo_lugar, ciudad, latitud, longitud) VALUES (2, 'Hotel Plaza de Armas', 'HOTEL', 'Cusco', -13.516600, -71.978500);
INSERT INTO LUGARES (lugar_id, nombre, tipo_lugar, ciudad, latitud, longitud) VALUES (3, 'Estación Ollantaytambo', 'TERMINAL', 'Ollantaytambo', -13.263300, -72.265000);
INSERT INTO LUGARES (lugar_id, nombre, tipo_lugar, ciudad, latitud, longitud) VALUES (4, 'Aeropuerto Jorge Chávez', 'AEROPUERTO', 'Lima', -12.021900, -77.114300);
INSERT INTO LUGARES (lugar_id, nombre, tipo_lugar, ciudad, latitud, longitud) VALUES (5, 'Hotel Miraflores Park', 'HOTEL', 'Lima', -12.130600, -77.033600);
INSERT INTO LUGARES (lugar_id, nombre, tipo_lugar, ciudad, latitud, longitud) VALUES (6, 'Barranco', 'ATRACTIVO', 'Lima', -12.149100, -77.021700);
INSERT INTO LUGARES (lugar_id, nombre, tipo_lugar, ciudad, latitud, longitud) VALUES (7, 'Aeropuerto Rodríguez Ballón', 'AEROPUERTO', 'Arequipa', -16.341100, -71.583100);
INSERT INTO LUGARES (lugar_id, nombre, tipo_lugar, ciudad, latitud, longitud) VALUES (8, 'Hotel Casona Arequipa', 'HOTEL', 'Arequipa', -16.398800, -71.536900);
INSERT INTO LUGARES (lugar_id, nombre, tipo_lugar, ciudad, latitud, longitud) VALUES (9, 'Chivay', 'CIUDAD', 'Caylloma', -15.638000, -71.601700);
INSERT INTO LUGARES (lugar_id, nombre, tipo_lugar, ciudad, latitud, longitud) VALUES (10, 'Hotel Paracas Bay', 'HOTEL', 'Paracas', -13.832600, -76.250300);
INSERT INTO LUGARES (lugar_id, nombre, tipo_lugar, ciudad, latitud, longitud) VALUES (11, 'Oasis de Huacachina', 'ATRACTIVO', 'Ica', -14.087500, -75.762600);
INSERT INTO LUGARES (lugar_id, nombre, tipo_lugar, ciudad, latitud, longitud) VALUES (12, 'Aeropuerto Padre Aldamiz', 'AEROPUERTO', 'Puerto Maldonado', -12.613600, -69.228600);
INSERT INTO LUGARES (lugar_id, nombre, tipo_lugar, ciudad, latitud, longitud) VALUES (13, 'Tambopata Eco Lodge', 'HOTEL', 'Puerto Maldonado', -12.833300, -69.291700);
INSERT INTO LUGARES (lugar_id, nombre, tipo_lugar, ciudad, latitud, longitud) VALUES (14, 'Puerto de Puno', 'TERMINAL', 'Puno', -15.840200, -70.021900);

-- ---------------------------------------------------------------------
-- Personal: guías y conductores (de Kampu y de los proveedores)
-- ---------------------------------------------------------------------
INSERT INTO PERSONAL (personal_id, proveedor_id, nombre_completo, rol, telefono, num_licencia, cat_licencia, venc_licencia, estado)
VALUES (1, 1, 'Carlos Quispe', 'GUIA', '984112233', NULL, NULL, NULL, 'DISPONIBLE');
INSERT INTO PERSONAL (personal_id, proveedor_id, nombre_completo, rol, telefono, num_licencia, cat_licencia, venc_licencia, estado)
VALUES (2, 1, 'María Torres', 'GUIA', '991445120', NULL, NULL, NULL, 'DISPONIBLE');
INSERT INTO PERSONAL (personal_id, proveedor_id, nombre_completo, rol, telefono, num_licencia, cat_licencia, venc_licencia, estado)
VALUES (3, 1, 'Luis Valdivia', 'AMBOS', '958302771', 'V40552311', 'A-IIb', ADD_MONTHS(TRUNC(SYSDATE), 30), 'DISPONIBLE');
INSERT INTO PERSONAL (personal_id, proveedor_id, nombre_completo, rol, telefono, num_licencia, cat_licencia, venc_licencia, estado)
VALUES (4, 1, 'Rosa Huamán', 'GUIA', '976880415', NULL, NULL, NULL, 'DISPONIBLE');
INSERT INTO PERSONAL (personal_id, proveedor_id, nombre_completo, rol, telefono, num_licencia, cat_licencia, venc_licencia, estado)
VALUES (5, 1, 'Julio Quispe Mamani', 'CONDUCTOR', '+51 984223300', 'Q45871236', 'A-IIIc', ADD_MONTHS(TRUNC(SYSDATE), 18), 'DISPONIBLE');
INSERT INTO PERSONAL (personal_id, proveedor_id, nombre_completo, rol, telefono, num_licencia, cat_licencia, venc_licencia, estado)
VALUES (6, 2, 'Wilber Tito Yupanqui', 'CONDUCTOR', '+51 984556677', 'T47125836', 'A-IIIc', ADD_MONTHS(TRUNC(SYSDATE), 12), 'DISPONIBLE');
INSERT INTO PERSONAL (personal_id, proveedor_id, nombre_completo, rol, telefono, num_licencia, cat_licencia, venc_licencia, estado)
VALUES (7, 2, 'Rosa Huamán Ccori', 'CONDUCTOR', '+51 984223344', 'H71452368', 'A-IIb', ADD_MONTHS(TRUNC(SYSDATE), 10), 'DISPONIBLE');
INSERT INTO PERSONAL (personal_id, proveedor_id, nombre_completo, rol, telefono, num_licencia, cat_licencia, venc_licencia, estado)
VALUES (8, 3, 'Marco Condori Apaza', 'CONDUCTOR', '+51 984334455', 'C43698521', 'A-IIIb', ADD_MONTHS(TRUNC(SYSDATE), 14), 'DISPONIBLE');
INSERT INTO PERSONAL (personal_id, proveedor_id, nombre_completo, rol, telefono, num_licencia, cat_licencia, venc_licencia, estado)
VALUES (9, 3, 'Elmer Choque Puma', 'CONDUCTOR', '+51 984445566', 'C42587136', 'A-IIb', ADD_MONTHS(TRUNC(SYSDATE), 8), 'OCUPADO');
INSERT INTO PERSONAL (personal_id, proveedor_id, nombre_completo, rol, telefono, num_licencia, cat_licencia, venc_licencia, estado)
VALUES (10, 4, 'Lucio Mamani Flores', 'CONDUCTOR', '+51 959112233', 'M40125879', 'A-IIIb', ADD_MONTHS(TRUNC(SYSDATE), 20), 'DISPONIBLE');
INSERT INTO PERSONAL (personal_id, proveedor_id, nombre_completo, rol, telefono, num_licencia, cat_licencia, venc_licencia, estado)
VALUES (11, 5, 'Raúl Shapiama Tuesta', 'CONDUCTOR', '+51 982554400', 'S46325871', 'A-IIb', ADD_MONTHS(TRUNC(SYSDATE), 16), 'DISPONIBLE');

-- ---------------------------------------------------------------------
-- Vehículos (Kampu y proveedores)
-- ---------------------------------------------------------------------
INSERT INTO VEHICULOS (vehiculo_id, proveedor_id, placa, tipo_vehiculo, marca_modelo, capacidad_pax, prox_mantenimiento, estado) VALUES (1, 1, 'VAN-152', 'VAN', 'Toyota Hiace', 15, ADD_MONTHS(TRUNC(SYSDATE), 4), 'OPERATIVO');
INSERT INTO VEHICULOS (vehiculo_id, proveedor_id, placa, tipo_vehiculo, marca_modelo, capacidad_pax, prox_mantenimiento, estado) VALUES (2, 1, 'CAM-084', 'CAMIONETA_4X4', 'Toyota Hilux 4x4', 4, ADD_MONTHS(TRUNC(SYSDATE), 2), 'OPERATIVO');
INSERT INTO VEHICULOS (vehiculo_id, proveedor_id, placa, tipo_vehiculo, marca_modelo, capacidad_pax, prox_mantenimiento, estado) VALUES (3, 2, 'BUS-401', 'BUS', 'Mercedes-Benz O500', 40, ADD_MONTHS(TRUNC(SYSDATE), 7), 'OPERATIVO');
INSERT INTO VEHICULOS (vehiculo_id, proveedor_id, placa, tipo_vehiculo, marca_modelo, capacidad_pax, prox_mantenimiento, estado) VALUES (4, 3, 'BUS-302', 'BUS', 'Volvo B7R', 30, ADD_MONTHS(TRUNC(SYSDATE), 6), 'OPERATIVO');
INSERT INTO VEHICULOS (vehiculo_id, proveedor_id, placa, tipo_vehiculo, marca_modelo, capacidad_pax, prox_mantenimiento, estado) VALUES (5, 3, 'VAN-210', 'VAN', 'Hyundai H1', 10, TRUNC(SYSDATE) + 40, 'OPERATIVO');
INSERT INTO VEHICULOS (vehiculo_id, proveedor_id, placa, tipo_vehiculo, marca_modelo, capacidad_pax, prox_mantenimiento, estado) VALUES (6, 4, 'MIN-077', 'MINIBUS', 'Mercedes-Benz Sprinter', 20, ADD_MONTHS(TRUNC(SYSDATE), 3), 'OPERATIVO');
INSERT INTO VEHICULOS (vehiculo_id, proveedor_id, placa, tipo_vehiculo, marca_modelo, capacidad_pax, prox_mantenimiento, estado) VALUES (7, 4, 'BUS-509', 'BUS', 'Scania K360', 45, TRUNC(SYSDATE) + 30, 'MANTENIMIENTO');
INSERT INTO VEHICULOS (vehiculo_id, proveedor_id, placa, tipo_vehiculo, marca_modelo, capacidad_pax, prox_mantenimiento, estado) VALUES (8, 5, 'VAN-330', 'VAN', 'Toyota Hiace', 12, ADD_MONTHS(TRUNC(SYSDATE), 5), 'OPERATIVO');

-- ---------------------------------------------------------------------
-- Paquetes
-- ---------------------------------------------------------------------
INSERT INTO PAQUETES_TOUR (paquete_id, nombre, descripcion, precio_base, dias, noches, imagen, estado) VALUES (1, 'Machu Picchu Clásico', 'Valle Sagrado, tren a Aguas Calientes y visita guiada a la ciudadela.', 1850, 4, 3, NULL, 'ACTIVO');
INSERT INTO PAQUETES_TOUR (paquete_id, nombre, descripcion, precio_base, dias, noches, imagen, estado) VALUES (2, 'Lima Gastronómica', 'Centro histórico, mercados, ceviche y degustación en Barranco.', 420, 2, 1, NULL, 'ACTIVO');
INSERT INTO PAQUETES_TOUR (paquete_id, nombre, descripcion, precio_base, dias, noches, imagen, estado) VALUES (3, 'Arequipa y Cañón del Colca', 'Ciudad blanca, Cruz del Cóndor y termas de Chivay.', 980, 3, 2, NULL, 'ACTIVO');
INSERT INTO PAQUETES_TOUR (paquete_id, nombre, descripcion, precio_base, dias, noches, imagen, estado) VALUES (4, 'Paracas e Ica Aventura', 'Islas Ballestas, reserva de Paracas, Huacachina y sandboard.', 380, 2, 1, NULL, 'ACTIVO');
INSERT INTO PAQUETES_TOUR (paquete_id, nombre, descripcion, precio_base, dias, noches, imagen, estado) VALUES (5, 'Amazonía Tambopata', 'Lodge en la selva, lago Sandoval y avistamiento de fauna.', 1650, 4, 3, NULL, 'ACTIVO');
INSERT INTO PAQUETES_TOUR (paquete_id, nombre, descripcion, precio_base, dias, noches, imagen, estado) VALUES (6, 'Lago Titicaca y Puno', 'Islas flotantes de los Uros, Taquile y Sillustani.', 890, 3, 2, NULL, 'INACTIVO');

-- ---------------------------------------------------------------------
-- Salidas (fecha_fin = inicio + días - 1; cupos_disponibles = máximos - vendidos)
-- ---------------------------------------------------------------------
INSERT INTO SALIDAS_PROGRAMADAS (salida_id, paquete_id, guia_id, fecha_inicio, fecha_fin, cupos_maximos, cupos_disponibles, estado) VALUES (1, 1, 1, TRUNC(SYSDATE) - 1,  TRUNC(SYSDATE) + 2,  20, 17, 'CONFIRMADA');
INSERT INTO SALIDAS_PROGRAMADAS (salida_id, paquete_id, guia_id, fecha_inicio, fecha_fin, cupos_maximos, cupos_disponibles, estado) VALUES (2, 1, 1, TRUNC(SYSDATE) + 30, TRUNC(SYSDATE) + 33, 20, 18, 'PROGRAMADA');
INSERT INTO SALIDAS_PROGRAMADAS (salida_id, paquete_id, guia_id, fecha_inicio, fecha_fin, cupos_maximos, cupos_disponibles, estado) VALUES (3, 2, 2, TRUNC(SYSDATE) + 4,  TRUNC(SYSDATE) + 5,  15, 12, 'PROGRAMADA');
INSERT INTO SALIDAS_PROGRAMADAS (salida_id, paquete_id, guia_id, fecha_inicio, fecha_fin, cupos_maximos, cupos_disponibles, estado) VALUES (4, 3, 3, TRUNC(SYSDATE) + 10, TRUNC(SYSDATE) + 12, 16, 16, 'PROGRAMADA');
INSERT INTO SALIDAS_PROGRAMADAS (salida_id, paquete_id, guia_id, fecha_inicio, fecha_fin, cupos_maximos, cupos_disponibles, estado) VALUES (5, 4, 4, TRUNC(SYSDATE) + 1,  TRUNC(SYSDATE) + 2,  25, 21, 'PROGRAMADA');
INSERT INTO SALIDAS_PROGRAMADAS (salida_id, paquete_id, guia_id, fecha_inicio, fecha_fin, cupos_maximos, cupos_disponibles, estado) VALUES (6, 5, 4, TRUNC(SYSDATE) + 23, TRUNC(SYSDATE) + 26, 12, 10, 'PROGRAMADA');
INSERT INTO SALIDAS_PROGRAMADAS (salida_id, paquete_id, guia_id, fecha_inicio, fecha_fin, cupos_maximos, cupos_disponibles, estado) VALUES (7, 1, 1, TRUNC(SYSDATE) - 33, TRUNC(SYSDATE) - 30, 20, 18, 'COMPLETADA');
INSERT INTO SALIDAS_PROGRAMADAS (salida_id, paquete_id, guia_id, fecha_inicio, fecha_fin, cupos_maximos, cupos_disponibles, estado) VALUES (8, 3, 3, TRUNC(SYSDATE) - 20, TRUNC(SYSDATE) - 18, 16, 13, 'COMPLETADA');
INSERT INTO SALIDAS_PROGRAMADAS (salida_id, paquete_id, guia_id, fecha_inicio, fecha_fin, cupos_maximos, cupos_disponibles, estado) VALUES (9, 6, 4, TRUNC(SYSDATE) + 66, TRUNC(SYSDATE) + 68, 18, 18, 'CANCELADA');

-- ---------------------------------------------------------------------
-- Reservas (se crean PENDIENTES; los estados finales se fijan al final,
-- para que TRG_VALIDAR_PAGO_PAQUETE acepte sus pagos)
-- ---------------------------------------------------------------------
INSERT INTO RESERVAS (reserva_id, cliente_id, fecha_reserva, monto_total, observaciones) VALUES (1, 1, SYSTIMESTAMP - INTERVAL '60' DAY, 3700, NULL);
INSERT INTO RESERVAS (reserva_id, cliente_id, fecha_reserva, monto_total, observaciones) VALUES (2, 2, SYSTIMESTAMP - INTERVAL '45' DAY, 2940, 'Un menor de edad en el grupo.');
INSERT INTO RESERVAS (reserva_id, cliente_id, fecha_reserva, monto_total, observaciones) VALUES (3, 3, SYSTIMESTAMP - INTERVAL '30' DAY, 3700, 'Habitación matrimonial.');
INSERT INTO RESERVAS (reserva_id, cliente_id, fecha_reserva, monto_total, observaciones) VALUES (4, 4, SYSTIMESTAMP - INTERVAL '20' DAY, 1850, NULL);
INSERT INTO RESERVAS (reserva_id, cliente_id, fecha_reserva, monto_total, observaciones) VALUES (5, 5, SYSTIMESTAMP - INTERVAL '15' DAY, 1260, NULL);
INSERT INTO RESERVAS (reserva_id, cliente_id, fecha_reserva, monto_total, observaciones) VALUES (6, 6, SYSTIMESTAMP - INTERVAL '12' DAY, 3300, 'Dieta vegetariana.');
INSERT INTO RESERVAS (reserva_id, cliente_id, fecha_reserva, monto_total, observaciones) VALUES (7, 7, SYSTIMESTAMP - INTERVAL '25' DAY, 980, 'Anulada a pedido del cliente.');
INSERT INTO RESERVAS (reserva_id, cliente_id, fecha_reserva, monto_total, observaciones) VALUES (8, 9, SYSTIMESTAMP - INTERVAL '5' DAY, 1520, 'Grupo corporativo.');
INSERT INTO RESERVAS (reserva_id, cliente_id, fecha_reserva, monto_total, observaciones) VALUES (9, 1, SYSTIMESTAMP - INTERVAL '2' DAY, 3700, NULL);

INSERT INTO DETALLE_RESERVA (detalle_id, reserva_id, salida_id, cantidad, precio_unitario, descuento) VALUES (1, 1, 7, 2, 1850, 0);
INSERT INTO DETALLE_RESERVA (detalle_id, reserva_id, salida_id, cantidad, precio_unitario, descuento) VALUES (2, 2, 8, 3, 980, 0);
INSERT INTO DETALLE_RESERVA (detalle_id, reserva_id, salida_id, cantidad, precio_unitario, descuento) VALUES (3, 3, 1, 2, 1850, 0);
INSERT INTO DETALLE_RESERVA (detalle_id, reserva_id, salida_id, cantidad, precio_unitario, descuento) VALUES (4, 4, 1, 1, 1850, 0);
INSERT INTO DETALLE_RESERVA (detalle_id, reserva_id, salida_id, cantidad, precio_unitario, descuento) VALUES (5, 5, 3, 3, 420, 0);
INSERT INTO DETALLE_RESERVA (detalle_id, reserva_id, salida_id, cantidad, precio_unitario, descuento) VALUES (6, 6, 6, 2, 1650, 0);
INSERT INTO DETALLE_RESERVA (detalle_id, reserva_id, salida_id, cantidad, precio_unitario, descuento) VALUES (7, 7, 4, 1, 980, 0);
INSERT INTO DETALLE_RESERVA (detalle_id, reserva_id, salida_id, cantidad, precio_unitario, descuento) VALUES (8, 8, 5, 4, 380, 0);
INSERT INTO DETALLE_RESERVA (detalle_id, reserva_id, salida_id, cantidad, precio_unitario, descuento) VALUES (9, 9, 2, 2, 1850, 0);

INSERT INTO PASAJEROS_RESERVA (pasajero_id, detalle_id, nombre_completo, tipo_documento, numero_documento, nacionalidad, fecha_nacimiento) VALUES (1, 1, 'Ana Ramírez Soto', 'DNI', '45871236', 'Peruana', DATE '1990-04-12');
INSERT INTO PASAJEROS_RESERVA (pasajero_id, detalle_id, nombre_completo, tipo_documento, numero_documento, nacionalidad, fecha_nacimiento) VALUES (2, 1, 'Jorge Mendoza Paredes', 'DNI', '40125879', 'Peruana', DATE '1985-09-30');
INSERT INTO PASAJEROS_RESERVA (pasajero_id, detalle_id, nombre_completo, tipo_documento, numero_documento, nacionalidad, fecha_nacimiento) VALUES (3, 2, 'Lucía Fernández Cruz', 'DNI', '71452368', 'Peruana', DATE '1995-01-22');
INSERT INTO PASAJEROS_RESERVA (pasajero_id, detalle_id, nombre_completo, tipo_documento, numero_documento, nacionalidad, fecha_nacimiento) VALUES (4, 2, 'Mateo Fernández Cruz', 'DNI', '71452369', 'Peruana', DATE '2012-06-15');
INSERT INTO PASAJEROS_RESERVA (pasajero_id, detalle_id, nombre_completo, tipo_documento, numero_documento, nacionalidad, fecha_nacimiento) VALUES (5, 2, 'Carla Vega Ruiz', 'DNI', '46325871', 'Peruana', DATE '1992-11-03');
INSERT INTO PASAJEROS_RESERVA (pasajero_id, detalle_id, nombre_completo, tipo_documento, numero_documento, nacionalidad, fecha_nacimiento) VALUES (6, 3, 'John Miller', 'PASAPORTE', 'X4587126', 'Estadounidense', DATE '1980-02-18');
INSERT INTO PASAJEROS_RESERVA (pasajero_id, detalle_id, nombre_completo, tipo_documento, numero_documento, nacionalidad, fecha_nacimiento) VALUES (7, 3, 'Emily Miller', 'PASAPORTE', 'X4587127', 'Estadounidense', DATE '1983-07-09');
INSERT INTO PASAJEROS_RESERVA (pasajero_id, detalle_id, nombre_completo, tipo_documento, numero_documento, nacionalidad, fecha_nacimiento) VALUES (8, 4, 'Sofía Castillo Rojas', 'CE', '001245789', 'Colombiana', DATE '1998-12-25');
INSERT INTO PASAJEROS_RESERVA (pasajero_id, detalle_id, nombre_completo, tipo_documento, numero_documento, nacionalidad, fecha_nacimiento) VALUES (9, 5, 'Diego Salazar Luna', 'DNI', '43698521', 'Peruana', DATE '1988-03-07');
INSERT INTO PASAJEROS_RESERVA (pasajero_id, detalle_id, nombre_completo, tipo_documento, numero_documento, nacionalidad, fecha_nacimiento) VALUES (10, 5, 'Valeria Salazar Luna', 'DNI', '43698522', 'Peruana', DATE '1991-08-19');
INSERT INTO PASAJEROS_RESERVA (pasajero_id, detalle_id, nombre_completo, tipo_documento, numero_documento, nacionalidad, fecha_nacimiento) VALUES (11, 5, 'Camila Salazar Luna', 'DNI', '43698523', 'Peruana', DATE '2015-02-11');
INSERT INTO PASAJEROS_RESERVA (pasajero_id, detalle_id, nombre_completo, tipo_documento, numero_documento, nacionalidad, fecha_nacimiento) VALUES (12, 6, 'Pedro Huertas Vela', 'DNI', '42587136', 'Peruana', DATE '1979-05-30');
INSERT INTO PASAJEROS_RESERVA (pasajero_id, detalle_id, nombre_completo, tipo_documento, numero_documento, nacionalidad, fecha_nacimiento) VALUES (13, 6, 'Marta Huertas Vela', 'DNI', '42587137', 'Peruana', DATE '1982-10-14');
INSERT INTO PASAJEROS_RESERVA (pasajero_id, detalle_id, nombre_completo, tipo_documento, numero_documento, nacionalidad, fecha_nacimiento) VALUES (14, 7, 'Luis Paredes Gil', 'DNI', '47125836', 'Peruana', DATE '1986-01-09');
INSERT INTO PASAJEROS_RESERVA (pasajero_id, detalle_id, nombre_completo, tipo_documento, numero_documento, nacionalidad, fecha_nacimiento) VALUES (15, 8, 'Martín Ruiz Peña', 'DNI', '41258963', 'Peruana', DATE '1978-03-21');
INSERT INTO PASAJEROS_RESERVA (pasajero_id, detalle_id, nombre_completo, tipo_documento, numero_documento, nacionalidad, fecha_nacimiento) VALUES (16, 8, 'Carmen Ruiz Peña', 'DNI', '41258964', 'Peruana', DATE '1981-07-02');
INSERT INTO PASAJEROS_RESERVA (pasajero_id, detalle_id, nombre_completo, tipo_documento, numero_documento, nacionalidad, fecha_nacimiento) VALUES (17, 8, 'Rocío Ruiz Peña', 'DNI', '41258965', 'Peruana', DATE '2010-01-30');
INSERT INTO PASAJEROS_RESERVA (pasajero_id, detalle_id, nombre_completo, tipo_documento, numero_documento, nacionalidad, fecha_nacimiento) VALUES (18, 8, 'Iván Ruiz Peña', 'DNI', '41258966', 'Peruana', DATE '2013-05-14');
INSERT INTO PASAJEROS_RESERVA (pasajero_id, detalle_id, nombre_completo, tipo_documento, numero_documento, nacionalidad, fecha_nacimiento) VALUES (19, 9, 'Ana Ramírez Soto', 'DNI', '45871236', 'Peruana', DATE '1990-04-12');
INSERT INTO PASAJEROS_RESERVA (pasajero_id, detalle_id, nombre_completo, tipo_documento, numero_documento, nacionalidad, fecha_nacimiento) VALUES (20, 9, 'Bruno Ramírez Soto', 'DNI', '45871237', 'Peruana', DATE '1993-02-08');

-- Pagos (registrado_por = quién lo registró; tipo_cambio en soles)
INSERT INTO PAGOS (pago_id, reserva_id, monto, moneda, tipo_cambio, metodo_pago, codigo_transaccion, fecha_pago, estado_transaccion, registrado_por) VALUES (1, 1, 1850, 'PEN', 1, 'Transferencia', 'OP-883421', SYSTIMESTAMP - INTERVAL '60' DAY, 'APROBADO', 'VENDEDOR');
INSERT INTO PAGOS (pago_id, reserva_id, monto, moneda, tipo_cambio, metodo_pago, codigo_transaccion, fecha_pago, estado_transaccion, registrado_por) VALUES (2, 1, 1850, 'PEN', 1, 'Tarjeta', 'VISA-1127', SYSTIMESTAMP - INTERVAL '40' DAY, 'APROBADO', 'VENDEDOR');
INSERT INTO PAGOS (pago_id, reserva_id, monto, moneda, tipo_cambio, metodo_pago, codigo_transaccion, fecha_pago, estado_transaccion, registrado_por) VALUES (3, 2, 1470, 'PEN', 1, 'Yape', 'YP-55120', SYSTIMESTAMP - INTERVAL '45' DAY, 'APROBADO', 'VENDEDOR');
INSERT INTO PAGOS (pago_id, reserva_id, monto, moneda, tipo_cambio, metodo_pago, codigo_transaccion, fecha_pago, estado_transaccion, registrado_por) VALUES (4, 2, 1470, 'PEN', 1, 'Efectivo', 'EF-00012', SYSTIMESTAMP - INTERVAL '25' DAY, 'APROBADO', 'ADMIN');
INSERT INTO PAGOS (pago_id, reserva_id, monto, moneda, tipo_cambio, metodo_pago, codigo_transaccion, fecha_pago, estado_transaccion, registrado_por) VALUES (5, 3, 1850, 'PEN', 1, 'Tarjeta', 'MC-9034', SYSTIMESTAMP - INTERVAL '30' DAY, 'APROBADO', 'VENDEDOR');
INSERT INTO PAGOS (pago_id, reserva_id, monto, moneda, tipo_cambio, metodo_pago, codigo_transaccion, fecha_pago, estado_transaccion, registrado_por) VALUES (6, 3, 500, 'PEN', 1, 'Transferencia', 'BCP-77120', SYSTIMESTAMP - INTERVAL '1' DAY, 'PENDIENTE', 'CLIENTE');
INSERT INTO PAGOS (pago_id, reserva_id, monto, moneda, tipo_cambio, metodo_pago, codigo_transaccion, fecha_pago, estado_transaccion, registrado_por) VALUES (7, 4, 300, 'PEN', 1, 'Tarjeta', 'VISA-7781', SYSTIMESTAMP - INTERVAL '20' DAY, 'RECHAZADO', 'VENDEDOR');
INSERT INTO PAGOS (pago_id, reserva_id, monto, moneda, tipo_cambio, metodo_pago, codigo_transaccion, fecha_pago, estado_transaccion, registrado_por) VALUES (8, 4, 925, 'PEN', 1, 'Transferencia', 'OP-901122', SYSTIMESTAMP - INTERVAL '10' DAY, 'PENDIENTE', 'VENDEDOR');
INSERT INTO PAGOS (pago_id, reserva_id, monto, moneda, tipo_cambio, metodo_pago, codigo_transaccion, fecha_pago, estado_transaccion, registrado_por) VALUES (9, 5, 630, 'PEN', 1, 'Efectivo', 'EF-00013', SYSTIMESTAMP - INTERVAL '15' DAY, 'APROBADO', 'VENDEDOR');
INSERT INTO PAGOS (pago_id, reserva_id, monto, moneda, tipo_cambio, metodo_pago, codigo_transaccion, fecha_pago, estado_transaccion, registrado_por) VALUES (10, 6, 264, 'USD', 3.75, 'Transferencia', 'OP-902277', SYSTIMESTAMP - INTERVAL '12' DAY, 'APROBADO', 'VENDEDOR');
INSERT INTO PAGOS (pago_id, reserva_id, monto, moneda, tipo_cambio, metodo_pago, codigo_transaccion, fecha_pago, estado_transaccion, registrado_por) VALUES (11, 7, 300, 'PEN', 1, 'Yape', 'YP-54001', SYSTIMESTAMP - INTERVAL '25' DAY, 'APROBADO', 'VENDEDOR');
INSERT INTO PAGOS (pago_id, reserva_id, monto, moneda, tipo_cambio, metodo_pago, codigo_transaccion, fecha_pago, estado_transaccion, registrado_por) VALUES (12, 8, 760, 'PEN', 1, 'Transferencia', 'OP-903310', SYSTIMESTAMP - INTERVAL '5' DAY, 'APROBADO', 'VENDEDOR');

-- Montos pagados (suma de pagos APROBADOS) y estados; los cambios quedan en AUDITORIA
UPDATE RESERVAS SET monto_pagado = 3700, estado_pago = 'PAGADO',  estado_reserva = 'COMPLETADA' WHERE reserva_id = 1;
UPDATE RESERVAS SET monto_pagado = 2940, estado_pago = 'PAGADO',  estado_reserva = 'COMPLETADA' WHERE reserva_id = 2;
UPDATE RESERVAS SET monto_pagado = 1850, estado_pago = 'PARCIAL', estado_reserva = 'CONFIRMADA' WHERE reserva_id = 3;
UPDATE RESERVAS SET monto_pagado = 630,  estado_pago = 'PARCIAL', estado_reserva = 'CONFIRMADA' WHERE reserva_id = 5;
UPDATE RESERVAS SET monto_pagado = 990,  estado_pago = 'PARCIAL'                                WHERE reserva_id = 6;
UPDATE RESERVAS SET monto_pagado = 760,  estado_pago = 'PARCIAL', estado_reserva = 'CONFIRMADA' WHERE reserva_id = 8;
-- Anulación de la reserva 7: el pago se reembolsa y el cupo ya está devuelto en la salida 4
UPDATE PAGOS    SET estado_transaccion = 'REEMBOLSADO' WHERE pago_id = 11;
UPDATE RESERVAS SET estado_reserva = 'ANULADA', estado_pago = 'REEMBOLSADO', forma_reembolso = 'Yape', usuario_anula_id = 1 WHERE reserva_id = 7;

-- ---------------------------------------------------------------------
-- Operaciones de transporte (código KMP-### de SEQ_CODIGO_OPERACION) e historial
-- ---------------------------------------------------------------------
INSERT INTO OPERACIONES_TRANSPORTE (operacion_id, codigo_operacion, salida_id, origen_id, destino_id, vehiculo_id, conductor_id, pax_programados, hora_salida, hora_llegada, estado, observaciones, tipo_servicio, confirmacion)
VALUES (1, 'KMP-' || LPAD(SEQ_CODIGO_OPERACION.NEXTVAL, 3, '0'), 7, 1, 2, 3, 6, 2,
        CAST(TRUNC(SYSDATE) - 33 AS TIMESTAMP) + INTERVAL '9' HOUR, CAST(TRUNC(SYSDATE) - 33 AS TIMESTAMP) + INTERVAL '9:20' HOUR TO MINUTE, 'FINALIZADO', 'Recojo en aeropuerto', 'TRASLADO', 'ACEPTADO');
INSERT INTO OPERACIONES_TRANSPORTE (operacion_id, codigo_operacion, salida_id, origen_id, destino_id, vehiculo_id, conductor_id, pax_programados, hora_salida, hora_llegada, estado, observaciones, tipo_servicio, confirmacion)
VALUES (2, 'KMP-' || LPAD(SEQ_CODIGO_OPERACION.NEXTVAL, 3, '0'), 8, 7, 8, 6, 10, 3,
        CAST(TRUNC(SYSDATE) - 20 AS TIMESTAMP) + INTERVAL '10' HOUR, CAST(TRUNC(SYSDATE) - 20 AS TIMESTAMP) + INTERVAL '10:25' HOUR TO MINUTE, 'FINALIZADO', NULL, 'TRASLADO', 'ACEPTADO');
INSERT INTO OPERACIONES_TRANSPORTE (operacion_id, codigo_operacion, salida_id, origen_id, destino_id, vehiculo_id, conductor_id, pax_programados, hora_salida, hora_llegada, estado, observaciones, tipo_servicio, confirmacion)
VALUES (3, 'KMP-' || LPAD(SEQ_CODIGO_OPERACION.NEXTVAL, 3, '0'), 8, 8, 9, 6, 10, 3,
        CAST(TRUNC(SYSDATE) - 19 AS TIMESTAMP) + INTERVAL '6' HOUR, CAST(TRUNC(SYSDATE) - 19 AS TIMESTAMP) + INTERVAL '9' HOUR, 'FINALIZADO', NULL, 'TOUR', 'ACEPTADO');
INSERT INTO OPERACIONES_TRANSPORTE (operacion_id, codigo_operacion, salida_id, origen_id, destino_id, vehiculo_id, conductor_id, pax_programados, hora_salida, hora_llegada, estado, observaciones, tipo_servicio, confirmacion)
VALUES (4, 'KMP-' || LPAD(SEQ_CODIGO_OPERACION.NEXTVAL, 3, '0'), 1, 1, 2, 1, 5, 3,
        CAST(TRUNC(SYSDATE) - 1 AS TIMESTAMP) + INTERVAL '9' HOUR, CAST(TRUNC(SYSDATE) - 1 AS TIMESTAMP) + INTERVAL '9:20' HOUR TO MINUTE, 'FINALIZADO', NULL, 'TRASLADO', 'ACEPTADO');
INSERT INTO OPERACIONES_TRANSPORTE (operacion_id, codigo_operacion, salida_id, origen_id, destino_id, vehiculo_id, conductor_id, pax_programados, hora_salida, hora_llegada, estado, observaciones, tipo_servicio, confirmacion)
VALUES (5, 'KMP-' || LPAD(SEQ_CODIGO_OPERACION.NEXTVAL, 3, '0'), 1, 2, 3, 1, 5, 3,
        CAST(TRUNC(SYSDATE) AS TIMESTAMP) + INTERVAL '7' HOUR, CAST(TRUNC(SYSDATE) AS TIMESTAMP) + INTERVAL '8:50' HOUR TO MINUTE, 'EN_RUTA', 'Tour al Valle Sagrado', 'TOUR', 'ACEPTADO');
INSERT INTO OPERACIONES_TRANSPORTE (operacion_id, codigo_operacion, salida_id, origen_id, destino_id, vehiculo_id, conductor_id, pax_programados, hora_salida, hora_llegada, estado, observaciones, tipo_servicio, confirmacion)
VALUES (6, 'KMP-' || LPAD(SEQ_CODIGO_OPERACION.NEXTVAL, 3, '0'), 3, 4, 5, 5, 8, 3,
        CAST(TRUNC(SYSDATE) + 4 AS TIMESTAMP) + INTERVAL '8' HOUR, CAST(TRUNC(SYSDATE) + 4 AS TIMESTAMP) + INTERVAL '8:50' HOUR TO MINUTE, 'ASIGNADO', NULL, 'TRASLADO', 'PENDIENTE');
INSERT INTO OPERACIONES_TRANSPORTE (operacion_id, codigo_operacion, salida_id, origen_id, destino_id, vehiculo_id, conductor_id, pax_programados, hora_salida, hora_llegada, estado, observaciones, tipo_servicio, confirmacion)
VALUES (7, 'KMP-' || LPAD(SEQ_CODIGO_OPERACION.NEXTVAL, 3, '0'), 5, 5, 10, NULL, NULL, 4,
        CAST(TRUNC(SYSDATE) + 1 AS TIMESTAMP) + INTERVAL '6:30' HOUR TO MINUTE, CAST(TRUNC(SYSDATE) + 1 AS TIMESTAMP) + INTERVAL '10:15' HOUR TO MINUTE, 'PROGRAMADO', 'Salida temprano por la Panamericana Sur', 'INTERPROVINCIAL', NULL);
INSERT INTO OPERACIONES_TRANSPORTE (operacion_id, codigo_operacion, salida_id, origen_id, destino_id, vehiculo_id, conductor_id, pax_programados, hora_salida, hora_llegada, estado, observaciones, tipo_servicio, confirmacion)
VALUES (8, 'KMP-' || LPAD(SEQ_CODIGO_OPERACION.NEXTVAL, 3, '0'), 2, 1, 2, NULL, NULL, 2,
        CAST(TRUNC(SYSDATE) + 30 AS TIMESTAMP) + INTERVAL '10' HOUR, CAST(TRUNC(SYSDATE) + 30 AS TIMESTAMP) + INTERVAL '10:20' HOUR TO MINUTE, 'PROGRAMADO', NULL, 'TRASLADO', NULL);
INSERT INTO OPERACIONES_TRANSPORTE (operacion_id, codigo_operacion, salida_id, origen_id, destino_id, vehiculo_id, conductor_id, pax_programados, hora_salida, hora_llegada, estado, observaciones, tipo_servicio, confirmacion)
VALUES (9, 'KMP-' || LPAD(SEQ_CODIGO_OPERACION.NEXTVAL, 3, '0'), 6, 12, 13, NULL, NULL, 2,
        CAST(TRUNC(SYSDATE) + 23 AS TIMESTAMP) + INTERVAL '11' HOUR, CAST(TRUNC(SYSDATE) + 23 AS TIMESTAMP) + INTERVAL '13' HOUR, 'PROGRAMADO', 'Traslado en bote incluido', 'TRASLADO', NULL);

-- Operaciones de hoy para el tablero de salidas y llegadas (horas relativas al momento de la instalación)
INSERT INTO OPERACIONES_TRANSPORTE (operacion_id, codigo_operacion, salida_id, origen_id, destino_id, vehiculo_id, conductor_id, pax_programados, hora_salida, hora_llegada, estado, observaciones, tipo_servicio, confirmacion, ruta, hora_salida_estimada)
VALUES (10, 'KMP-' || LPAD(SEQ_CODIGO_OPERACION.NEXTVAL, 3, '0'), NULL, 4, 5, 4, 8, 18,
        CAST(TRUNC(SYSDATE, 'HH24') AS TIMESTAMP) + INTERVAL '1' HOUR, CAST(TRUNC(SYSDATE, 'HH24') AS TIMESTAMP) + INTERVAL '2:45' HOUR TO MINUTE,
        'RETRASADO', 'Vuelo LA2041 llega con demora', 'TRASLADO', 'ACEPTADO', 'Av. Elmer Faucett / Costa Verde',
        CAST(TRUNC(SYSDATE, 'HH24') AS TIMESTAMP) + INTERVAL '1:40' HOUR TO MINUTE);
INSERT INTO OPERACIONES_TRANSPORTE (operacion_id, codigo_operacion, salida_id, origen_id, destino_id, vehiculo_id, conductor_id, pax_programados, hora_salida, hora_llegada, estado, observaciones, tipo_servicio, confirmacion, ruta)
VALUES (11, 'KMP-' || LPAD(SEQ_CODIGO_OPERACION.NEXTVAL, 3, '0'), NULL, 8, 7, 6, 10, 9,
        CAST(TRUNC(SYSDATE, 'HH24') AS TIMESTAMP) - INTERVAL '30' MINUTE, CAST(TRUNC(SYSDATE, 'HH24') AS TIMESTAMP) + INTERVAL '10' MINUTE,
        'LLEGADA_PROXIMA', 'Grupo con vuelo de salida', 'TRASLADO', 'ACEPTADO', 'Av. Aviación');
INSERT INTO OPERACIONES_TRANSPORTE (operacion_id, codigo_operacion, salida_id, origen_id, destino_id, vehiculo_id, conductor_id, pax_programados, hora_salida, hora_llegada, estado, observaciones, tipo_servicio, confirmacion, ruta)
VALUES (12, 'KMP-' || LPAD(SEQ_CODIGO_OPERACION.NEXTVAL, 3, '0'), NULL, 2, 1, 2, 3, 3,
        CAST(TRUNC(SYSDATE, 'HH24') AS TIMESTAMP) + INTERVAL '20' MINUTE, CAST(TRUNC(SYSDATE, 'HH24') AS TIMESTAMP) + INTERVAL '50' MINUTE,
        'EN_ESPERA_SALIDA', 'Pasajeros ya a bordo', 'TRASLADO', 'ACEPTADO', 'Av. El Sol');
INSERT INTO OPERACIONES_TRANSPORTE (operacion_id, codigo_operacion, salida_id, origen_id, destino_id, vehiculo_id, conductor_id, pax_programados, hora_salida, hora_llegada, estado, observaciones, tipo_servicio, confirmacion, ruta, tipo_vehiculo_req)
VALUES (13, 'KMP-' || LPAD(SEQ_CODIGO_OPERACION.NEXTVAL, 3, '0'), NULL, 4, 6, NULL, NULL, 6,
        CAST(TRUNC(SYSDATE, 'HH24') AS TIMESTAMP) + INTERVAL '4' HOUR, CAST(TRUNC(SYSDATE, 'HH24') AS TIMESTAMP) + INTERVAL '5:10' HOUR TO MINUTE,
        'PROGRAMADO', 'Familia con equipaje voluminoso', 'TRASLADO', NULL, 'Circuito de Playas', 'VAN');

-- Historial de estados (en la aplicación lo registra el backend en cada cambio)
INSERT INTO HISTORIAL_OPERACIONES (operacion_id, estado_nuevo, fecha_cambio, usuario, observacion)
SELECT o.operacion_id, e.estado,
       o.hora_salida + NUMTODSINTERVAL(e.minutos, 'MINUTE'), 'operador', NULL
  FROM OPERACIONES_TRANSPORTE o
  JOIN (SELECT 'PROGRAMADO' estado, -4320 minutos, 1 orden FROM dual UNION ALL
        SELECT 'ASIGNADO',  -2880, 2 FROM dual UNION ALL
        SELECT 'EMBARQUE',    -10, 3 FROM dual UNION ALL
        SELECT 'EN_RUTA',       3, 4 FROM dual UNION ALL
        SELECT 'FINALIZADO',   25, 5 FROM dual) e
    ON e.orden <= CASE o.estado WHEN 'PROGRAMADO' THEN 1 WHEN 'ASIGNADO' THEN 2 WHEN 'EN_RUTA' THEN 4 WHEN 'FINALIZADO' THEN 5 END
 WHERE o.operacion_id <= 9
 ORDER BY o.operacion_id, e.orden;

-- Historial de las operaciones de hoy con los estados nuevos
INSERT INTO HISTORIAL_OPERACIONES (operacion_id, estado_nuevo, fecha_cambio, usuario, observacion)
SELECT x.op, x.estado, o.hora_salida + NUMTODSINTERVAL(x.minutos, 'MINUTE'), x.usuario, x.obs
  FROM (SELECT 10 op, 'PROGRAMADO' estado, -1440 minutos, 'operador' usuario, CAST(NULL AS VARCHAR2(200)) obs FROM dual UNION ALL
        SELECT 10, 'ASIGNADO', -600, 'operador', 'Asignación automática' FROM dual UNION ALL
        SELECT 10, 'RETRASADO', -65, 'operador', 'Vuelo LA2041 llega con demora' FROM dual UNION ALL
        SELECT 11, 'PROGRAMADO', -1440, 'operador', NULL FROM dual UNION ALL
        SELECT 11, 'ASIGNADO', -720, 'operador', NULL FROM dual UNION ALL
        SELECT 11, 'EMBARQUE', -10, 'operador', NULL FROM dual UNION ALL
        SELECT 11, 'EN_RUTA', 2, 'operador', NULL FROM dual UNION ALL
        SELECT 11, 'LLEGADA_PROXIMA', 30, 'operador', NULL FROM dual UNION ALL
        SELECT 12, 'PROGRAMADO', -1440, 'operador', NULL FROM dual UNION ALL
        SELECT 12, 'ASIGNADO', -720, 'operador', NULL FROM dual UNION ALL
        SELECT 12, 'EMBARQUE', -25, 'operador', NULL FROM dual UNION ALL
        SELECT 12, 'EN_ESPERA_SALIDA', -21, 'operador', NULL FROM dual UNION ALL
        SELECT 13, 'PROGRAMADO', -300, 'operador', NULL FROM dual) x
  JOIN OPERACIONES_TRANSPORTE o ON o.operacion_id = x.op
 ORDER BY x.op, x.minutos;

COMMIT;

EXEC PKG_SEGURIDAD_ROLES.cerrar_sesion;

-- Las identidades continúan desde el último ID cargado
ALTER TABLE USUARIOS_SISTEMA       MODIFY usuario_id   GENERATED BY DEFAULT AS IDENTITY (START WITH LIMIT VALUE);
ALTER TABLE CLIENTES               MODIFY cliente_id   GENERATED BY DEFAULT AS IDENTITY (START WITH LIMIT VALUE);
ALTER TABLE PROVEEDORES            MODIFY proveedor_id GENERATED BY DEFAULT AS IDENTITY (START WITH LIMIT VALUE);
ALTER TABLE LUGARES                MODIFY lugar_id     GENERATED BY DEFAULT AS IDENTITY (START WITH LIMIT VALUE);
ALTER TABLE PERSONAL               MODIFY personal_id  GENERATED BY DEFAULT AS IDENTITY (START WITH LIMIT VALUE);
ALTER TABLE VEHICULOS              MODIFY vehiculo_id  GENERATED BY DEFAULT AS IDENTITY (START WITH LIMIT VALUE);
ALTER TABLE PAQUETES_TOUR          MODIFY paquete_id   GENERATED BY DEFAULT AS IDENTITY (START WITH LIMIT VALUE);
ALTER TABLE SALIDAS_PROGRAMADAS    MODIFY salida_id    GENERATED BY DEFAULT AS IDENTITY (START WITH LIMIT VALUE);
ALTER TABLE RESERVAS               MODIFY reserva_id   GENERATED BY DEFAULT AS IDENTITY (START WITH LIMIT VALUE);
ALTER TABLE DETALLE_RESERVA        MODIFY detalle_id   GENERATED BY DEFAULT AS IDENTITY (START WITH LIMIT VALUE);
ALTER TABLE PASAJEROS_RESERVA      MODIFY pasajero_id  GENERATED BY DEFAULT AS IDENTITY (START WITH LIMIT VALUE);
ALTER TABLE PAGOS                  MODIFY pago_id      GENERATED BY DEFAULT AS IDENTITY (START WITH LIMIT VALUE);
ALTER TABLE OPERACIONES_TRANSPORTE MODIFY operacion_id GENERATED BY DEFAULT AS IDENTITY (START WITH LIMIT VALUE);
