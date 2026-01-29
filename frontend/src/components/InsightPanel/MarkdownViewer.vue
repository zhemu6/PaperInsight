<script setup lang="ts">
import MarkdownIt from 'markdown-it'
import mermaid from 'mermaid'
import { computed, nextTick, onMounted, onUpdated } from 'vue'
import 'github-markdown-css/github-markdown.css'

const props = defineProps<{
  content: string
}>()

const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
})

// Configure mermaid
mermaid.initialize({
  startOnLoad: false,
  theme: 'default',
  // You can detect dark mode here if needed
  securityLevel: 'loose',
})

const renderedContent = computed(() => {
  if (!props.content)
    return '<p class="text-gray-400 italic">暂无内容</p>'

  // 1. 处理可能存在的转义换行符
  let raw = props.content.replace(/\\n/g, '\n')

  // 2. 检测并移除可能包裹的 markdown 代码块标记
  const codeBlockRegex = /```(?:markdown)?\s+([\s\S]*?)\s+```/i
  const match = raw.match(codeBlockRegex)

  if (match && match[1]) {
    raw = match[1]
  }

  // 3. 将 mermaid 代码块转换为 div，以便 mermaid.js 处理
  // Markdown-it 默认会将 ```mermaid 渲染为 <pre><code class="language-mermaid">
  // 我们需要拦截它，或者在渲染后处理。这里选择自定义渲染规则比较稳妥，但也比较复杂。
  // 简单方案：先用正则把 ```mermaid ... ``` 替换成 <div class="mermaid">...</div>
  // 注意：这只是一个轻量级的替换，针对简单的块有效
  raw = raw.replace(/```mermaid\s+([\s\S]*?)\s+```/gi, (match, code) => {
    return `<div class="mermaid">${code}</div>`
  })

  return md.render(raw)
})

async function renderMermaid() {
  await nextTick()
  try {
    await mermaid.run({
      querySelector: '.mermaid',
    })
  }
  catch (e) {
    console.error('Mermaid render error:', e)
  }
}

onMounted(() => {
  renderMermaid()
})

onUpdated(() => {
  renderMermaid()
})
</script>

<template>
  <div class="markdown-body custom-markdown" v-html="renderedContent" />
</template>

<style scoped>
.markdown-body {
  box-sizing: border-box;
  min-width: 200px;
  max-width: 100%;
  margin: 0;
  padding: 10px;
  background-color: transparent !important;
  font-family: inherit;
  font-size: 15px;
  line-height: 1.6;
  color: var(--el-text-color-primary); /* Adapt to theme */
}

/* 覆盖 github-markdown-css 的默认样式以适应 Element Plus 主题 */
:deep(.markdown-body p) {
  margin-bottom: 12px;
}
:deep(.markdown-body h1),
:deep(.markdown-body h2),
:deep(.markdown-body h3) {
  border-bottom: none;
  font-weight: 600;
  margin-top: 1.5em;
  margin-bottom: 1em;
  color: var(--el-text-color-primary);
}

/* Dark mode specific overrides if github-markdown-css fails to detect class-based dark mode */
:deep(.markdown-body pre) {
  background-color: var(--el-fill-color-light);
}

/* 修复列表在 ChatPanel 中被裁切的问题 */
:deep(.markdown-body ul),
:deep(.markdown-body ol) {
  padding-left: 1.5em; /* 确保有足够空间显示标记 */
  list-style-position: outside;
  margin-bottom: 1em;
}

:deep(.markdown-body li) {
  margin-top: 0.25em;
}
</style>
