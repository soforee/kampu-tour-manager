# KAMPU · Tour Manager v4 — sistema integrado (Angular + Spring Boot + Oracle)

Agencia de turismo: catálogo de paquetes, salidas programadas, reservas, pagos, tráfico/transporte y, desde la v4,
**portal del cliente** con **seguridad por roles** aplicada en la base de datos.

| Capa | Carpeta | Tecnología |
|---|---|---|
| Base de datos | `kampu-backend/src/main/resources/db/` | Oracle 12.2+ (probado en Oracle 23ai Free) |
| Backend | `kampu-backend` | Spring Boot 3.4 · JPA · JdbcTemplate (paquetes PL/SQL) · BCrypt |
| Frontend | `kampu-frontend` | Angular 18 (standalone, signals) · Tailwind |

## Versión 4 — qué cambió

1. **"Registrarse" reemplaza a "Probar cuenta demo".** El login ya no tiene los botones Administrador / Ventas / Operador.
   Ahora tiene un enlace **Registrarse** (autoregistro de clientes externos) y conserva **Volver al inicio**.
2. **Módulo de clientes** (`/registro`, `/mi-cuenta`, `/mi-cuenta/paquetes/:id`): el cliente ve **solo sus paquetes**, en modo
   consulta, con **estado de ejecución** (Por empezar / En curso / Terminado) y **estado de pago**
   (Pagado completo / Falta pagar / Pendiente de pago). Lo único que puede hacer es *informar un pago*, que queda
   PENDIENTE hasta que Ventas lo valide. Todas sus pantallas muestran **Volver al inicio**.
3. **Seguridad por roles** en tres capas: menú y rutas en Angular (guards) → interceptor del backend (token) →
   `PKG_SEGURIDAD_ROLES` en Oracle (matriz de permisos + vistas que filtran por la sesión).
4. **Modelo v4 de 14 tablas** (+ `AUDITORIA`): `PROVEEDORES`, `PERSONAL` (guías y conductores) y `VEHICULOS` reemplazan a
   empresas/guías/conductores/rutas de la v3. Columnas añadidas a lo que trae el script v4 van marcadas `-- (+v4)`.
5. **Exactamente dos objetos por categoría** (PL/SQL, triggers, vistas, paquetes) y **script de particionamiento por nivel de usuario**.
6. Contraseñas con **BCrypt** (se aceptan las de texto plano antiguas de usuarios internos) y sesión con **token Bearer**.

### Cuentas

| Usuario | Contraseña | Rol | Entra a |
|---|---|---|---|
| `admin` | `admin123` | ADMIN | Panel completo |
| `ventas` | `ventas123` | VENDEDOR | Panel comercial |
| `operador` | `operador123` | OPERADOR | Panel operativo |
| `ana.ramirez@mail.com` (o DNI `45871236`) | `cliente123` | CLIENTE | Mis paquetes |
| `john.miller@mail.com` | `cliente123` | CLIENTE | Mis paquetes |

Los demás clientes de la demo los creó Ventas y **no tienen cuenta web**: pueden activarla con *Registrarse* usando
su **mismo documento y correo** (el procedimiento "reclama" el cliente existente y sus reservas aparecen solas).

## Versión 4.3 — mejoras visuales

Solo frontend; además, los datos de demostración cargan una foto del paquete Machu Picchu.

- **Menú lateral**:
  - fijo y abierto, con el nombre de cada módulo, el usuario y su rol;
  - se puede colapsar a íconos, y el sistema recuerda la elección;
  - migas de pan en los detalles (*Reservas › R-0003*).
- **Modo oscuro**:
  - botón en el menú; la primera vez sigue la preferencia del sistema;
  - los colores salen de variables CSS (`styles.css`): el tema oscuro solo cambia sus valores;
  - la clase `tema-claro` mantiene claras las zonas que van sobre fotos.
- **Panel**:
  - comparación de lo cobrado con el mes anterior;
  - sección **Hoy**: salidas que empiezan, traslados del día y pagos por validar;
  - gráficos *Vendido por mes* y *Pasajeros por paquete*, con tooltip y vista de tabla.
- **Listas** (reservas, clientes, pagos, salidas, tráfico):
  - en celular cada fila es una tarjeta;
  - la columna de acciones queda fija a la derecha;
  - esqueleto de carga mientras llegan los datos;
  - estados vacíos con un botón de acción;
  - el registro recién guardado se resalta un momento;
  - avatares con iniciales y fechas relativas (*mañana*, *hace 5 días*);
  - íconos según el tipo de lugar.
- **Nueva reserva**: asistente en 4 pasos (Cliente → Salida → Pasajeros → Revisar) que valida cada paso antes de avanzar.
- **Portal del cliente**:
  - línea de tiempo del viaje (Reservado → Pagado → Por empezar → En curso → Terminado);
  - cuenta regresiva para la salida;
  - barra de progreso del pago.
- **Página principal**: testimonios (son de ejemplo: reemplázalos por reseñas reales) y botón flotante de WhatsApp.
- **Fotos**: Machu Picchu en la portada, en el login y en su paquete, de Wikimedia Commons con licencia CC BY-SA. Los créditos están en `kampu-backend/images/demo/ATRIBUCIONES.md` y en el pie de página. Las fotos de los demás paquetes se suben desde *Paquetes → Editar*.
- **Accesibilidad**:
  - las ventanas se cierran con Esc, ponen el foco en el primer campo y no dejan que el tabulador salga de ellas;
  - mayor contraste del texto secundario;
  - se respeta la opción del sistema de reducir animaciones.

## Versión 4.2 — lógica de tour manager y pulido

**Reglas de negocio nuevas (backend, `SalidaProgramadaServiceImpl` y `ReservaServiceImpl`):**

| Regla | Antes | Ahora |
|---|---|---|
| Estados de una salida | Se podía pasar de cualquier estado a cualquiera (revivir una cancelada, editar una terminada) | `PROGRAMADA → CONFIRMADA → COMPLETADA`, con `CANCELADA` desde las dos primeras y *reabrir* (`CONFIRMADA → PROGRAMADA`). `COMPLETADA` y `CANCELADA` son finales. |
| Crear una salida | Se podía crear ya confirmada, en una fecha pasada o de un paquete inactivo | Nace `PROGRAMADA`, desde hoy y solo de paquetes activos. |
| Confirmar una salida | Solo revisaba el transporte | Además exige **guía** y **al menos un pasajero**. |
| Terminar una salida | Bastaba con que hubiera empezado | Tampoco puede quedar ninguna **reserva pendiente** ni ningún **traslado abierto**. |
| Cancelar una salida | Sus traslados seguían ocupando vehículos y conductores | Los traslados que no han salido se cancelan solos (queda en su historial). |
| Editar una salida con reservas | Se podía cambiar el paquete que compraron los clientes | Bloqueado. Tampoco se reprograma a una fecha pasada. |
| Confirmar una reserva | Sin condiciones | Exige un **pago aprobado** (adelanto). |
| Completar una reserva | En cualquier momento | Solo cuando el viaje ya empezó. |
| Vender un paquete inactivo | Permitido | Bloqueado. |

**Pantallas nuevas o mejoradas (frontend):**

- **Ficha de salida** (`/salidas/:id`): lista de verificación *"Lista para salir"* (guía, pasajeros, transporte,
  reservas confirmadas y pagos), **manifiesto de pasajeros** imprimible con firma, notas para el guía, traslados del
  día, ocupación y saldo por cobrar. Botones para confirmar, terminar, reabrir o cancelar según el estado y el rol.
  API: `GET /salidas/{id}/manifiesto` (los datos de pago solo llegan a quien tiene `VER_MONTOS`).
- **Panel → "Requiere atención"**: salidas de los próximos 7 días sin guía o sin transporte, reservas pendientes con
  el viaje encima, pagos por validar, traslados sin vehículo, licencias por vencer y mantenimientos atrasados. Cada
  aviso lleva a la pantalla donde se resuelve.
- **Salidas**: las próximas primero, cuenta regresiva ("Sale mañana") y aviso de las que no tienen guía; el estado
  solo ofrece las transiciones válidas.
- Fechas y números en **español de Perú**; marcadores de carga, animaciones escalonadas y vista de impresión.
- Las pantallas se cargan bajo demanda: el paquete inicial bajó de 780 kB a 500 kB.

**Limpieza:** 13 hojas de estilo vacías, etiquetas y comentarios de objetos de la v3 que ya no existen
(`PKG_ASIGNACION`, `PKG_RUTAS`, `trg_*`, `DOMINIO_ESTADOS`) y dependencias de prueba comentadas en `pom.xml`.
Los estados `SUSPENDIDO`, `MANTENIMIENTO` y `OCUPADO` ahora tienen color y texto propios.

**Datos de demostración (`02_data.sql`):** la salida *Paracas e Ica* pasa a `PROGRAMADA` (no tenía transporte
completo) y la reserva R-0008 tiene un adelanto aprobado (estaba confirmada sin pagos).

## Objetos críticos de backend (2 por categoría) y su justificación

Todos están en `01_schema.sql`, comentados.

| Categoría | Objeto | Qué hace | Por qué este y no otro |
|---|---|---|---|
| PL/SQL | `FN_ESTADO_PAGO_PAQUETE(reserva_id)` | Devuelve `PAGADO_COMPLETO`, `FALTA_PAGAR`, `PENDIENTE_PAGO` o `ANULADO` usando solo pagos **APROBADOS**. | Es el "estado de pago" que pide el cliente. Al estar en la BD, la vista del cliente, el resumen interno y la boleta usan la misma regla. |
| PL/SQL | `SP_REGISTRAR_CLIENTE_EXTERNO(...)` | Registro externo: valida documento, correo y teléfono únicos (errores `-20100`…`-20104`); si el cliente ya existía sin cuenta y coincide documento **y** correo, le activa la cuenta; deja fila en `AUDITORIA`. | El botón *Registrarse* necesita una operación atómica que no deje clientes duplicados aunque dos personas se registren a la vez (índices únicos + bloqueo). |
| Trigger | `TRG_AUDITORIA_ESTADO_PAQUETE` | `AFTER UPDATE OF estado_reserva, estado_pago ON RESERVAS` → inserta quién (usuario y rol de la sesión), qué cambió y cuándo. | Trazabilidad de los estados que ve el cliente; funciona también si alguien cambia datos directo en SQL. |
| Trigger | `TRG_VALIDAR_PAGO_PAQUETE` | Compound trigger en `PAGOS`: impide pagos en reservas anuladas/completadas (`-20201`) y que los aprobados superen el total (`-20202`). | Es la última barrera para el dinero: el portal permite que el cliente informe pagos y no se puede confiar solo en el frontend. |
| Vista | `VW_PAQUETES_CLIENTE` | Paquetes del cliente **de la sesión** (`PKG_SEGURIDAD_ROLES.cliente_actual()`), con estado de ejecución, estado de pago, total, pagado, saldo y monto por validar. | El aislamiento entre clientes lo hace Oracle: aunque el backend tuviera un error, la vista nunca devuelve reservas ajenas. |
| Vista | `VW_RESUMEN_PAQUETES_POR_ROL` | Resumen por salida (cupos, pasajeros, operaciones, guía, montos). Montos solo si el rol puede `VER_MONTOS`; el OPERADOR solo ve `POR_EMPEZAR`/`EN_CURSO`. | Un solo origen para el panel de los tres roles internos: la restricción la aplica la BD, no la pantalla. |
| Paquete | `PKG_SEGURIDAD_ROLES` | Contexto de la sesión (`iniciar_sesion`, `usuario_actual`, `rol_actual`, `cliente_actual`), catálogo de acciones, matriz `puede(rol, acción)`, `permisos(rol)` (lista para el frontend), `exigir`, y administración de accesos (`asignar_rol`, `revocar_acceso`, `restablecer_acceso`). | Centraliza el "quién puede qué". El backend le pregunta en cada petición y las vistas y triggers lo usan. |
| Paquete | `PKG_GESTION_PAQUETES_CLIENTE` | `estado_ejecucion`, `pertenece`, `exigir_propietario` (`-20080`), `listar_paquetes` y `detalle_paquete` (REF CURSOR) e `iniciar_pago` (pago PENDIENTE con `registrado_por = CLIENTE`, errores `-20081`…`-20083`). | Todo lo que el cliente puede hacer pasa por aquí: lectura y un único tipo de escritura controlada. |

> El script v4 original traía además `TRG_GESTION_CUPOS`, `TRG_AUTO_HISTORIAL_OP`, `V_PORTAL_CLIENTE`, `V_TABLERO_FIDS` y
> `PKG_TRANSACCIONES`. Para cumplir "exactamente dos por categoría" esa lógica pasó al backend: los cupos se descuentan
> con `SELECT ... FOR UPDATE` en `ReservaServiceImpl` y el historial de operaciones lo escribe `OperacionTransporteServiceImpl`.

### Matriz de permisos (`PKG_SEGURIDAD_ROLES.puede`)

| Acción | ADMIN | VENDEDOR | OPERADOR | CLIENTE |
|---|:-:|:-:|:-:|:-:|
| Usuarios (ver / gestionar) · Auditoría | ✔ | — | — | — |
| Clientes (ver / crear / editar) | ✔ | ✔ | — | — |
| Reservas (ver / gestionar) | ✔ | ✔ | — | — |
| Ver montos · Registrar/validar pagos | ✔ | ✔ | — | — |
| Catálogo (paquetes y salidas) | ✔ | ✔ | solo ver | — |
| Actualizar estado de ejecución | ✔ | ✔ | ✔ | — |
| Logística (proveedores, personal, vehículos, lugares) | ✔ | ver | ✔ | — |
| Operaciones de tráfico | ✔ | ver | ✔ | — |
| Resumen de paquetes | ✔ | ✔ | ✔ (sin montos, sin terminados) | — |
| Ver paquetes propios · Informar pago | — | — | — | ✔ |

Una acción que no está en el catálogo se niega a todos (también a ADMIN). Cambiar un permiso es editar solo
`PKG_SEGURIDAD_ROLES.puede`: el backend lo aplica en cada petición (caché de 5 min) y el frontend lo recibe en
`permisos` al iniciar sesión y al recargar (`GET /auth/sesion`); menú, rutas y botones salen de esa lista, así que
la interfaz nunca ofrece algo que el backend rechazaría con 403.

## Auditoría (solo Administrador)

Menú **Auditoría** (`/auditoria`, permiso `VER_AUDITORIA`). La tabla `AUDITORIA` guarda todo lo que pasa en el sistema:

| Acción | Quién la registra | Qué guarda |
|---|---|---|
| Alta (`CREAR`) | `AuditoriaHibernateListener` (toda escritura JPA) | Resumen de los datos guardados |
| Modificación (`ACTUALIZAR`) | El mismo listener · `TRG_AUDITORIA_ESTADO_PAQUETE` para estados de reserva | **Una fila por campo**: campo, valor anterior y valor nuevo |
| Baja (`ELIMINAR`) | El listener | Los datos que tenía el registro |
| Registro web (`REGISTRO`) | `SP_REGISTRAR_CLIENTE_EXTERNO` | Alta o activación de la cuenta |
| Pago informado en el portal | `PortalClienteService` (el INSERT lo hace PL/SQL) | Reserva, monto, método y operación |
| `LOGIN`, `LOGOUT`, `LOGIN_FALLIDO` | `AuthServiceImpl` | Usuario, rol y motivo del rechazo |

Cada fila tiene usuario, rol, fecha y hora, entidad e id, y origen (`BD` = Oracle, `APP` = backend). Los cambios
se registran en la **misma transacción** que el dato: si la operación falla, su registro también se deshace (los
ingresos fallidos usan una transacción propia para que queden). Las contraseñas nunca se guardan (`******`).

La pantalla permite filtrar por texto, rango de fechas (atajos Hoy / 7 días / 30 días), rol, acción, entidad y usuario;
ver el **historial completo de un registro**; paginar en el servidor (50 por página) y **exportar a CSV** con los mismos filtros.
Filtrar por rol aprovecha la partición de `AUDITORIA` por `rol_usuario`.

## Particionamiento y roles por nivel de usuario (`03_particionamiento_roles.sql`)

| Tabla | Particionada por (LIST) | Particiones | Para qué nivel |
|---|---|---|---|
| `AUDITORIA` | `rol_usuario` | admin · ventas · operador · cliente · sistema | El ADMIN revisa todo; cada nivel se consulta por separado. |
| `PAGOS` | `registrado_por` | admin · ventas · cliente | Los pagos informados en el portal quedan como cola de validación de Ventas. |
| `OPERACIONES_TRANSPORTE` | `estado` | cola del operador (PROGRAMADO, ASIGNADO, EMBARQUE, EN_RUTA) · cerradas | Cola de trabajo del Operador. `ROW MOVEMENT`: al finalizar pasa sola al histórico. |
| `RESERVAS` | `estado_reserva` | vigentes (PENDIENTE, CONFIRMADA) · cerradas | Trabajo diario de Ventas separado de lo histórico. |

Además crea `ROL_ADMIN`, `ROL_VENTAS`, `ROL_OPERADOR` y `ROL_CLIENTE` con los mismos ámbitos que la matriz
(Ventas sin usuarios ni flota; Operador sin pagos, sin `CLIENTES` y sin montos; Cliente solo las vistas y el paquete
del portal) para cuentas que entren directo a Oracle (reportes, SQL Developer). La aplicación usa un único usuario
de conexión y su nivel lo controla `PKG_SEGURIDAD_ROLES`.

Requisitos: opción *Partitioning* (XE 18c/21c, 23ai Free o Enterprise), conexión a la **PDB** (`XEPDB1` en XE,
`FREEPDB1` en 23ai Free; no al contenedor raíz `XE`/`FREE`) y el privilegio `CREATE ROLE`
(`GRANT CREATE ROLE TO LP2;` como DBA). Si falta algo, el script no se detiene con errores: particiona lo que puede
y muestra en la salida qué no hizo y cómo resolverlo. Se puede volver a ejecutar las veces que haga falta
(las tablas ya particionadas y los roles existentes se respetan).

## Especificación de cambios de interfaz

| Pantalla | Antes | Ahora |
|---|---|---|
| Login (`/login`) | "o entra con una cuenta demo" + 3 botones | Enlace **Registrarse**; campo "Usuario o correo" (el cliente entra con correo o documento); **Volver al inicio** se mantiene. Tras entrar: personal → `/dashboard`, cliente → `/mi-cuenta`. |
| Registro (`/registro`, nuevo) | — | Nombre, tipo y número de documento, correo (único), teléfono con país, contraseña (**mínimo 8**, indicador de fortaleza) y **confirmación**. Errores bajo cada campo; los de unicidad vienen de Oracle. Al registrarse queda **autenticado** y va a *Mis paquetes*. |
| Mis paquetes (`/mi-cuenta`, nuevo) | — | Tarjetas con imagen, fechas, pasajeros, total, saldo y dos insignias: ejecución y pago. Filtros Todos / Por empezar / En curso / Terminados. Saldo total por pagar. |
| Detalle (`/mi-cuenta/paquetes/:id`, nuevo) | — | Descripción, guía, pasajeros, barra de pago (pagado / por validar / saldo), historial de pagos e **Informar un pago** (monto ≤ saldo − por validar). Una reserva ajena muestra "no pertenece a tu cuenta". |
| Cabecera del portal | — | Logo, **Volver al inicio**, Mis paquetes y Salir en todas las pantallas del cliente. |
| Panel (`/dashboard`) | Igual para todos | Por rol: Admin/Ventas ven KPIs de dinero, reservas recientes y todos los estados; Operador ve por empezar / en curso, pasajeros y operaciones, **sin montos**. Botones *Confirmar salida* y *Marcar terminada*. |
| Menú lateral | 8 opciones para todos | Según `permisos`: Admin 9 (incluye **Auditoría**), Ventas 8, Operador 4 (Panel, Salidas solo lectura, Tráfico, Logística). Cada ruta tiene `permisoGuard`. |
| Botones de gestión | Visibles para todos | Solo con el permiso: Logística `GESTIONAR_LOGISTICA` (Ventas solo consulta), Tráfico `GESTIONAR_OPERACIONES` (Ventas solo consulta), Salidas `GESTIONAR_CATALOGO` (Operador solo consulta), Clientes, Reservas y Pagos con sus permisos. |
| Volver al inicio (panel) | — | En la barra superior (escritorio), la cabecera (móvil) y el menú lateral. Lleva a la página principal **sin cerrar sesión**; allí "Ir al panel" regresa. |
| Auditoría (`/auditoria`, nuevo) | — | Bitácora con filtros, historial por registro, paginación y exportación CSV (solo Admin). |
| Clientes / Pagos | — | Insignia **Cuenta web** en clientes registrados; insignia **Portal** en pagos informados por el cliente (para validarlos). |
| Nueva reserva | Modo "Invitado (sin registro)" | Modo **Cliente nuevo** con documento: se crea el cliente (en v4 toda reserva tiene cliente) y luego puede activar su cuenta. |
| Página de inicio | "Acceso Admin" | "Iniciar sesión" y "Registrarse" (o "Mis paquetes"/"Ir al panel" con sesión). |

## Manual de integración (frontend ↔ backend ↔ Oracle)

**Cómo viaja la sesión.** `POST /auth/login` o `POST /auth/registro` devuelven `{usuarioId, clienteId, username, nombre, rol, token}`.
Angular lo guarda (`tm4_session`) y `usuario.interceptor.ts` envía `Authorization: Bearer <token>` en cada llamada; ante un 401
borra la sesión y manda al login. En el backend, `SeguridadInterceptor` resuelve el token, decide la acción y pregunta a
`PKG_SEGURIDAD_ROLES.puede(rol, acción)` (403 si no). `SesionOracleDataSource` llama a
`PKG_SEGURIDAD_ROLES.iniciar_sesion(usuario, rol, cliente)` en **cada conexión del pool** antes de usarla, así las vistas y
los triggers saben quién está operando.

| Pantalla (Angular) | Servicio | Endpoint | Objeto Oracle | Roles |
|---|---|---|---|---|
| Registro | `AuthService.registrar` | `POST /auth/registro` | `SP_REGISTRAR_CLIENTE_EXTERNO` | público |
| Login | `AuthService.login` | `POST /auth/login` | `CLIENTES` / `USUARIOS_SISTEMA` (BCrypt) | público |
| Mis paquetes | `PortalService.misPaquetes` | `GET /portal/paquetes` | `VW_PAQUETES_CLIENTE` (+ `FN_ESTADO_PAGO_PAQUETE`) | CLIENTE |
| Detalle del paquete | `PortalService.detalle` | `GET /portal/paquetes/{reservaId}` | `PKG_GESTION_PAQUETES_CLIENTE.exigir_propietario` + `detalle_paquete` | CLIENTE |
| Informar pago | `PortalService.informarPago` | `POST /portal/paquetes/{reservaId}/pagos` | `PKG_GESTION_PAQUETES_CLIENTE.iniciar_pago` → `TRG_VALIDAR_PAGO_PAQUETE` | CLIENTE |
| Panel | `TourStore.resumen` | `GET /resumen-paquetes` | `VW_RESUMEN_PAQUETES_POR_ROL` | ADMIN, VENDEDOR, OPERADOR |
| Panel (botones) | `TourStore.marcarEjecucion` | `PATCH /salidas/{id}/ejecucion` `{estado: CONFIRMADA\|COMPLETADA}` | `TRG_AUDITORIA_ESTADO_PAQUETE` (reservas → COMPLETADA) | ADMIN, VENDEDOR, OPERADOR |
| Validar pago | `TourStore` | `PATCH /pagos/{id}/resolver` | `TRG_VALIDAR_PAGO_PAQUETE`, `TRG_AUDITORIA_ESTADO_PAQUETE` | ADMIN, VENDEDOR |
| Permisos de la sesión | `AuthService.refrescar` | `GET /auth/sesion` | `PKG_SEGURIDAD_ROLES.permisos` | todos con sesión |
| Auditoría | `AuditoriaService` | `GET /auditoria?desde&hasta&rol&accion&entidad&entidadId&usuario&q&pagina&tamanio` · `GET /auditoria/filtros` · `GET /auditoria/exportar` (CSV) | `AUDITORIA` (particionada) | ADMIN |

Resto de la API (bajo `/lp2_gp2/api/v1`, con token salvo lo indicado):

| Recurso | Endpoints | Acción exigida |
|---|---|---|
| Auth | `POST /auth/login` · `POST /auth/registro` (públicos) · `GET /auth/sesion` · `POST /auth/logout` (login y registro devuelven `permisos`) | — |
| Catálogo | `GET /paquetes[/{id}]`, `GET /salidas[/{id}]` (públicos) · `POST/PUT/DELETE /paquetes`, `/salidas` | `GESTIONAR_CATALOGO` |
| Clientes | `GET /clientes`, `/clientes/buscar?q=`, `/clientes/{id}` · `POST/PUT/DELETE` | `VER_CLIENTES` / `GESTIONAR_CLIENTES` |
| Reservas | `GET /reservas[/{id}]`, `/{id}/pasajeros`, `/{id}/boleta` · `POST /reservas`, `/{id}/boleta` · `PATCH /{id}/confirmar\|completar\|anular` | `VER_RESERVAS` / `GESTIONAR_RESERVAS` |
| Pagos | `GET /pagos[?reservaId=]` · `POST /pagos` · `PATCH /pagos/{id}/resolver` | `VER_MONTOS` / `REGISTRAR_PAGO` |
| Logística | `/proveedores`, `/personal`, `/vehiculos`, `/lugares` (CRUD) | `VER_LOGISTICA` / `GESTIONAR_LOGISTICA` |
| Tráfico | `GET /operaciones`, `/operaciones/estados`, `/operaciones/estimar-duracion`, `/{id}`, `/{id}/historial`, `/{id}/candidatos` · `POST /operaciones`, `/{id}/asignar-automatico` · `PUT /{id}` · `PATCH /{id}/estado`, `/{id}/asignar` | `VER_OPERACIONES` / `GESTIONAR_OPERACIONES` |
| Usuarios | `GET /usuarios` | `VER_USUARIOS` |

Errores: `{ "status", "error", "timestamp" }` — 400 validación/regla, 401 sin sesión o sesión vencida, 403 rol sin permiso,
404 no existe, 409 duplicado. Los `RAISE_APPLICATION_ERROR` de Oracle se traducen a mensajes en español en `ErroresOracle`.

## Rediseño visual del frontend (UI/UX)

Esta versión solo cambia la **apariencia** del frontend (`kampu-frontend`). El backend, la base de datos, la API,
los servicios, las rutas y la lógica de los componentes son los mismos.

- **Identidad visual**: nuevo logotipo Kampu (isotipo "K" atravesada por un camino + wordmark con hoja) en `src/app/shared/logo.component.*` y como favicon.
- **Sistema de diseño** (`src/index.html` + `src/styles.css`): esquinas redondeadas, sombras suaves, paleta KAMPU
  del logotipo (verde oliva `#4B5B1D`, hoja lima `#C4BD45` y fondo crema), tipografía Nunito y clases reutilizables: `btn-dark` (principal), `btn-brand` (guardar/confirmar),
  `btn-light` (secundario/cancelar), `btn-danger` (eliminar/anular), `icon-btn*`, `field`, `label`/`label req`,
  `form-section`, `form-actions`, `card`, `tbl`, `seg`/`seg-btn` (filtros), `alert-*`, `empty`, `progress`, `chip`.
- **Barra lateral fija y compacta**: en escritorio muestra solo los iconos y se expande suavemente al pasar el cursor
  (o con el teclado), resaltando la sección activa; en móvil se abre como menú lateral.
- **Formularios** organizados en secciones, con campos obligatorios (`*`) y opcionales marcados, foco visible,
  selects y fechas estilizados.
- **Tablas, tarjetas, KPIs, modales y alertas (SweetAlert2)** rediseñados, con animaciones discretas
  (se desactivan si el sistema pide "reducir movimiento") y diseño responsive.

## Validación de formularios (frontend + backend)

Las mismas reglas se aplican en ambos lados: `kampu-frontend/src/app/core/utils/validacion.ts` y
`kampu-backend/src/main/java/pe/edu/upeu/lp2_gp2/dto/Reglas.java`.

| Campo | Regla |
|---|---|
| Nombres de personas (cliente, guía, pasajero) | Letras (con tildes y ñ), espacios y `. ' -` |
| DNI / CE / Pasaporte / RUC | DNI: 8 dígitos · CE: 8 a 12 letras o números · Pasaporte: 6 a 12 letras o números · RUC (solo clientes y empresas): 11 dígitos que empiezan con 10, 15, 17 o 20 |
| Teléfono (opcional) | Números con `+ ( ) -`, entre 6 y 15 dígitos |
| Correo (opcional) | Formato `nombre@dominio.com` |
| Placa | Letras, números y guion (3 a 10, `VEHICULO.placa` es `VARCHAR2(10)`) |
| Días / noches / cupos / capacidad | Enteros: 1–99 / 0–99 (y noches ≤ días) / 1–999 / 1–999 |
| Precio base / monto | Mayor a 0, máximo 2 decimales (cabe en `NUMBER(10,2)`); el pago aprobado no puede exceder el saldo |
| Fecha de nacimiento (opcional) | Anterior a hoy y desde 1900 |

- En el navegador, las funciones de `core/utils/filtro.ts` (enlazadas con `(beforeinput)` y `(paste)` en las plantillas) impiden escribir o pegar caracteres inválidos
  (por ejemplo letras o la "e" en campos numéricos) y los mensajes aparecen debajo de cada campo.
- En el backend, Bean Validation rechaza los datos con estado 400 y un mensaje claro; si llega un tipo incorrecto
  (letras o decimales en un campo entero, fechas mal escritas) se indica qué campo falló.
- Los tipos y longitudes coinciden con `01_schema.sql`; las reglas que dependen de otros datos (cupos, cruces de
  horario, transiciones, capacidad) las valida la base de datos con sus triggers.

## 1. Base de datos (Oracle 12.2 o superior)

1. Usuario dueño del esquema (por defecto `LP2` / `admin123`) con permisos para crear tablas, vistas, secuencias,
   procedimientos y triggers. Para el paso 3 también `CREATE ROLE` (`GRANT CREATE ROLE TO LP2;` como DBA).
2. Conectado como ese usuario, ejecuta **en orden** los tres scripts de `kampu-backend/src/main/resources/db/`
   (SQL Developer: "Ejecutar script" F5; SQL*Plus: `@archivo`):
   1. `01_schema.sql` — **primero borra lo que haya de una instalación anterior** (cualquier versión, con sus datos)
      y luego crea las 14 tablas + `AUDITORIA`, índices únicos (documento, correo, teléfono), secuencia y los
      8 objetos PL/SQL. Al final todos deben salir `VALID`.
   2. `02_data.sql` — datos demo con **fechas relativas a hoy** (hay paquetes terminados, en curso y por empezar).
   3. `03_particionamiento_roles.sql` — particiones y roles por nivel de usuario.
3. En SQL*Plus, para las tildes: `set NLS_LANG=AMERICAN_AMERICA.AL32UTF8` antes de entrar.

> Para reinstalar (o actualizar desde una versión anterior) se ejecutan los mismos tres scripts en el mismo orden.
> Como `01_schema.sql` borra los datos, si necesitas conservar datos reales expórtalos antes.

### Correo (boleta informativa)

La boleta se envía sola al **confirmar una reserva** o al **aprobar un pago**, y se puede reenviar desde el detalle.
El correo sale por SMTP (Gmail por defecto) con credenciales en variables de entorno, para no subirlas a GitHub:

```bash
export MAIL_USERNAME=tu_correo@gmail.com
export MAIL_PASSWORD=tu_contraseña_de_aplicación   # Gmail → Seguridad → Contraseñas de aplicaciones
```

En IntelliJ: *Run → Edit Configurations → Environment variables*. Sin `MAIL_USERNAME` el envío queda desactivado
(la reserva funciona igual) y el botón "Ver" del detalle muestra la boleta en el navegador.

## 2. Backend (`kampu-backend`)

Conexión en `src/main/resources/application.yaml`. Puedes editarla ahí o usar variables de entorno:

| Variable      | Valor por defecto                        |
|---------------|------------------------------------------|
| `DB_URL`      | `jdbc:oracle:thin:@localhost:1522/XE`    |
| `DB_USERNAME` | `LP2`                                    |
| `DB_PASSWORD` | `admin123`                               |

Ejemplos de URL: Oracle XE 21c → `jdbc:oracle:thin:@localhost:1521/XEPDB1`; Oracle 23 Free → `jdbc:oracle:thin:@localhost:1521/FREEPDB1`.

```bash
cd kampu-backend
./mvnw spring-boot:run
```

(En Windows: `mvnw.cmd spring-boot:run`, o ejecutar `Lp2Gp2Application` desde IntelliJ.)

- Swagger: http://localhost:8082/lp2_gp2/swagger-ui/index.html .
- Imágenes de paquetes: se guardan en la carpeta `images/` (propiedad `storage.location`) y se sirven en `/lp2_gp2/images/...`.
- `spring.jpa.hibernate.ddl-auto` está en **`none`**: Hibernate no crea ni altera tablas; manda el script.

## 3. Frontend (`kampu-frontend`)

```bash
cd kampu-frontend
npm install
npm start
```

Abre http://localhost:4200. `npm start` usa `proxy.config.json`: `/api` → `http://localhost:8082/lp2_gp2/api` y
`/lp2_gp2/images` → `http://localhost:8082`. **Orden de arranque:** Oracle → backend → frontend.

## 4. Pruebas realizadas

- Auditoría y restauración (ver `INFORME_AUDITORIA.md`): scripts instalados dos veces seguidas sin errores, entidades
  validadas contra el esquema (`ddl-auto=validate` y comparación columna por columna), 129 pruebas de API (permisos,
  CRUD, reglas de negocio y rechazos) y recorrido de todas las pantallas por rol sin errores.

- v4.1: Logística con datos para Admin, Ventas y Operador; recorrido de las 11 rutas del panel por rol sin ningún 403
  ni error de JavaScript; Ventas y Operador sin botones de gestión fuera de sus permisos; por API, Ventas no crea
  lugares, el Operador no crea clientes ni ve pagos (403); la bitácora registra alta, modificación campo por campo,
  baja, login, logout e ingreso fallido; "Volver al inicio" conserva la sesión.

- Oracle 23ai Free: los 3 scripts se ejecutan sin errores (también dos veces seguidas); los 8 objetos `VALID`; 12 particiones y 4 roles creados.
- Backend con `ddl-auto=validate` (las entidades coinciden con el esquema) y pruebas de API por rol (200/401/403, errores de Oracle).
- Navegador (Playwright): login sin botones demo; registro con contraseña corta, confirmación distinta y correo repetido
  (rechazados) y registro válido con ingreso automático; cliente redirigido fuera del panel; "Volver al inicio" en
  registro, lista y detalle; cliente ve solo sus 2 paquetes y no una reserva ajena; informar pago por encima del saldo
  (rechazado) y válido; Operador sin montos ni acceso a Pagos; Ventas con montos e insignia "Cuenta web".

## 5. Limitaciones y decisiones

- El token es opaco y vive en memoria del backend: al reiniciar el backend hay que volver a iniciar sesión.
- La v4 no tiene tabla de rutas: la asignación automática de transporte no filtra por región del proveedor.
- Los tipos de cambio son fijos: `TASAS` (frontend) y `ConversorMoneda` (backend) deben coincidir.
- La duración de los traslados usa OSRM (`OSRM_URL`) y, si no responde, distancia en línea recta con velocidad media.
- Producción: servir `ng build` detrás de un proxy (Nginx/Apache) que envíe `/api/v1` y `/lp2_gp2/images` al backend.
