# Recallr API Architecture

## Project overview

This repository is a Spring Boot REST API for Recallr, a study/knowledge application organized around users, notebooks, decks, notes, and flashcards.

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
│   ├── notebook/                    # Notebook CRUD (user-owned)
│   ├── deck/                        # Deck CRUD (optionally nested under a notebook)
│   ├── note/                        # Note CRUD nested under notebooks, plus note-link model
│   └── flashcard/                   # Unified flashcard hierarchy (BASIC and MCQ types)
├── global/                          # Global exception-to-HTTP mapping
└── security/                        # Spring Security config, JWT filter, current-user provider
```

Tests are under `src/test/java/com/atinroy/recallr`, grouped by domain service: `notebook`, `deck`, `note`, `flashcard`, plus a bootstrapping `ApiApplicationTests`.

## Domain model

```
User
├── Notebook          (user-owned; name, description)
│   ├── Note          (belongs to Notebook — required; title, content, note-links)
│   └── Deck          (optional: a deck can also be standalone)
└── Deck              (user-owned; notebook FK nullable for standalone decks)
    └── Flashcard     (abstract; SINGLE_TABLE inheritance)
        ├── BasicFlashcard   (question, answer, reverse)
        └── MCQFlashcard     (question, options[], correctOptionIndex, explanation)
```

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
- `domain/notebook/`
  - Entity: `Notebook`
  - Notebooks belong to a `User`.
  - Controller/service/repository/mapper for notebook CRUD.
- `domain/deck/`
  - Entity: `Deck`
  - Decks belong to a `User` and optionally to a `Notebook` (nullable FK).
  - `DeckController` handles both notebook-nested routes and standalone deck routes.
- `domain/note/`
  - Entities: `Note`, `NoteLink`
  - Notes belong to a `User` and `Notebook` (required FK).
  - `NoteLink` connects source and target notes with an optional label.
  - `NoteLinkService` creates/updates/deletes links between notes. There is no `NoteLinkController`; note-link management is intentionally not yet exposed via HTTP.
- `domain/flashcard/`
  - Abstract entity: `Flashcard` (SINGLE_TABLE inheritance, discriminator column `type`)
  - Subtypes: `BasicFlashcard` (question, answer, reverse), `MCQFlashcard` (question, options[], correctOptionIndex, explanation)
  - All flashcard types are managed through a single unified controller/service under `/decks/{deckId}/flashcards`.

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

### Notebook flow

Routes: `/api/v1/notebooks`, `/api/v1/notebooks/{notebookId}`

- `NotebookController` delegates to `NotebookService`.
- `NotebookService` scopes all reads/updates/deletes by the current user.
- `NotebookMapper` maps between request/response DTOs and the `Notebook` entity.

### Deck flow

Routes:
- `POST /api/v1/notebooks/{notebookId}/decks` — create a deck inside a notebook
- `GET /api/v1/notebooks/{notebookId}/decks` — list decks in a notebook
- `POST /api/v1/decks` — create a standalone deck
- `GET/PUT/DELETE /api/v1/decks/{deckId}` — standalone deck operations

- `DeckController` handles both notebook-nested and standalone routes in a single controller without a common `@RequestMapping` prefix.
- `DeckService` verifies notebook ownership (when creating inside a notebook) via `NotebookRepository.findByIdAndUserId()`, and deck ownership via `DeckRepository.findByIdAndUserId()` for all other operations.

### Note flow

Routes: `/api/v1/notebooks/{notebookId}/notes`, `/api/v1/notebooks/{notebookId}/notes/{noteId}`

- `NoteController` is nested under notebooks.
- `NoteService` verifies that the parent notebook belongs to the current user before accessing or mutating notes.
- Note lookup uses `NoteRepository.findByIdAndNotebookId()` after notebook ownership is verified.
- `NoteMapper` derives the note `user` from `notebook.getUser()` on create.

### Flashcard flow

Routes: `/api/v1/decks/{deckId}/flashcards`, `/api/v1/decks/{deckId}/flashcards/{flashcardId}`

- `FlashcardController` handles all flashcard types through a single unified endpoint.
- `FlashcardService` first verifies deck ownership via `DeckRepository.findByIdAndUserId()` before all operations.
- Individual flashcard lookup uses `FlashcardRepository.findByIdAndDeckId()`.
- `FlashcardMapper` reads the `type` discriminator from the request and constructs the correct subtype entity.

### Flashcard design decision: SINGLE_TABLE inheritance

All flashcard types (`BASIC`, `MCQ`, and future types) live in one `flashcards` table with a `type` discriminator column. Type-specific fields are nullable for other types. This was chosen because the dominant access pattern — listing all cards in a deck — is a free single-table query with no joins. New types require a small migration (adding columns) but keep full JPA type safety. Jackson `@JsonTypeInfo` polymorphism on the `type` field gives a unified API (`/decks/{deckId}/flashcards`) that returns mixed types in one list.

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
- `notebooks`
- `decks`
- `notes`
- `note_links`
- `flashcards`
- `flashcard_options`

Important relationships:

- `users` → `notebooks`, `decks`, `notes`, `flashcards`, `refresh_tokens`
- `notebooks` → `decks` (nullable; a deck may exist without a notebook), `notes` (required)
- `decks` → `flashcards`
- `note_links` links notes by source and target, unique on `(source_note_id, target_note_id)`
- `flashcards` uses SINGLE_TABLE inheritance; the `type` column (`VARCHAR(31)`) discriminates subtypes. `flashcard_options` stores MCQ option strings as a collection table.

### External integrations

- PostgreSQL is the only concrete external runtime dependency visible in configuration.
- Spring Mail dependency is present in `pom.xml`, but no mail-sending code or mail configuration was found in the inspected source.
- Identity-provider modeling supports external providers (`GOOGLE`, `GITHUB`, `FACEBOOK` according to comments), but only local email/password auth is implemented in the current controllers/services.

## Testing strategy

Tests are primarily service-level unit tests with Mockito:

- `NotebookServiceTest` covers notebook create/read/update/delete and not-found behavior.
- `DeckServiceTest` covers notebook-nested and standalone deck creation, deck CRUD, parent notebook resolution, and exceptions.
- `NoteServiceTest` covers note CRUD, notebook ownership validation, and not-found cases.
- `NoteLinkServiceTest` covers link creation/update/delete and note/link not-found cases.
- `FlashcardServiceTest` covers flashcard CRUD for both `BASIC` and `MCQ` types, deck ownership validation, and not-found cases.
- `ApiApplicationTests` is a Spring Boot context-load test (requires a live PostgreSQL instance).

The tests map directly to service classes rather than controller integration tests. No repository integration tests or end-to-end HTTP/security tests were found in the inspected tree.

## Architectural notes and dependencies

- The project uses a conventional layered architecture:
  - Controller: HTTP mapping and request/response boundaries
  - Service: business rules, transactions, ownership checks
  - Repository: Spring Data JPA persistence
  - Mapper: entity/DTO conversion
- Authorization is enforced in services by querying resources through current-user or parent-resource scoped repository methods.
- Nested resources use parent ownership as the primary boundary: notes are accessed through a verified notebook; flashcards are accessed through a verified deck.
- JWT access tokens are stateless, but refresh tokens are stateful and persisted by hash, enabling revocation and reuse detection.
- Entities use UUID IDs generated in the Java domain layer rather than database sequences.
- JPA schema generation is disabled for mutation (`ddl-auto: validate`); schema changes should be made through Flyway migrations.
- `open-in-view: false` means lazy relationships must be accessed within transactional/service boundaries or eagerly loaded when needed.

## Unknowns / ambiguities

- `NoteLinkService` has no matching controller; note-link API endpoints are not yet exposed via HTTP. This is intentional — the service layer exists but the HTTP surface is deferred.
- Spring Mail is included but unused in the inspected source.
- External OAuth providers are modeled but not implemented in controllers/services.
- Production environment configuration is not present; JWT secret, datasource settings, secure cookie settings, and CORS origins likely need environment-specific overrides.
- The repository appears to be part of a larger `apps` structure with a sibling web app, but this document covers the API application under `apps/api`.

## Onboarding summary

Start with `ApiApplication.java`, then read `SecurityConfig` and `JwtAuthenticationFilter` to understand how every request is authenticated. For domain behavior, follow the pattern used by `NotebookController` → `NotebookService` → `NotebookRepository` → `NotebookMapper`; decks, notes, and flashcards use the same shape with additional parent-resource ownership checks. Persistence is defined by JPA entities and validated against the Flyway schema in `V1__init_db.sql`. Service tests under `src/test/java` are the best executable documentation for domain rules and exception behavior.
