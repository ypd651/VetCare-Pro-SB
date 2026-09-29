# 🐾 Sistema de Gestión Veterinaria - Spring Boot MVC

Sistema web para la gestión de propietarios y mascotas en una clínica veterinaria, desarrollado con **Java 17**, **Spring Boot**, **Spring Data JPA** y base de datos en memoria **H2**.

---

## 🚀 Tecnologías Utilizadas

- **Lenguaje:** Java 17
- **Framework:** Spring Boot 3.2.x
- **Persistencia & ORM:** Spring Data JPA / Hibernate
- **Base de Datos:** H2 Database (En memoria)
- **Utilidades:** Lombok
- **Gestor de Dependencias:** Maven
- **Arquitectura:** MVC (Model - View - Controller / DTO - Service - Repository)

---

## 📁 Estructura del Proyecto

```text
veterinaria/
├── src/
│   ├── main/
│   │   ├── java/com/veterinaria/veterinaria/
│   │   │   ├── controllers/         # Endpoints de la API REST
│   │   │   │   ├── MascotaController.java
│   │   │   │   └── PropietarioController.java
│   │   │   ├── dto/                 # Objetos de Transferencia de Datos
│   │   │   │   ├── CreateMascotaRequest.java
│   │   │   │   ├── CreatePropietarioRequest.java
│   │   │   │   ├── MascotaResponse.java
│   │   │   │   └── PropietarioResponse.java
│   │   │   ├── entities/            # Entidades JPA (Mapeo BD)
│   │   │   │   ├── Mascota.java
│   │   │   │   └── Propietario.java
│   │   │   ├── repository/          # Interfaces JpaRepository
│   │   │   │   ├── MascotaRepository.java
│   │   │   │   └── PropietarioRepository.java
│   │   │   ├── servicies/           # Lógica de Negocio (Interfaces e Impl)
│   │   │   │   ├── MascotaService.java
│   │   │   │   ├── PropietarioService.java
│   │   │   │   └── impl/
│   │   │   │       ├── MascotaServiceImpl.java
│   │   │   │       └── PropietarioServiceImpl.java
│   │   │   └── VeterinariaApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
└── pom.xml
