# Informe de auditoría y restauración — Tour Manager

**Referencia:** el zip `Kampu_tour-claude-serene-hypatia-go5zdq` es idéntico, archivo por archivo, a la versión v4 importada en
el commit `7392da4`.

**Decisión de alcance:** se conserva la v4 junto con tres cambios pedidos que funcionaban:

| Cambio | Contenido |
|---|---|
| **A** | Reglas de salidas y reservas; ficha de salida con manifiesto; avisos del panel |
| **B** | Solo 3 scripts SQL; particionamiento corregido |
| **C** | Banderas en el selector de teléfono |

Se revierte por completo **D**: el centro de operaciones multi-empresa.

## 1. Restauración

Se aplicó `git revert` del commit `d2bbafc`. El árbol resultante coincide byte a byte con el estado anterior a D (`5000e5c`).

**Qué se quitó al revertir D**
- **Estados de tráfico:** *En espera de salida*, *Llegada próxima* y *Retrasado*.
- **Rol y alta de empresas:** el rol `PROVEEDOR` y su `ROL_PROVEEDOR`, junto con la página `/afiliacion` y el endpoint `/auth/afiliacion`.
- **Respuesta de la empresa:** aceptar o rechazar servicios (`/operaciones/{id}/respuesta`).
- **Asignación:** la asignación masiva y las reglas de licencia por tipo de vehículo, zonas y reposicionamiento.
- **Vistas de tráfico:** el tablero de salidas y llegadas, el calendario y la vista de flota.
- **Columnas de base de datos:**
  - en `PROVEEDORES`: dirección, correo, contacto, servicios, zonas y fecha de afiliación;
  - en `OPERACIONES_TRANSPORTE`: tipo de servicio, ruta, vehículo exigido, empresa preferida, hora estimada y confirmación;
  - en `USUARIOS_SISTEMA`: `proveedor_id`.

Se conserva el módulo de Tráfico de la v4: tabla, asignación automática y manual, cambios de estado e historial.

## 2. Auditoría de la base de datos

### Errores encontrados y solución aplicada (`01_schema.sql`)

| # | Problema | Riesgo | Solución |
|---|---|---|---|
| 1 | Las columnas de estado (`USUARIOS_SISTEMA`, `CLIENTES.estado_cuenta`, `PROVEEDORES`, `PERSONAL`, `VEHICULOS`, `PAQUETES_TOUR`, `SALIDAS_PROGRAMADAS`, `RESERVAS.estado_reserva/estado_pago`, `OPERACIONES_TRANSPORTE`) aceptaban `NULL`. Un `CHECK (estado IN (...))` deja pasar `NULL`. | Un registro sin estado rompe las reglas del backend y, en las tablas particionadas por estado, la inserción. | `NOT NULL` (con el `DEFAULT` que ya tenían). |
| 2 | Las fechas de alta `CLIENTES.fecha_registro`, `RESERVAS.fecha_reserva`, `PAGOS.fecha_pago` y `HISTORIAL_OPERACIONES.fecha_cambio` aceptaban `NULL`. | Historiales y bitácoras sin fecha. | `NOT NULL`. |
| 3 | `OPERACIONES_TRANSPORTE` permitía vehículo sin conductor (o al revés), y estados de ejecución sin recursos. | La base de datos no defendía la regla; solo la imponía el backend. | `chk_op_recursos` (vehículo y conductor van juntos) y `chk_op_asignada` (ASIGNADO, EMBARQUE, EN_RUTA y FINALIZADO exigen vehículo). |
| 4 | `HISTORIAL_OPERACIONES.estado_nuevo` aceptaba cualquier texto. | Historial con estados inexistentes. | `chk_hist_est` con los mismos 6 estados de `chk_op_est`. |
| 5 | `DETALLE_RESERVA` aceptaba precio 0 o negativo y descuentos mayores al importe. | `subtotal` (columna virtual) negativo y totales incorrectos. | `chk_det_montos`: `precio_unitario > 0` y `descuento` entre 0 y `cantidad × precio`. |
| 6 | `CLIENTES` tenía dos restricciones únicas solapadas: `uq_cli_tipo_doc (tipo, número)` y `uq_cli_numero_doc (UPPER(número))`. La segunda es más estricta y vuelve inútil la primera. | Índice redundante y mensajes de error contradictorios. | Se eliminó `uq_cli_tipo_doc` y su mensaje en `ErroresOracle`. |

### Revisado sin cambios

Estos elementos se revisaron y se mantienen, con su justificación:

- **Las 14 tablas y `AUDITORIA`:** cada una la usa un módulo (catálogo, salidas, reservas, pasajeros, pagos, logística, tráfico, historial y bitácora). No hay tablas sin uso.
- **Claves foráneas:** cubren todas las relaciones, y no quedan registros huérfanos posibles.
- **`RESERVAS.monto_pagado` y `estado_pago`:** son valores derivados de `PAGOS`. Se conservan porque los usan los listados, la partición por estado y `TRG_AUDITORIA_ESTADO_PAQUETE`. El backend los recalcula en cada pago, aprobación, rechazo y anulación, y `TRG_VALIDAR_PAGO_PAQUETE` impide que los aprobados superen el total.
- **La sección 0 de `01_schema.sql`:** borra objetos de versiones anteriores (v3) para poder reinstalar sobre una base antigua. No afecta a una base nueva.
- **Los 8 objetos PL/SQL:** 2 por categoría, todos en uso.

### Archivos SQL

| Archivo | Estado | Motivo |
|---|---|---|
| `01_schema.sql` | Modificado | Esquema definitivo (único script de estructura). |
| `02_data.sql` | Conservado | Datos de demostración. Se verificó que cupos, montos, pasajeros y totales cuadran. |
| `03_particionamiento_roles.sql` | Conservado | Separado porque necesita el privilegio `CREATE ROLE`. |

No quedan scripts redundantes en el proyecto. `00_reset.sql` y `01_schema_v4.sql` de la referencia ya estaban consolidados en `01_schema.sql` desde el cambio B.

## 3. Backend

- **Entidades:** un script comparó las 17 entidades con `USER_TAB_COLUMNS`. Ninguna columna inexistente ni sin mapear. Además, Hibernate con `ddl-auto=validate` arranca sin diferencias.
- **Consultas escritas a mano:** las consultas nativas, las llamadas a `PKG_SEGURIDAD_ROLES`, `PKG_GESTION_PAQUETES_CLIENTE` y `SP_REGISTRAR_CLIENTE_EXTERNO`, y el `INSERT` de auditoría coinciden con las firmas y columnas reales.
- **Correcciones:**
  - **`ErroresOracle`:** se quitó `UQ_CLI_TIPO_DOC` y se añadieron mensajes para `CHK_OP_RECURSOS`, `CHK_OP_ASIGNADA` y `CHK_DET_MONTOS`.
  - **`GlobalExceptionHandler`:** tenía nombres de campos de la v3 (`empresaId`, `duracionEstimadaMin`); se reemplazaron por `proveedorId`.
  - **`Reglas.java`:** su comentario citaba `EMPRESA_TRANSPORTE.chk_emp_ruc`; ahora cita `PROVEEDORES.chk_prov_ruc`.
  - **`application.yaml`:** se quitó `hibernate.dialect`, que era redundante y generaba una advertencia al arrancar.

## 4. Frontend

Se compararon automáticamente los DTO del backend con las interfaces de `models.ts`, y las URL que llama el frontend con los endpoints existentes. Todas las URL existen.

**Corregido**
- **Paquetes:** el formulario tenía un selector *Lugar de destino* (`lugarDestinoId`) que no existe en la base ni en el backend. Lo elegido se descartaba en silencio, y la ayuda remitía a un módulo *Logística → Rutas* que ya no existe. Se quitaron el campo, el tipo y la ayuda.
- **Modelo de usuario:** `UsuarioSistema` declaraba `passwordHash`, que el backend nunca envía. Se quitó.
- **Validación de RUC:** el comentario de `validacion.ts` citaba `chk_emp_ruc` y ahora cita `chk_prov_ruc`.

## 5. Pruebas (Oracle 23ai Free · Spring Boot · Angular · Playwright)

**Instalación de los scripts**
- Los 3 scripts instalan sin errores sobre una base nueva y también una segunda vez seguida.
- Los 8 objetos quedan `VALID`, con 12 particiones y 4 roles; todas las restricciones quedan `ENABLED VALIDATED`.
- Las consultas de cuadre devuelven vacío: cupos ocupados frente a vendidos, monto pagado frente a pagos aprobados, pasajeros frente a cantidad, y total frente a la suma de subtotales.

**API: 129 pruebas, todas correctas**
- **Permisos:** la matriz de lecturas por rol (200 o 403) para Admin, Ventas, Operador y Cliente, y el 401 sin sesión.
- **CRUD con casos que deben rechazarse:**
  - **Proveedores:** RUC duplicado y segundo proveedor propio.
  - **Personal:** conductor sin licencia.
  - **Vehículos:** placa duplicada.
  - **Lugares:** coordenada incompleta.
  - **Paquetes:** más noches que días.
  - **Salidas:** el Operador no puede crearlas.
  - **Clientes:** correo o documento duplicado.
- **Reserva:** cálculo del monto, descuento y devolución de cupos, pasajero repetido, pago mayor al saldo, estado de pago, confirmación y boleta.
- **Operación:** llegada calculada, vehículo sin conductor rechazado, transición inválida rechazada, asignación automática, ciclo hasta FINALIZADO e historial.
- **Portal del cliente:** lista, detalle propio, reserva ajena bloqueada, informar un pago y que Ventas lo apruebe.
- **Borrados:** se rechazan si el registro tiene dependencias y se permiten si está libre.
- **Auditoría:** se registra cada cambio.

**Navegador**
- Se recorrieron 12 rutas por rol interno, más el portal y las páginas públicas, sin errores de consola ni respuestas fallidas; los guards redirigen correctamente.
- Formularios probados de punta a punta: alta de paquete (con validación de campos vacíos), cliente y operación (con llegada calculada y asignación automática), confirmación de salida, y la regla de no confirmar una reserva sin un pago aprobado.

## 6. Archivos de esta revisión

| Acción | Archivos |
|---|---|
| Revertidos (cambio D) | Ver el commit de revert: se eliminan 10 archivos que solo usaba D y se restauran 48. |
| Modificados (auditoría) | `01_schema.sql`, `ErroresOracle.java`, `GlobalExceptionHandler.java`, `Reglas.java`, `application.yaml`, `models.ts`, `validacion.ts`, `paquetes.component.ts/.html`, `README.md` |
| Eliminados | Ninguno adicional |
| Conservados | El resto del proyecto (v4 + A + B + C) |

## 7. Instalación y ejecución

Ver el README (secciones 1 a 3). El orden de arranque es Oracle, luego el backend y luego el frontend.

1. **Base de datos:** como dueño del esquema, ejecutar en orden `01_schema.sql`, `02_data.sql` y `03_particionamiento_roles.sql`.
2. **Backend:** `cd kampu-backend && ./mvnw spring-boot:run`. La conexión se indica con `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`.
3. **Frontend:** `cd kampu-frontend && npm install && npm start`, y abrir http://localhost:4200.
