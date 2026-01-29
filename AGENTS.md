# AGENTS.md - PaperInsight Development Guide

> AI-driven academic paper management with AgentScope multi-agent framework.

## 🤖 Agent Behavior Guidelines (MUST FOLLOW)

1.  **Code Style**:
    *   **Backend**: Strict Java 21+ style. Use Records, text blocks, and `var`.
    *   **Frontend**: Strict Standard JS style (no semicolons) via `@antfu/eslint-config`.
2.  **Verification**:
    *   **Always** run `mvn test` after backend changes.
    *   **Always** run `pnpm typecheck` and `pnpm lint` after frontend changes.
3.  **Refactoring**:
    *   Do not refactor working code unless explicitly requested.
    *   Preserve existing folder structures (e.g., `pages/` for routing).

## 🛠 Commands

### Backend (Spring Boot 3.5.10 + Java 21)
| Action | Command | Note |
|--------|---------|------|
| **Build** | `mvn clean compile` | Basic compilation |
| **Run** | `mvn spring-boot:run` | Starts on port 8123 |
| **Test All** | `mvn test` | **Mandatory** before commit |
| **Test One** | `mvn test -Dtest=ClassName` | Class level |
| **Test Method** | `mvn test -Dtest=Class#method` | Method level |
| **Package** | `mvn clean package -DskipTests` | For deployment |

### Frontend (Vue 3 + Vite)
Workdir: `cd frontend`
| Action | Command | Note |
|--------|---------|------|
| **Install** | `pnpm install` | Requires pnpm 10.14+ |
| **Dev** | `pnpm dev` | Starts on localhost:5173 |
| **Lint** | `pnpm lint` | **Mandatory**. Fixes formatting |
| **Typecheck** | `pnpm typecheck` | **Mandatory**. Vue-TSC |
| **API Gen** | `pnpm openapi` | Syncs with backend |
| **Test** | *(None configured)* | |

## 📂 Project Structure

```text
src/main/java/com/zhemu/paperinsight/
├── agent/          # AgentScope agents (core/, tools/, config/)
├── controller/     # REST endpoints (@RestController)
├── service/impl/   # Business logic (@Service)
├── mapper/         # MyBatis-Plus interfaces
├── model/entity/   # DB Entities (@TableName)
├── model/dto/      # Request Data Transfer Objects
├── model/vo/       # Response View Objects
└── mq/             # RabbitMQ consumers

frontend/src/
├── api/            # Generated API clients (do not edit manually)
├── components/     # Shared Vue components
├── pages/          # File-based routing (unplugin-vue-router)
├── stores/         # Pinia state stores
└── types/          # Global TypeScript definitions
```

## ☕ Backend Code Style (Java 21)

### 1. Modern Java Features
Use **Records** for immutable DTOs, **Text Blocks** for SQL/JSON, and **Switch Expressions**.
```java
// Record
public record UserInfo(String name, String email) {}

// Switch Expression
String type = switch (input) {
    case "A" -> "Type A";
    default -> "Unknown";
};
```

### 2. Controller & Error Handling
- Return `BaseResponse<T>`.
- Use `ThrowUtils` for validations.
- **Never** catch generic `Exception` without re-throwing or logging.

```java
@RestController
@RequestMapping("/paper")
@RequiredArgsConstructor
public class PaperController {
    private final PaperService paperService;

    @PostMapping("/add")
    public BaseResponse<Long> add(@RequestBody @Validated PaperAddRequest req) {
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(paperService.add(req));
    }
}
```

### 3. MyBatis-Plus Entities
Use Lombok `@Data`, `@Builder`. Use Logical Deletion.
```java
@TableName("paper_info")
@Data @Builder
public class PaperInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableLogic
    private Integer isDelete; // 0=valid, 1=deleted
}
```

## 🎨 Frontend Code Style (Vue 3 + TS)

### 1. Component Setup
Use `<script setup lang="ts">`. Use `~/` alias for imports.
```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listPaper } from '~/api/paper' // Use alias

const list = ref([])
const fetch = async () => {
  const res = await listPaper({})
  if (res.code === 0) list.value = res.data
}
</script>

<template>
  <!-- Use UnoCSS utility classes -->
  <div class="p-4 flex flex-col gap-2">
    <div v-for="item in list" :key="item.id">{{ item.title }}</div>
  </div>
</template>
```

### 2. State Management (Pinia)
Use Setup Stores pattern with persistence.
```typescript
export const useUserStore = defineStore('user', () => {
  const user = ref(null)
  return { user }
}, { persist: true })
```

### 3. Linting Rules (@antfu)
*   **No Semicolons**
*   **Single Quotes**
*   **Trailing Commas**
*   Run `pnpm lint` to auto-fix.

## 📦 Key Dependencies
*   **Java**: Spring Boot 3.5.10, MyBatis-Plus 3.5.10, AgentScope 1.0.8, Hutool 5.8.37
*   **Node**: Vue 3.5, Vite 7, Element Plus 2.10, UnoCSS, Pinia
