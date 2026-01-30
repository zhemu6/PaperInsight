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
  // Chat-style: treat single newlines as line breaks
  breaks: true,
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

  // 1. 处理可能存在的转义换行符/引号（有些模型会输出字面量 "\\n"、"\\\""）
  let raw = props.content
  console.log(raw)
  // double-escaped newline/quote -> single-escaped
  raw = raw.replace(/\\\\n/g, '\\n')
  raw = raw.replace(/\\\\"/g, '\\"')

  // single-escaped -> real chars
  raw = raw.replace(/\\n/g, '\n')
  raw = raw.replace(/\\"/g, '"')

  // remaining escaped backslashes
  raw = raw.replace(/\\\\/g, '\\')
  console.log("~~~~~~~~~~~~~~~~2~~~~~~~~~~~~~~~~~~")
  console.log(raw)
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
  console.log("~~~~~~~~~~~~~~~~3~~~~~~~~~~~~~~~~~~")
  console.log(raw)
  console.log("~~~~~~~~~~~~~~~~md.render(raw)~~~~~~~~~~~~~~~~~~")
  console.log(md.render(raw))
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
/* 容器本身 */
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
  color: var(--el-text-color-primary);
  overflow: visible; /* 防止 marker 被裁切 */
}

/* 段落间距 */
:deep(p) {
  margin: 0 0 12px;
}

/* 标题 */
:deep(h1),
:deep(h2),
:deep(h3) {
  border-bottom: none;
  font-weight: 600;
  margin-top: 1.5em;
  margin-bottom: 1em;
  color: var(--el-text-color-primary);
}

/* 代码块 */
:deep(pre) {
  background-color: var(--el-fill-color-light);
  padding: 12px;
  border-radius: 6px;
  overflow: auto;
}

/* ✅ 列表：极限兜底（覆盖各种 reset / UI 框架） */
:deep(ol),
:deep(ul) {
  /* 把框架常见的 list-style:none 拉回来 */
  list-style: initial !important;
  list-style-position: outside !important;

  /* 有些框架会把 padding/margin 清零 */
  padding-left: 1.5em !important;
  margin: 0 0 1em !important;

  /* 有些框架会把 ol/ul 设成 flex 导致 marker 异常 */
  display: block !important;
}

/* 分别指定类型，避免 initial 变成 none 的情况 */
:deep(ol) {
  list-style-type: decimal !important;
}
:deep(ul) {
  list-style-type: disc !important;
}

/* li 可能被框架改成 block / flex，导致 marker 不出现 */
:deep(li) {
  display: list-item !important;
  margin-top: 0.25em;
}

/* 兜底：强制 marker 颜色可见（有些主题会把 marker 变透明/淡） */
:deep(li::marker) {
  color: var(--el-text-color-primary) !important;
  opacity: 1 !important;
}

/* 防止某些样式把 marker 内容清空 */
:deep(ol > li::marker),
:deep(ul > li::marker) {
  content: normal !important;
}

/* 加粗 */
:deep(strong) {
  font-weight: 600;
}
</style>
