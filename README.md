# 🐾 VetCare Pro · Sistema Integral de Gestión Veterinaria

Aplicación web full-stack desarrollada con **Spring Boot 3.5** y un **front-end clínico moderno integrado**, diseñada para resolver las necesidades operativas y médicas reales de una clínica veterinaria: gestión de propietarios y pacientes, agenda inteligente con detección de conflictos horarios, historial médico completo, control preventivo de vacunas con alertas automáticas de vencimiento y un panel operativo en tiempo real.

---

## 🎯 ¿Qué problemas resuelve?

1. **Agenda sin cruces ni errores:** Detecta automáticamente conflictos de horario por veterinario y por paciente dentro de una ventana de ±29 minutos (`409 Conflict`), evitando citas duplicadas.
2. **Seguimiento preventivo de vacunas y desparasitaciones:** Calcula de forma automática el estado de cada dosis (`VENCIDA`, `PROXIMA` a 15 días, `AL_DIA`, `COMPLETADA`) y genera alertas visibles con acceso directo a llamada o WhatsApp del propietario.
3. **Expediente Clínico Unificado:** Centraliza en una sola ficha lateral (e imprimible/exportable a PDF) los datos de la mascota, **alertas de alergias**, contacto del propietario, historial de consultas con signos vitales, vacunas y citas.
4. **Sincronización automática en consulta:** Al registrar una consulta médica asociada a una cita, la cita pasa automáticamente a `COMPLETADA` y el peso del paciente se actualiza en su ficha clínica.
5. **Validación y manejo robusto de errores:** Respuestas HTTP semánticas (`400`, `404`, `409`) con detalle por campo para mostrar mensajes claros directamente en el formulario del front-end.
6. **Cero fricción de despliegue:** El front-end se sirve integrado desde Spring Boot y la base de datos H2 persiste en archivo local por defecto, sin requerir instalaciones externas ni compilación Node.

---

## 🚀 Tecnologías Utilizadas

### Backend
- **Java 17** (compatible con ejecución en JDK 17, 21 y 25 LTS)
- **Spring Boot 3.5.16** (Web, Data JPA, Validation, Actuator)
- **Hibernate / Spring Data JPA** con consultas optimizadas (`JOIN FETCH`) y `open-in-view=false`
- **H2 Database** (persistencia local en archivo `./data/veterinariadb` + consola web `/h2-console`)
- **Lombok 1.18.48**
- **SpringDoc OpenAPI / Swagger UI 2.8.17** (`/swagger-ui.html`)
- **JUnit 5 & MockMvc** (pruebas de integración end-to-end)

### Front-end (Integrado en `src/main/resources/static`)
- **HTML5 semántico + CSS3 moderno** (variables CSS, Grid/Flexbox, diseño responsive y modo oscuro/claro persistente)
- **JavaScript ES6+ modular** (`api.js`, `utils.js`, `app.js`) sin dependencias de build externas
- **Atajos de teclado** (`Ctrl + K` o `/` para búsqueda global instantánea, `Esc` para cerrar modales/expedientes)
- **Soporte de impresión (`@media print`)** para imprimir o exportar el expediente clínico a PDF

### Despliegue
- **Maven Wrapper (`mvnw` / `mvnw.cmd`)**
- **Docker & Docker Compose** (multi-stage build con healthcheck y volumen persistente)

---

## 📁 Estructura del Proyecto

```text
Veterinaria-Spring-Boot-taller/
├── Dockerfile
├── docker-compose.yml
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/veterinaria/
    │   │   ├── VeterinariaApplication.java
    │   │   ├── config/
    │   │   │   ├── DataInitializer.java         # Datos iniciales de demostración
    │   │   │   └── OpenApiConfig.java           # Configuración Swagger / OpenAPI
    │   │   ├── controllers/
    │   │   │   ├── CitaController.java
    │   │   │   ├── ConsultaMedicaController.java
    │   │   │   ├── DashboardController.java
    │   │   │   ├── MascotaController.java
    │   │   │   ├── PropietarioController.java
    │   │   │   └── VacunaController.java
    │   │   ├── dto/                             # Request & Response DTOs con Bean Validation
    │   │   ├── entities/                        # Entidades JPA y Enums del dominio
    │   │   ├── exceptions/                      # Excepciones de negocio y GlobalExceptionHandler
    │   │   ├── repository/                      # Repositorios JPA con consultas optimizadas
    │   │   └── services/                        # Interfaces e implementaciones de lógica de negocio
    │   │       └── impl/
    │   └── resources/
    │       ├── application.properties
    │       └── static/                          # Front-end profesional integrado
    │           ├── index.html
    │           ├── css/styles.css
    │           └── js/
    │               ├── api.js
    │               ├── app.js
    │               └── utils.js
    └── test/
        ├── java/com/veterinaria/
        │   ├── VeterinariaApplicationTests.java
        │   └── VeterinariaApiIntegrationTest.java
        └── resources/
            └── application.properties
```

---

## ✨ Módulos y Funcionalidades

- **Panel Principal (Dashboard):** KPIs interactivos, agenda del día con cambio rápido de estado y atención directa, alertas de vacunación vencidas/próximas, próximas revisiones y distribución de pacientes por especie.
- **Agenda de Citas:** Filtros por hoy, próximas, urgencias, completadas y canceladas; control de prioridades (`NORMAL`, `ALTA`, `URGENTE`) y validación de conflictos de horario.
- **Pacientes (Mascotas):** Filtros por especie o propietario, alertas visuales de alergias, registro de microchip, esterilización y acceso al **Expediente Clínico** completo.
- **Propietarios:** Búsqueda por nombre, documento, teléfono o correo; enlace rápido a llamada/WhatsApp y creación directa de mascotas asociadas.
- **Consultas Médicas:** Registro de signos vitales (peso, temperatura, frecuencia cardíaca), síntomas, diagnóstico, tratamiento y próxima revisión.
- **Vacunas y Desparasitación:** Clasificación automática de vencimientos (`VENCIDA`, `PROXIMA`, `AL_DIA`) y registro de refuerzos en 1 clic.

---

## 🔌 Endpoints Principales de la API REST

| Recurso | Métodos | Ruta Base | Descripción |
| :--- | :--- | :--- | :--- |
| **Dashboard** | `GET` | `/api/dashboard` | Métricas globales, citas de hoy y alertas |
| **Propietarios** | `GET, POST, PUT, DELETE` | `/api/propietarios` | CRUD completo y búsqueda (`?q=`) |
| **Mascotas** | `GET, POST, PUT, DELETE` | `/api/mascotas` | CRUD, filtros (`?q=`, `?especie=`, `?propietarioId=`) |
| **Expediente** | `GET` | `/api/mascotas/{id}/expediente` | Ficha clínica integral del paciente |
| **Citas** | `GET, POST, PUT, PATCH, DELETE` | `/api/citas` | Agenda, cambio de estado (`/{id}/estado`) y conflictos |
| **Consultas** | `GET, POST, PUT, DELETE` | `/api/consultas` | Historial médico y signos vitales |
| **Vacunas** | `GET, POST, PUT, DELETE` | `/api/vacunas` | Control preventivo y alertas (`?estado=ALERTAS`) |

---

## 🛠️ Cómo Desplegar Fácilmente

### Opción 1: Inicio rápido en Windows (1 clic)

Haz doble clic en [`iniciar-vetcare.bat`](iniciar-vetcare.bat) o ejecútalo desde terminal dentro de `Veterinaria-Spring-Boot-taller`:
```powershell
.\iniciar-vetcare.bat
```
Compilará si es necesario, iniciará el servidor Spring Boot y abrirá automáticamente el front-end en **http://localhost:8080**.

---

### Opción 2: Ejecución local con Maven Wrapper (Recomendada para desarrollo)

Requisitos previos: **Java 17 o superior** instalado (no necesitas instalar Maven manualmente).

**En Windows (PowerShell o CMD):**
```powershell
cd Veterinaria-Spring-Boot-taller
.\mvnw.cmd spring-boot:run
```

**En Linux / macOS:**
```bash
cd Veterinaria-Spring-Boot-taller
./mvnw spring-boot:run
```

Una vez iniciado, abre en tu navegador:
- **Aplicación Web (Front-end):** http://localhost:8080
- **Documentación API (Swagger UI):** http://localhost:8080/swagger-ui.html
- **Consola H2:** http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:file:./data/veterinariadb`, Usuario: `sa`, Contraseña vacía)
- **Estado de salud (Actuator):** http://localhost:8080/actuator/health

---

### Opción 3: Empaquetar y ejecutar como JAR independiente

```bash
./mvnw clean package
java -jar target/veterinaria-1.0.0.jar
```
*(En Windows usa `.\mvnw.cmd clean package`)*

---

### Opción 4: Despliegue en un comando con Docker Compose

Requisitos previos: **Docker** y **Docker Compose**.

```bash
docker compose up -d --build
```

La aplicación quedará disponible en **http://localhost:8080** con volumen persistente (`veterinaria_data`) para conservar los datos entre reinicios.

Para detenerla:
```bash
docker compose down
```

---

## ⚙️ Variables de Entorno Opcionales

| Variable | Valor por defecto | Descripción |
| :--- | :--- | :--- |
| `PORT` | `8080` | Puerto HTTP del servidor |
| `SPRING_DATASOURCE_URL` | `jdbc:h2:file:./data/veterinariadb` | Cadena JDBC de la base de datos |
| `SPRING_DATASOURCE_USERNAME` | `sa` | Usuario de base de datos |
| `SPRING_DATASOURCE_PASSWORD` | *(vacío)* | Contraseña de base de datos |
| `APP_SEED_ENABLED` | `true` | Carga datos de demostración si la base está vacía |

---

## 🧪 Ejecutar Pruebas Automatizadas

Para validar compilación, reglas de negocio, control de conflictos, validaciones y disponibilidad del front-end:

```powershell
.\mvnw.cmd test
```
*(En Linux/macOS: `./mvnw test`)*
