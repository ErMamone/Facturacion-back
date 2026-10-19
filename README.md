# Facturación — Backend

API REST para emitir facturas entre empresas, generar su PDF a partir de una plantilla y guardarlo en S3 con
vencimiento automático.

**Stack:** Java 25 (LTS) · Spring Boot 4.1 · PostgreSQL 18 + Flyway · AWS SDK v2 (S3) · Thymeleaf + OpenHTMLtoPDF ·
Lombok · MapStruct · Testcontainers · ArchUnit

---

## Arquitectura

Clean Architecture (puertos y adaptadores). Las dependencias apuntan siempre hacia adentro y se verifican en
cada build con ArchUnit ([`ArquitecturaTest`](src/test/java/ar/com/facturacion/ArquitecturaTest.java)).

```
ar.com.facturacion
├── domain                      Núcleo. Java puro: sin Spring, JPA ni AWS.
│   ├── model                   Factura (agregado), Item, Empresa, Cuit (value object), Moneda
│   └── exception               ReglaDeNegocioException, FacturaNoEncontradaException
│
├── application                 Casos de uso. Tampoco conoce Spring.
│   ├── port.in                 EmitirFactura, BuscarFacturas, ConsultarFactura, DescargarPdf, PurgarFacturasVencidas
│   ├── port.out                FacturaRepository, EmpresaRepository, GeneradorPdf, AlmacenamientoPdf, UnidadDeTrabajo
│   ├── service                 Implementaciones de los casos de uso
│   └── model                   Commands, filtros, paginado y modelos de lectura
│
└── infrastructure              Adaptadores: todo lo que es framework vive acá.
    ├── web                     Controller, DTOs, MapStruct, ProblemDetail, @CuitValido
    ├── persistence             Entidades JPA, Spring Data, MapStruct, adaptadores de los repos
    ├── storage                 S3 (put, presign, delete por lotes)
    ├── pdf                     Thymeleaf → HTML → PDF
    ├── transaction             UnidadDeTrabajo sobre TransactionTemplate
    ├── scheduling              Disparador de la purga
    └── config                  Composition root (beans de casos de uso), propiedades, CORS
```

### Decisiones

| Tema | Decisión | Por qué |
|---|---|---|
| Invariantes | Viven en el dominio (`Factura`, `Item`, `Cuit`) | Bean Validation en la web da errores por campo, pero la garantía es el dominio: no se puede construir una factura inválida |
| Dinero | `BigDecimal` / `NUMERIC(15,2)`, total redondeado `HALF_UP` una sola vez | Nunca `double`. El total del cliente se ignora |
| Empresas | Upsert atómico `INSERT … ON CONFLICT` por CUIT, en orden de CUIT | Último nombre gana; sin carreras ni deadlocks entre altas cruzadas |
| Emisión | PDF → S3 → transacción DB; si la DB falla se borra el PDF (compensación) | No se retiene una conexión de DB durante la llamada a S3 y no quedan archivos huérfanos |
| Descarga | `302` a URL firmada de S3 (5 min) | El backend nunca transporta el archivo; la DB queda liviana |
| Vencimiento | Toda consulta filtra `expires_at > now()`; la purga borra PDF → fila → empresas sin facturas | Lo vencido no se ve aunque la purga no haya corrido. Si S3 falla la fila queda y se reintenta |
| Transacciones | Puerto `UnidadDeTrabajo` | La capa de aplicación no depende de `@Transactional` |
| Mapeos | MapStruct (web ↔ aplicación ↔ dominio ↔ JPA ↔ vista PDF) | Sin mapeo a mano, y falla en compilación si un campo queda sin mapear |
| Errores | `ProblemDetail` (RFC 9457) con `errors: [{campo, mensaje}]`, mensajes en castellano | El frontend puede marcar cada input |

---

## Correr en local

Requisitos: Docker. Java 25 solo si se corre desde el IDE.

```bash
docker compose up -d --build
```

Levanta Postgres, LocalStack (S3, con el bucket ya creado) y la app en `http://localhost:8080`.

Para desarrollar desde el IDE, levantar solo la infraestructura y correr `FacturacionApplication` (el perfil
`local` es el default):

```bash
docker compose up -d db localstack
```

```bash
./mvnw spring-boot:run
```

### Probar

```bash
curl -i localhost:8080/api/facturas -H "Content-Type: application/json" -d '{"vendedor":{"cuit":"20123456786","nombre":"Taller"},"comprador":{"cuit":"30712345671","nombre":"Cliente SRL"},"moneda":"ARS","items":[{"nombre":"Cambio de aceite","cantidad":2,"precio":15000.50}]}'
```

---

## Perfiles y configuración

| Perfil | Cuándo | Qué hace |
|---|---|---|
| `local` (default) | IDE / docker compose | Postgres y LocalStack locales, logs legibles, facturas vencen a los 10 min y la purga corre cada minuto, la plantilla PDF se lee del código fuente sin caché |
| `prod` | AWS (`SPRING_PROFILES_ACTIVE=prod`, ya seteado en la imagen) | Todo por variables de entorno, logs JSON (ECS) a stdout, credenciales AWS por la cadena por defecto del SDK |

Variables del perfil `prod` (la app no arranca si falta alguna obligatoria):

| Variable | Obligatoria | Ejemplo |
|---|---|---|
| `SPRING_DATASOURCE_URL` | sí | `jdbc:postgresql://mi-rds:5432/facturacion` |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | sí | desde un Secret |
| `APP_BUCKET` | sí | `facturacion-pdfs-prod` |
| `AWS_REGION` | sí* | `sa-east-1` (*en EKS/ECS normalmente ya viene) |
| `APP_RETENTION` | no (`P30D`) | `PT10M` para la demo |
| `APP_CORS_ALLOWED_ORIGINS` | no | `https://facturacion.midominio.com` |
| `DB_POOL_SIZE` | no (`10`) | |

Credenciales AWS: **nunca** en variables ni en código. En EKS se usa IRSA (ver `deploy/k8s`), en ECS el task role.

---

## API

| Método | Path | Respuesta |
|---|---|---|
| POST | `/api/facturas` | `201 {id, total, moneda, expiresAt}` + `Location` |
| GET | `/api/facturas?cuitVendedor=&cuitComprador=&page=&size=` | Página de resúmenes, la más nueva primero. Al menos un filtro |
| GET | `/api/facturas/{id}` | Detalle con ítems |
| GET | `/api/facturas/{id}/pdf` | `302` a URL firmada (5 min). `404` si no existe o venció |
| GET | `/actuator/health/{liveness,readiness}` | Probes |

Validaciones: CUIT de 11 dígitos con dígito verificador, vendedor ≠ comprador, 1 a 50 ítems, cantidad entera > 0,
precio > 0 con hasta 2 decimales, moneda `ARS` o `USD`.

---

## PDF: cambiar el diseño

Todo el diseño está en un solo archivo: [`src/main/resources/templates/pdf/factura.html`](src/main/resources/templates/pdf/factura.html).
Arriba del archivo está documentado el contrato de variables (`factura`, `vendedor`, `comprador`, `items`,
`total`, `moneda`, `emitidaEl`, `venceEl`). Mientras no cambie ese contrato, la plantilla se reescribe entera sin
tocar Java.

- Con `docker compose` o el perfil `local`, la plantilla se lee en caliente: se guarda y la próxima factura ya sale
  con el cambio.
- CSS 2.1: el layout se arma con tablas (no hay flexbox ni grid).
- Logos/imágenes: dejarlos en `templates/pdf/` y referenciarlos relativo (`<img src="logo.png" />`).
- Los tests dejan un ejemplo en `target/factura-ejemplo.pdf`.
- En prod: editar → push → CI → deploy. Si algún día se quiere cambiar sin rebuild, montar la carpeta desde un
  ConfigMap y setear `SPRING_THYMELEAF_PREFIX=file:/ruta/` (es lo mismo que hace el compose).

---

## Tests

```bash
./mvnw verify
```

| Nivel | Qué cubre |
|---|---|
| Dominio | CUIT, cálculo de total, invariantes de factura e ítem |
| Aplicación (Mockito) | Orden de la emisión, compensación si falla la DB, purga por lotes y tolerancia a fallos de S3 |
| PDF | Genera un PDF real y verifica su texto con PDFBox |
| API (Testcontainers) | Postgres + LocalStack reales: alta, búsqueda, detalle, descarga por URL firmada, validaciones, vencimiento y purga |
| Arquitectura (ArchUnit) | Dirección de dependencias entre capas y núcleo libre de frameworks |

Requieren Docker para Testcontainers.

---

## Despliegue en AWS

1. **CI** ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)): tests → build de la imagen → escaneo con Trivy
   (corta si hay CVEs CRITICAL/HIGH con fix) → push a ECR. Se autentica con OIDC, sin access keys. Variables del
   repo: `AWS_REGION`, `AWS_ROLE_ARN`, `ECR_REPOSITORY`.
2. **EKS** ([`deploy/k8s/facturacion-back.yaml`](deploy/k8s/facturacion-back.yaml)): ServiceAccount con IRSA,
   ConfigMap, Deployment con probes, usuario no root y filesystem de solo lectura, y Service.
3. Permisos mínimos del rol IAM:

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": ["s3:PutObject", "s3:GetObject", "s3:DeleteObject"],
    "Resource": "arn:aws:s3:::<bucket>/facturas/*"
  }]
}
```

La imagen es multi-stage, con capas de Spring Boot separadas (un cambio de código solo invalida la última capa),
JRE 25 y usuario no root.

---

## Pendientes / mejoras

- **ShedLock** para la purga: hoy se corre con 1 réplica para que el job no se ejecute duplicado (igual es idempotente).
- **Plantilla definitiva** del PDF: la actual es un placeholder prolijo, el diseño final lo define el usuario final.
- Versiones de Tomcat y Jackson fijadas en el `pom.xml` por CVEs: quitar al subir a un Spring Boot que ya las incluya.
