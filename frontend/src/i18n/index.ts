/**
 * i18n 国际化配置
 * 支持中英文切换，自动识别浏览器语言
 */
import { createI18n } from 'vue-i18n'
import en from './en'
import zh from './zh'

const STORAGE_KEY = 'paper-insight-locale'

/**
 * 获取默认语言
 * 优先级：localStorage → 浏览器语言 → 中文
 */
function getDefaultLocale(): string {
  // 1. 先检查 localStorage
  const savedLocale = localStorage.getItem(STORAGE_KEY)
  if (savedLocale && ['zh', 'en'].includes(savedLocale)) {
    return savedLocale
  }

  // 2. 再检查浏览器语言
  const browserLang = navigator.language.toLowerCase()
  if (browserLang.startsWith('zh')) {
    return 'zh'
  }
  if (browserLang.startsWith('en')) {
    return 'en'
  }

  // 3. 默认中文
  return 'zh'
}

const messages = {
  zh,
  en,
}

export const i18n = createI18n({
  legacy: false, // 使用 Composition API 模式
  locale: getDefaultLocale(),
  fallbackLocale: 'en',
  messages,
})

/**
 * 设置当前语言
 */
export function setLocale(locale: string) {
  if (['zh', 'en'].includes(locale)) {
    i18n.global.locale.value = locale as 'zh' | 'en'
    localStorage.setItem(STORAGE_KEY, locale)
    // 更新 HTML lang 属性（无障碍访问）
    document.documentElement.lang = locale === 'zh' ? 'zh-CN' : 'en'
  }
}

/**
 * 获取当前语言
 */
export function getLocale(): string {
  return i18n.global.locale.value
}

/**
 * 切换中英文
 */
export function toggleLocale(): string {
  const currentLocale = getLocale()
  const newLocale = currentLocale === 'zh' ? 'en' : 'zh'
  setLocale(newLocale)
  return newLocale
}
