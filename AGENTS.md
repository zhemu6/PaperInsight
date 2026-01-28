# AGENTS.md - PaperInsight Development Guide

> AI-driven academic paper management with AgentScope multi-agent framework.

## Commands

### Backend (Spring Boot 3.5.10 + Java 21)
```bash
mvn clean compile                              # Build
mvn spring-boot:run                            # Run (dev profile)
mvn test                                       # Run all tests
mvn test -Dtest=PaperInsightApplicationTests   # Single test class
mvn test -Dtest=ClassName#methodName           # Single test method
mvn clean package -DskipTests                  # Package
```

### Frontend (Vue 3 + Vite)
```bash
cd frontend
pnpm install          # Install (pnpm 10.14.0)
pnpm dev              # Dev server (localhost:5173)
pnpm typecheck        # Type check
pnpm lint             # Lint
pnpm build            # Build
pnpm openapi          # Generate API types from OpenAPI
```

## Project Structure
```
src/main/java/com/zhemu/paperinsight/
├── controller/     # REST endpoints (@RestController)
├── service/impl/   # Business logic
├── mapper/         # MyBatis-Plus data access
├── model/entity/   # DB entities | dto/ Requests | vo/ Responses
├── agent/          # AgentScope AI agents
├── exception/      # Error handling (ErrorCode, ThrowUtils)
└── mq/             # RabbitMQ consumers/producers

frontend/src/
├── api/            # Auto-generated API clients
├── pages/          # File-based routing pages
├── stores/         # Pinia state stores
└── components/     # Vue components
```

## Backend Code Style

### Java 21 Features (USE THEM)
```java
// Records for DTOs
public record UserInfo(String name, String email) {}

// Pattern matching switch
return switch (value) {
    case String s -> "String: " + s;
    case null -> "null";
    default -> value.toString();
};

// Text blocks
String sql = """
    SELECT * FROM users WHERE status = 'ACTIVE'
    """;
```

### Naming Conventions
| Element | Convention | Example |
|---------|------------|---------|
| Class | PascalCase | `PaperController` |
| Method/Field | camelCase | `findUserById` |
| Constant | UPPER_SNAKE | `MAX_PAGE_SIZE` |
| Request DTO | XxxRequest | `PaperAddRequest` |
| Response VO | XxxVO | `PaperVO` |

### Controller Pattern
```java
@RestController
@RequestMapping("/paper")
@RequiredArgsConstructor
public class PaperController {
    private final PaperInfoService paperInfoService;

    @PostMapping("/add")
    @AuthCheck
    public BaseResponse<Long> addPaper(@RequestBody @Validated PaperAddRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(paperInfoService.addPaper(request));
    }
}
```

### Error Handling
```java
// Use ErrorCode enum
ThrowUtils.throwIf(condition, ErrorCode.PARAMS_ERROR);
ThrowUtils.throwIf(condition, ErrorCode.NOT_FOUND_ERROR, "Custom message");
throw new BusinessException(ErrorCode.OPERATION_ERROR, "详细信息");

// ErrorCode values: SUCCESS(0), PARAMS_ERROR(40000), NOT_LOGIN_ERROR(40100),
// NO_AUTH_ERROR(40101), NOT_FOUND_ERROR(40400), SYSTEM_ERROR(50000)
```

### Response Format
```java
// All responses use BaseResponse<T> with code/data/message
return ResultUtils.success(data);      // code=0
return ResultUtils.error(ErrorCode.X); // code=error code
```

### Entity Pattern (MyBatis-Plus)
```java
@TableName("paper_info")
@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class PaperInfo implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    @TableLogic
    private Integer isDelete;  // Logical delete
}
```

## Frontend Code Style

### Vue Component Structure
```vue
<script setup lang="ts">
// 1. Vue core
import { onMounted, ref } from 'vue'
// 2. Vue ecosystem
import { useRouter } from 'vue-router'
// 3. UI library
import { ElMessage } from 'element-plus'
// 4. Local (use ~/ alias)
import { listPaperByPage } from '~/api/paperController'

const loading = ref(false)
const papers = ref<any[]>([])

const fetchPapers = async () => {
  try {
    loading.value = true
    const res = await listPaperByPage({}) as any
    if (res.code === 0) papers.value = res.data.records
  } finally {
    loading.value = false
  }
}
onMounted(fetchPapers)
</script>

<template>
  <div class="h-full flex flex-col p-6"><!-- UnoCSS --></div>
</template>
```

### Pinia Store Pattern
```typescript
export const useUserStore = defineStore('user', () => {
  const loginUser = ref<API.SysUserVO>({ userName: '' })
  async function fetchLoginUser() { /* ... */ }
  return { loginUser, fetchLoginUser }
}, { persist: true })
```

### TypeScript
- Strict mode enabled
- Use `~/` alias for src imports
- API types auto-generated in `src/api/typings.d.ts`
- ESLint: @antfu/eslint-config with Vue + UnoCSS

## Key Dependencies

**Backend**: Spring Boot 3.5.10, Java 21, MyBatis-Plus 3.5.10, AgentScope 1.0.8, Hutool 5.8.37, Knife4j 4.4.0

**Frontend**: Vue 3.5.18, Vite 7.x, Element Plus 2.10.5, UnoCSS 66.x, Pinia 3.x, TypeScript 5.x

## Development Notes
- API Docs: `http://localhost:8123/api/doc.html` (Knife4j)
- API prefix: `/api`
- MySQL 8.0 with logical delete (`is_delete`), underscore to camelCase mapping
- Frontend file-based routing via `unplugin-vue-router`
