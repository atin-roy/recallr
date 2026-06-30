# Recallr API Architecture

## Project overview

This repository is a Spring Boot REST API for Recallr, a study/knowledge application organized around users, subjects, topics, notes, note links, and MCQs.

- Application entrypoint: `src/main/java/com/atinroy/recallr/ApiApplication.java`
- HTTP base path: `/api/v1` via `spring.mvc.servlet.path` in `src/main/resources/application.yaml`
- Main framework: Spring Boot 4.1, Spring MVC, Spring Security, Spring Data JPA, Flyway
- Database: PostgreSQL, with schema managed by Flyway migration `src/main/resources/db/migration/V1__init_db.sql`
- Authentication: stateless JWT access tokens plus persisted/rotated refresh tokens in an HttpOnly cookie

## Directory/package structure

```text
src/main/java/com/atinroy/recallr
├── ApiApplication.java              # Spring Boot entrypoint
├── auth/                            # Registration, login, JWT, refresh-token lifecycle
├── common/                          # Shared base entity and common exceptions
├── domain/
│   ├── user/                        # User, roles, identity providers, user repositories
│   ├── subject/                     # Subject CRUD
│   ├── topic/                       # Topic CRUD nested under subjects
│   ├── note/                        # Note CRUD and note-link service/model
│   └── mcq/                         # Multiple-choice question CRUD
├── global/                          # Global exception-to-HTTP mapping
└── security/                        # Spring Security config, JWT filter, current-user provider
```

Tests are under `src/test/java/com/atinroy/recallr`, grouped by domain service: `subject`, `topic`, `note`, `mcq`, plus a bootstrapping `ApiApplicationTests`.

## Core components and responsibilities

### Entry point

- `ApiApplication.java` contains `@SpringBootApplication` and starts component scanning under `com.atinroy.recallr`.

### Auth module (`auth/`)

- `AuthController` exposes:
  - `POST /auth/register`
  - `POST /auth/login`
  - `GET /auth/me`
  - `POST /auth/refresh`
  - `POST /auth/logout`
- `AuthService` handles registration, credential authentication, access-token creation, and refresh-token rotation.
- `JwtService` signs and validates JWTs using the configured Base64 secret.
- `RefreshTokenService` creates refresh-token JWTs, stores SHA-256 hashes in the database, validates tokens, detects reuse, revokes tokens, and rotates tokens on refresh.
- `RefreshTokenCookie` centralizes refresh-token cookie settings.
- `RefreshTokenRepository` provides JPA access to `refresh_tokens`, including pessimistic locking by token hash and bulk revocation.

### Security module (`security/`)

- `SecurityConfig` configures stateless Spring Security:
  - CSRF disabled.
  - CORS allows `http://localhost:3000` and `http://localhost:3001` with credentials.
  - `POST /auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout` are public.
  - All other routes require authentication.
  - `JwtAuthenticationFilter` runs before username/password authentication.
- `JwtAuthenticationFilter` reads `Authorization: Bearer <token>`, validates `token_type=access_token`, loads the user by UUID, and populates `SecurityContext`.
- `CustomUserDetailsService` loads local users by email for login and users by UUID for JWT authentication.
- `AuthenticatedUserProvider` converts the current `SecurityContext` principal into a persisted `User` entity for services.

### Domain modules

Each main domain follows a controller → service → repository → mapper pattern.

- `domain/user/`
  - Entities: `User`, `UserProvider`
  - Enums: `Role`, `IdentityProvider`
  - Repositories: `UserRepository`, `UserProviderRepository`
  - DTO: `UserResponse`
  - Mapper: `UserMapper`
- `domain/subject/`
  - Entity: `Subject`
  - Controller/service/repository/mapper for subject CRUD
  - Subjects belong to a `User`.
- `domain/topic/`
  - Entity: `Topic`
  - Topics belong to a `Subject`.
  - Routes are nested under `/subjects/{subjectId}/topics`.
- `domain/note/`
  - Entities: `Note`, `NoteLink`
  - Notes belong to a `User` and `Subject`, with optional `Topic`.
  - `NoteLink` connects source and target notes.
  - `NoteLinkService` exists, but there is no controller exposing note-link endpoints in the current codebase.
- `domain/mcq/`
  - Entity: `MCQ`
  - MCQs belong to a `User` and `Subject`, with optional `Topic`.
  - Options are stored as a JPA `@ElementCollection` in `mcqs_options`.

### Shared/common components

- `BaseEntity` is a `@MappedSuperclass` used by persisted entities. It provides:
  - UUID primary key generated in Java.
  - `createdAt` and `updatedAt` via Hibernate timestamp annotations.
  - Spring Data `Persistable<UUID>` support to distinguish new entities.
- `GlobalControllerAdvice` maps domain/auth exceptions to JSON error responses with timestamp, status, error, and message.
- `BadRequestException` represents validation/business-rule failures not covered by bean validation.

## Request/data flow

### Authentication flow

#### Registration

1. `POST /api/v1/auth/register` → `AuthController.register()`
2. `AuthService.register()` checks `UserRepository.existsByEmail()`.
3. Password is BCrypt-hashed via `PasswordEncoder`.
4. `UserMapper.toEntity()` creates:
   - `User` with normalized lowercase email
   - `UserProvider` with `IdentityProvider.LOCAL`, provider ID equal to normalized email, and password hash
   - default `Role.USER`
5. `UserRepository.save()` persists the user, providers, and roles.
6. Response is `UserResponse`.

#### Login

1. `POST /api/v1/auth/login` → `AuthController.login()`
2. `AuthService.login()` authenticates through `AuthenticationManager`/`DaoAuthenticationProvider`.
3. `CustomUserDetailsService.loadUserByUsername()` loads user and local password hash.
4. `JwtService.generateAccessToken()` creates a short-lived access token.
5. `RefreshTokenService.generateRefreshToken()` creates a refresh-token JWT, stores its SHA-256 hash in `refresh_tokens`, and returns the raw token.
6. Controller writes the refresh token as an HttpOnly cookie and returns `LoginResponse` containing the access token and user.

#### Authenticated request

1. Client sends `Authorization: Bearer <access-token>`.
2. `JwtAuthenticationFilter` parses and validates the JWT.
3. User is loaded by UUID from the token subject.
4. A `UsernamePasswordAuthenticationToken` is stored in `SecurityContext`.
5. Domain services call `AuthenticatedUserProvider.getCurrentUser()` to enforce ownership checks.

#### Refresh/logout

- `POST /auth/refresh` reads the refresh-token cookie, validates the stored token hash, revokes the old token, creates a new refresh token, and returns a new access token.
- If a revoked refresh token is reused, `RefreshTokenService` revokes all tokens for that user.
- `POST /auth/logout` revokes the current refresh token if present and clears the cookie.

### Subject flow

Routes: `/api/v1/subjects`

- `SubjectController` delegates to `SubjectService`.
- `SubjectService` always scopes reads/updates/deletes by current user using `SubjectRepository.findByIdAndUserId()`.
- `SubjectMapper` maps between request/response DTOs and `Subject` entity.

### Topic flow

Routes: `/api/v1/subjects/{subjectId}/topics`

- `TopicService.resolveSubject()` first verifies that the parent subject belongs to the current user.
- Topic lookup uses `TopicRepository.findByIdAndSubjectId()`.
- This creates a parent-scoped authorization boundary: a user must own the subject before accessing its topics.

### Note flow

Routes: `/api/v1/subjects/{subjectId}/notes`

- `NoteService.resolveSubject()` verifies user ownership of the subject.
- Optional topic IDs are validated with `TopicRepository.findByIdAndSubjectId()`; a topic outside the subject produces `BadRequestException`.
- Note lookup uses `NoteRepository.findByIdAndSubjectId()` after subject ownership is verified.
- `NoteMapper` derives note `user` from `subject.getUser()` on create.

### MCQ flow

Routes: `/api/v1/subjects/{subjectId}/mcqs`

- Mirrors note flow.
- `MCQService` verifies subject ownership, validates optional topic membership, persists through `MCQRepository`, and maps with `MCQMapper`.

### Note-link flow

- `NoteLinkService` creates/updates/deletes links between notes.
- It validates both source and target notes by `NoteRepository.findByIdAndUserId()` to ensure both notes belong to the current user.
- Current ambiguity: no `NoteLinkController` is present, so this service is not exposed via HTTP in the inspected code.

## Configuration and environment setup

### Maven/build

`pom.xml` declares:

- Java `25`
- Spring Boot parent `4.1.0`
- Spring starters: MVC, Security, Validation, JPA, Flyway, Mail
- PostgreSQL runtime driver
- JJWT `0.13.0`
- Lombok for boilerplate reduction
- Springdoc OpenAPI UI (`springdoc-openapi-starter-webmvc-ui`)
- Spring Boot Docker Compose runtime support

### Application configuration

`src/main/resources/application.yaml`:

- Activates profile from `SPRING_PROFILES_ACTIVE`, defaulting to `local`.
- Sets app name to `api`.
- Configures JWT secret from `JWT_SECRET` unless overridden by profile.
- Access-token TTL: `900` seconds.
- Refresh-token TTL: `1209600` seconds.
- Refresh cookie:
  - name: `refresh_token`
  - path: `/api/v1/auth`
  - same-site: `Lax`
  - secure: `false` for local development
- JPA:
  - `ddl-auto: validate`
  - `open-in-view: false`
- Flyway enabled.
- Servlet path: `/api/v1`.

`src/main/resources/application-local.yaml` supplies a local JWT secret.

### Local database

`docker-compose.yaml` defines a PostgreSQL 18 service:

- DB: `recallr`
- User: `atinroy`
- Password: `123@tinRoy`
- Port: `5432`
- Persistent volume: `postgres_data`

No datasource URL/username/password is explicitly shown in `application.yaml`; Spring Boot Docker Compose support may provide connection details locally. If running without Docker Compose integration, datasource configuration may need to be supplied externally.

## Persistence / external services / integrations

### Database schema

Flyway migration `V1__init_db.sql` creates:

- `users`
- `user_providers`
- `user_roles`
- `refresh_tokens`
- `subjects`
- `topics`
- `notes`
- `note_links`
- `mcqs`
- `mcqs_options`

Important relationships:

- `users` → `subjects`, `notes`, `mcqs`, `refresh_tokens`
- `subjects` → `topics`, `notes`, `mcqs`
- `topics` may be set null on note/MCQ delete relationships via `ON DELETE SET NULL`
- `note_links` links notes by source and target, unique on `(source_note_id, target_note_id)`

### External integrations

- PostgreSQL is the only concrete external runtime dependency visible in configuration.
- Spring Mail dependency is present in `pom.xml`, but no mail-sending code or mail configuration was found in the inspected source.
- Identity-provider modeling supports external providers (`GOOGLE`, `GITHUB`, `FACEBOOK` according to comments), but only local email/password auth is implemented in the current controllers/services.

## Testing strategy

Tests are primarily service-level unit tests with Mockito:

- `SubjectServiceTest` covers subject create/read/update/delete and not-found behavior.
- `TopicServiceTest` covers parent subject resolution, topic CRUD, and exceptions.
- `NoteServiceTest` covers note CRUD, optional topic handling, invalid topic-in-subject checks, and not-found cases.
- `NoteLinkServiceTest` covers link creation/update/delete and note/link not-found cases.
- `MCQServiceTest` covers MCQ CRUD, subject/topic validation, and not-found cases.
- `ApiApplicationTests` is a Spring Boot context-load test.

The tests map directly to service classes rather than controller integration tests. No repository integration tests or end-to-end HTTP/security tests were found in the inspected tree.

## Architectural notes and dependencies

- The project uses a conventional layered architecture:
  - Controller: HTTP mapping and request/response boundaries
  - Service: business rules, transactions, ownership checks
  - Repository: Spring Data JPA persistence
  - Mapper: entity/DTO conversion
- Authorization is enforced mostly in services by querying resources through current-user or parent-subject scoped repository methods.
- Nested resources use subject ownership as the primary boundary: topics, notes, and MCQs are all accessed through a verified subject.
- JWT access tokens are stateless, but refresh tokens are stateful and persisted by hash, enabling revocation and reuse detection.
- Entities use UUID IDs generated in the Java domain layer rather than database sequences.
- JPA schema generation is disabled for mutation (`ddl-auto: validate`); schema changes should be made through Flyway migrations.
- `open-in-view: false` means lazy relationships must be accessed within transactional/service boundaries or eagerly loaded when needed.

## Unknowns / ambiguities

- `NoteLinkService` has no matching controller, so note-link API endpoints are either planned or missing.
- Spring Mail is included but unused in the inspected source.
- External OAuth providers are modeled but not implemented in controllers/services.
- Production environment configuration is not present; JWT secret, datasource settings, secure cookie settings, and CORS origins likely need environment-specific overrides.
- The repository appears to be part of a larger `apps` structure with a sibling web app, but this document covers the API application under `apps/api`.

## Onboarding summary

Start with `ApiApplication.java`, then read `SecurityConfig` and `JwtAuthenticationFilter` to understand how every request is authenticated. For domain behavior, follow the pattern used by `SubjectController` → `SubjectService` → `SubjectRepository` → `SubjectMapper`; topics, notes, and MCQs use the same shape with additional parent-subject checks. Persistence is defined by JPA entities and validated against the Flyway schema in `V1__init_db.sql`. Service tests under `src/test/java` are the best executable documentation for domain rules and exception behavior.
