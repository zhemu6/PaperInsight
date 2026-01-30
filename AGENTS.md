# AGENTS.md - PaperInsight

Agent-facing engineering guide for `F:\code\Java\AgentScope\PaperInsight`.

## Rule Sources

- Primary: `AGENTS.md`
- Also relevant: `README.md`, `pom.xml`, `frontend/package.json`, `frontend/eslint.config.js`, `frontend/tsconfig.json`, `frontend/vite.config.ts`
- GitHub Actions: `.github/workflows/opencode.yml`, `frontend/.github/workflows/ci.yml`, `frontend/.github/workflows/gh-pages.yml`
- No Cursor/Copilot rules found: no `.cursorrules`, no `.cursor/rules/*`, no `.github/copilot-instructions.md`

## Agent Operating Constraints (MUST FOLLOW)

1. Verification
   - Backend changes: run `mvn test` (even if there are currently no tests, it catches config/compile issues)
   - Frontend changes: run `pnpm typecheck` and `pnpm lint` in `frontend/`
2. Scope discipline
   - No refactors while fixing bugs; preserve folder structure and routing under `frontend/src/pages/`
   - Do not edit generated files unless explicitly requested (see `frontend/src/api/`)
3. Secrets
   - Never commit secrets; runtime secrets are injected via `.env` / docker-compose env vars

## Commands

### Backend (Spring Boot 3.5.10, Java 21, Maven)

| Action | Command | Notes |
|---|---|---|
| Build (compile) | `mvn clean compile` | Fast compile check |
| Run (dev) | `mvn spring-boot:run` | Local dev server (often proxied from frontend to `http://localhost:8123`) |
| Test (all) | `mvn test` | Surefire |
| Test (single class) | `mvn test -Dtest=ClassName` | Surefire: https://maven.apache.org/surefire/maven-surefire-plugin/examples/single-test.html |
| Test (single method) | `mvn test -Dtest=ClassName#methodName` | Surefire supports patterns |
| Package | `mvn clean package -DskipTests` | Used by `Dockerfile` build stage |

### Frontend (Vue 3 + Vite, TypeScript, pnpm)

| Action | Command | Notes |
|---|---|---|
| Install | `pnpm install` | `frontend/package.json` pins `pnpm@10.14.0` |
| Dev | `pnpm dev` | Vite dev server (default `5173`); proxies `/api` to backend (`frontend/vite.config.ts`) |
| Build | `pnpm build` | Vite build |
| Lint | `pnpm lint` | `eslint .` via `@antfu/eslint-config` |
| Typecheck | `pnpm typecheck` | `vue-tsc --noEmit`, `strict: true` |
| API client gen | `pnpm openapi` | Generates `frontend/src/api/*` from `http://localhost:8123/api/v3/api-docs` (`frontend/openapi.config.js`) |
| Tests | (none) | No test runner configured in `frontend/package.json` |

### Docker / Deploy

| Action | Command | Notes |
|---|---|---|
| Deploy local stack | `./deploy.sh` | Runs `docker-compose up -d --build` |
| Compose file | `docker-compose.yml` | Frontend on `:80`, backend on `:8080`, plus MySQL/Redis/RabbitMQ/Elasticsearch |

### CI (GitHub Actions)

- `.github/workflows/opencode.yml`: runs Opencode bot on PR comments (not a build/test CI)
- `frontend/.github/workflows/ci.yml`: runs `npm run typecheck` on pushes to `main`
- `frontend/.github/workflows/gh-pages.yml`: runs `npm run build` and deploys `dist/` to GitHub Pages

## Project Structure

```text
src/main/java/com/zhemu/paperinsight/
  agent/          AgentScope agents, tools, configs
  controller/     REST endpoints (@RestController)
  service/        Service interfaces
  service/impl/   Service implementations (@Service)
  mapper/         MyBatis-Plus mappers
  model/entity/   DB entities (MyBatis-Plus annotations)
  model/dto/      Request DTOs (Jakarta Validation)
  model/vo/       Response VOs
  exception/      ErrorCode, BusinessException, ThrowUtils, GlobalExceptionHandler
  aop/            AuthCheck + interceptors, UserContext wiring
  mq/             RabbitMQ producers/consumers

frontend/src/
  pages/          File-based routes (unplugin-vue-router)
  layouts/        Layout shells (Element Plus containers)
  components/     Shared components (auto-registered)
  stores/         Pinia stores (setup store + persistedstate)
  api/            Generated OpenAPI client (do not hand-edit)
  styles/         SCSS + Element Plus theme vars
```

## Backend Style (Java)

### DTO / Entity / VO

- DTOs are classes with Lombok + Jakarta Validation (e.g. `@NotBlank`) (see `src/main/java/com/zhemu/paperinsight/model/dto/`)
- Entities use MyBatis-Plus annotations and Lombok (`@Data`, `@Builder`, `@TableLogic`) (see `src/main/java/com/zhemu/paperinsight/model/entity/`)
- VOs are mutable objects used for responses and mapping (see `src/main/java/com/zhemu/paperinsight/model/vo/`)

### Controllers

- Return type: `BaseResponse<T>` (see `src/main/java/com/zhemu/paperinsight/common/BaseResponse.java`)
- Success: `ResultUtils.success(data)`
- Validation: prefer `@RequestBody @Validated` DTOs
- Auth: use `@AuthCheck` for endpoints requiring login/role (see `src/main/java/com/zhemu/paperinsight/annotation/AuthCheck.java`)

### Error Handling

- Throw domain errors via `ThrowUtils.throwIf(...)` and `BusinessException` + `ErrorCode`
- Centralized handling: `src/main/java/com/zhemu/paperinsight/exception/GlobalExceptionHandler.java`
- Note: SSE endpoints return errors as `text/event-stream` events (`event: business-error`) when applicable
- Avoid `catch (Exception)` unless truly for degradation; log (or document why it is intentionally ignored)

### Persistence / Multi-tenancy

- MyBatis-Plus tenant line plugin reads `UserContext.getUserId()` and injects `user_id` (see `src/main/java/com/zhemu/paperinsight/config/MyBatisPlusConfig.java`)
- Ensure request paths that must query across tenants are explicitly excluded in `ignoreTable`.

## Frontend Style (Vue 3 + TS)

### Formatting (enforced)

- ESLint: `frontend/eslint.config.js` uses `@antfu/eslint-config` with `formatters: true`, `unocss: true`, `vue: true`
- No semicolons; single quotes in JS/TS; Stroustrup braces (`} \n else {`)
- Prefer `import type` for type-only imports; imports are auto-sorted by lint rules

### TypeScript

- `strict: true` in `frontend/tsconfig.json`
- Prefer explicit types at module boundaries (store state, API responses)

### Imports

- Use `~/` alias for `frontend/src/` (see `frontend/vite.config.ts`, `frontend/tsconfig.json`)

### Vue components

- Use `<script setup lang="ts">`
- File routing under `frontend/src/pages/` (unplugin-vue-router)
- Components auto-imported/registered (see `frontend/vite.config.ts`)

### API client generation

- `frontend/src/api/` is generated; it may contain `// @ts-ignore` and eslint disables
- Do not hand-edit generated files; re-run `pnpm openapi` after backend API changes

### HTTP + errors

- Central request wrapper: `frontend/src/request.ts` (Axios)
- Convention: backend success is `code === 0`; auth error is `40100` and triggers redirect

