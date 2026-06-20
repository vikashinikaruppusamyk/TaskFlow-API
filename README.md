# Full-Stack TaskFlow-API(ToDo) Application

A full-stack TaskFlow app built to practice and demonstrate core backend + frontend skills: REST API design, authentication with JWT, relational data persistence, and a vanilla JS client consuming the API.

## Overview

Users can register, log in, and manage a personal list of todos (create, view, update, delete, mark complete). The backend exposes a documented REST API (Swagger/OpenAPI) and the frontend is a lightweight HTML/CSS/JS client.

## Tech Stack

**Backend**
- Java 17, Spring Boot 4
- Spring Web (REST controllers)
- Spring Data JPA + PostgreSQL
- Spring Security + JWT (`io.jsonwebtoken`) for stateless authentication
- Bean validation (`jakarta.validation`)
- Lombok
- springdoc-openapi (Swagger UI)
- JUnit 5 (Spring Boot Test starter)

**Frontend**
- HTML5, CSS3, vanilla JavaScript (Fetch API)
- Token stored client-side and sent as a Bearer header on protected requests

## Project Structure

```
.
├── src/main/java/com/example/todoApp/
│   ├── controller/        # AuthController, TodoController
│   ├── service/            # UserService, TodoService
│   ├── repository/         # JPA repositories
│   ├── models/              # User, Todo entities
│   ├── utils/JwtUtil.java   # token generation & validation
│   ├── JwtFilter.java       # request-level JWT filter
│   └── SecurityConfig.java  # security / CORS rules
├── src/main/resources/application.properties
├── TodoFrontend/             # static HTML/CSS/JS client
│   ├── login.html
│   ├── register.html
│   ├── todos.html
│   ├── script.js
│   └── style.css
└── pom.xml
```

## Getting Started

### Prerequisites
- Java 17+
- Maven (or use the included `mvnw` wrapper)
- PostgreSQL running locally with a `todo` database (or update `application.properties` for your own DB)

### Run the backend
```bash
./mvnw spring-boot:run
```
The API starts on **http://localhost:8081**.

### Run the frontend
Open `TodoFrontend/login.html` directly in a browser (no build step required). Make sure `SERVER_URL` in `script.js` matches your backend address.

### API Docs
Once the backend is running, Swagger UI is available at:
```
http://localhost:8081/swagger-ui/index.html
```

## API Reference

### Auth

| Method | Endpoint | Body | Description |
|---|---|---|---|
| POST | `/auth/register` | `{ "email": "string", "password": "string" }` | Creates a new user. Returns `409` if email already exists. |
| POST | `/auth/login` | `{ "email": "string", "password": "string" }` | Validates credentials and returns a JWT. Returns `401` on bad credentials. |

**Register — success**
```json
// 200 OK
{ "message": "User registered" }
```

**Login — success**
```json
// 200 OK
{ "token": "<jwt-token>" }
```

### Todos
All routes below are under `/api/v1/todo` and expect an `Authorization: Bearer <token>` header.

| Method | Endpoint | Body | Description |
|---|---|---|---|
| GET | `/api/v1/todo` | — | Returns all todos |
| GET | `/api/v1/todo/{id}` | — | Returns a single todo, `404` if not found |
| GET | `/api/v1/todo/page?page=0&size=10` | — | Returns a paginated list |
| POST | `/api/v1/todo/create` | `{ "title": "string", "isCompleted": false }` | Creates a todo, returns `201` |
| PUT | `/api/v1/todo` | `{ "id": 1, "title": "string", "isCompleted": true }` | Updates an existing todo |
| DELETE | `/api/v1/todo/{id}` | — | Deletes a todo |

**Create — success**
```json
// 201 Created
{ "id": 1, "title": "Complete Spring Boot", "isCompleted": false }
```

## Features
- Email/password registration and login
- Stateless authentication using JWT
- CRUD operations on todos, including pagination
- Mark todos complete/incomplete from the UI
- Input validation on the API layer (e.g. title cannot be blank)

## Known Limitations / Roadmap
Documenting these openly to track next steps and show what I'd improve with more time:
- The JWT filter bean is currently registered but disabled in `SecurityConfig` — endpoints aren't yet enforcing auth at the filter level; enabling this and adding route-level role checks is the next step.
- JWT secret and DB credentials are hardcoded in source for local development — move to environment variables / a secrets manager before any real deployment.
- CORS is currently open to all origins (`*`) — should be scoped to the actual frontend origin.
- No automated tests yet beyond the default Spring Boot context-load test — adding unit/integration tests for controllers and services is planned.
- Frontend has no build tooling/framework; a future iteration could move to React for component reuse and state management.

## What I Learned
- Structuring a Spring Boot app with clear separation between controllers, services, and repositories
- Implementing stateless auth with JWT and wiring a custom filter into the Spring Security filter chain
- Designing a REST API consumed by a separate static frontend, including handling CORS
- Using Bean Validation and consistent error responses across endpoints
