<script setup lang="ts">
import { ArrowRight, DataAnalysis, Document, Monitor, Search } from '@element-plus/icons-vue'
import { useDark, useToggle } from '@vueuse/core'
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import logo from '~/assets/logo.svg'
import { getLocale, toggleLocale } from '~/i18n'

const { t } = useI18n()
const router = useRouter()

const isDark = useDark()
const toggleDark = useToggle(isDark)
const currentLocale = computed(() => getLocale())

const mobileMenuOpen = ref(false)

function goLogin() {
  router.push({
    path: '/common/login',
    query: { redirect: '/file' },
  })
}

function goRegister() {
  router.push({
    path: '/common/register',
    query: { redirect: '/file' },
  })
}

const features = computed(() => [
  {
    icon: DataAnalysis,
    title: t('landing.features.analysis.title'),
    desc: t('landing.features.analysis.desc'),
    accent: 'text-emerald-600 dark:text-emerald-400',
    bg: 'bg-emerald-500/10',
  },
  {
    icon: Search,
    title: t('landing.features.search.title'),
    desc: t('landing.features.search.desc'),
    accent: 'text-blue-600 dark:text-blue-400',
    bg: 'bg-blue-500/10',
  },
  {
    icon: Document,
    title: t('landing.features.reader.title'),
    desc: t('landing.features.reader.desc'),
    accent: 'text-sky-600 dark:text-sky-400',
    bg: 'bg-sky-500/10',
  },
  {
    icon: Monitor,
    title: t('landing.features.workspace.title'),
    desc: t('landing.features.workspace.desc'),
    accent: 'text-amber-600 dark:text-amber-400',
    bg: 'bg-amber-500/10',
  },
])

const steps = computed(() => [
  {
    num: '01',
    title: t('landing.steps.upload.title'),
    desc: t('landing.steps.upload.desc'),
  },
  {
    num: '02',
    title: t('landing.steps.analyze.title'),
    desc: t('landing.steps.analyze.desc'),
  },
  {
    num: '03',
    title: t('landing.steps.chat.title'),
    desc: t('landing.steps.chat.desc'),
  },
])

function scrollToId(id: string) {
  const el = document.getElementById(id)
  el?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  mobileMenuOpen.value = false
}
</script>

<template>
  <div class="min-h-screen w-full bg-[#fbfbfa] text-stone-900 dark:bg-[#07090c] dark:text-stone-100">
    <!-- Background atmosphere -->
    <div class="pointer-events-none fixed inset-0 overflow-hidden">
      <div class="absolute -top-40 -right-40 h-[520px] w-[520px] rounded-full bg-sky-500/12 blur-[90px] dark:bg-sky-400/10" />
      <div class="absolute -bottom-44 -left-44 h-[560px] w-[560px] rounded-full bg-emerald-500/12 blur-[90px] dark:bg-emerald-400/10" />
      <div class="absolute inset-0 opacity-[0.45] [background-image:radial-gradient(rgba(120,120,120,0.08)_1px,transparent_1px)] [background-size:18px_18px] dark:opacity-[0.25]" />
    </div>

    <!-- Navbar -->
    <header class="sticky top-0 z-50 border-b border-stone-200/60 bg-[#fbfbfa]/75 backdrop-blur-md dark:border-white/10 dark:bg-[#07090c]/70">
      <div class="mx-auto max-w-7xl flex items-center justify-between px-5 py-4">
        <div class="flex items-center gap-3">
          <img :src="logo" alt="PaperInsight" class="h-10 w-10">
          <span class="text-xl font-bold tracking-tight font-serif">PaperInsight</span>
          <span class="hidden border border-stone-200 rounded-full bg-white px-2 py-0.5 text-[11px] text-stone-600 font-medium sm:inline-block dark:border-white/10 dark:bg-white/5 dark:text-stone-300">AI Paper Reading</span>
        </div>

        <nav class="hidden items-center gap-8 md:flex">
          <button
            class="bg-transparent border-none p-0 text-sm text-stone-700 font-medium hover:text-stone-900 hover:underline underline-offset-6 dark:text-stone-300 dark:hover:text-white"
            @click="scrollToId('features')"
          >
            {{ t('landing.nav.features') }}
          </button>
          <button
            class="bg-transparent border-none p-0 text-sm text-stone-700 font-medium hover:text-stone-900 hover:underline underline-offset-6 dark:text-stone-300 dark:hover:text-white"
            @click="scrollToId('how')"
          >
            {{ t('landing.nav.how') }}
          </button>
          <button
            class="bg-transparent border-none p-0 text-sm text-stone-700 font-medium hover:text-stone-900 hover:underline underline-offset-6 dark:text-stone-300 dark:hover:text-white"
            @click="scrollToId('cta')"
          >
            {{ t('landing.nav.start') }}
          </button>
        </nav>

        <div class="hidden items-center gap-3 md:flex">
          <button
            class="h-9 w-9 flex items-center justify-center rounded-full bg-transparent transition hover:bg-stone-200/60 dark:hover:bg-white/10"
            :title="t('common.language')"
            @click="toggleLocale()"
          >
            <span class="text-xs font-semibold">{{ currentLocale === 'zh' ? '中' : 'EN' }}</span>
          </button>
          <button
            class="h-9 w-9 flex items-center justify-center rounded-full bg-transparent transition hover:bg-stone-200/60 dark:hover:bg-white/10"
            :title="isDark ? 'Light' : 'Dark'"
            @click="toggleDark()"
          >
            <i class="i-ep-sunny dark:i-ep-moon text-lg" />
          </button>

          <el-button text @click="goLogin">
            {{ t('nav.login') }}
          </el-button>
          <el-button type="primary" round @click="goRegister">
            {{ t('nav.register') }}
            <el-icon class="ml-1">
              <ArrowRight />
            </el-icon>
          </el-button>
        </div>

        <button class="md:hidden" @click="mobileMenuOpen = !mobileMenuOpen">
          <div class="i-carbon-menu text-xl" />
        </button>
      </div>

      <div v-show="mobileMenuOpen" class="border-t border-stone-200/60 bg-[#fbfbfa]/85 px-5 py-4 md:hidden dark:border-white/10 dark:bg-[#07090c]/75">
        <div class="flex flex-col gap-3">
          <button class="text-left text-sm text-stone-800 font-medium dark:text-stone-200" @click="scrollToId('features')">
            {{ t('landing.nav.features') }}
          </button>
          <button class="text-left text-sm text-stone-800 font-medium dark:text-stone-200" @click="scrollToId('how')">
            {{ t('landing.nav.how') }}
          </button>
          <button class="text-left text-sm text-stone-800 font-medium dark:text-stone-200" @click="scrollToId('cta')">
            {{ t('landing.nav.start') }}
          </button>

          <div class="mt-2 flex items-center gap-3">
            <el-button class="w-full" @click="goLogin">
              {{ t('nav.login') }}
            </el-button>
            <el-button type="primary" class="w-full" @click="goRegister">
              {{ t('nav.register') }}
            </el-button>
          </div>
        </div>
      </div>
    </header>

    <!-- Hero -->
    <main class="relative">
      <section class="mx-auto max-w-7xl px-5 pt-16 sm:pt-20 lg:pt-24">
        <div class="grid gap-10 lg:grid-cols-12 lg:items-center">
          <div class="lg:col-span-6">
            <div class="inline-flex items-center gap-2 border border-stone-200 rounded-full bg-white px-3 py-1 text-xs text-stone-700 font-medium dark:border-white/10 dark:bg-white/5 dark:text-stone-200">
              <span class="h-2 w-2 rounded-full bg-emerald-500" />
              {{ t('landing.badge') }}
            </div>

            <h1 class="mt-6 text-4xl font-bold leading-tight tracking-tight font-serif sm:text-5xl lg:text-6xl">
              {{ t('landing.hero.title1') }}
              <span class="block bg-gradient-to-r from-sky-700 to-emerald-600 bg-clip-text text-transparent dark:from-sky-300 dark:to-emerald-300">
                {{ t('landing.hero.title2') }}
              </span>
            </h1>
            <p class="mt-5 max-w-xl text-base text-stone-600 leading-relaxed sm:text-lg dark:text-stone-300">
              {{ t('landing.hero.subtitle') }}
            </p>

            <div class="mt-8 flex flex-col gap-3 sm:flex-row">
              <el-button type="primary" size="large" class="!h-12 !px-7" @click="goRegister">
                {{ t('landing.hero.ctaPrimary') }}
                <el-icon class="ml-2">
                  <ArrowRight />
                </el-icon>
              </el-button>
              <el-button size="large" class="!h-12 !px-7" plain @click="goLogin">
                {{ t('landing.hero.ctaSecondary') }}
              </el-button>
            </div>

            <div class="mt-8 flex flex-wrap items-center gap-x-6 gap-y-2 text-xs text-stone-500 dark:text-stone-400">
              <span class="inline-flex items-center gap-2">
                <i class="i-carbon-checkmark-filled text-emerald-600 dark:text-emerald-400" />
                {{ t('landing.hero.point1') }}
              </span>
              <span class="inline-flex items-center gap-2">
                <i class="i-carbon-checkmark-filled text-emerald-600 dark:text-emerald-400" />
                {{ t('landing.hero.point2') }}
              </span>
              <span class="inline-flex items-center gap-2">
                <i class="i-carbon-checkmark-filled text-emerald-600 dark:text-emerald-400" />
                {{ t('landing.hero.point3') }}
              </span>
            </div>
          </div>

          <div class="lg:col-span-6">
            <div class="rounded-3xl border border-stone-200 bg-white/70 p-2 shadow-[0_30px_80px_-30px_rgba(15,23,42,0.25)] backdrop-blur dark:border-white/10 dark:bg-white/5">
              <div class="overflow-hidden rounded-2xl bg-[#0b0f14]">
                <div class="flex items-center gap-2 border-b border-white/10 px-4 py-3">
                  <div class="flex gap-1.5">
                    <div class="h-3 w-3 rounded-full bg-red-500/90" />
                    <div class="h-3 w-3 rounded-full bg-yellow-500/90" />
                    <div class="h-3 w-3 rounded-full bg-green-500/90" />
                  </div>
                  <div class="mx-auto hidden h-6 w-[55%] items-center justify-center rounded-md bg-white/5 text-[11px] text-white/50 font-mono sm:flex">
                    paperinsight / analysis
                  </div>
                </div>
                <div class="grid gap-4 p-6 sm:p-8">
                  <div class="grid grid-cols-12 gap-4">
                    <div class="col-span-12 rounded-xl bg-white/5 p-4 sm:col-span-7">
                      <div class="text-xs text-white/55">{{ t('landing.preview.summary') }}</div>
                      <div class="mt-2 space-y-2">
                        <div class="h-2 w-11/12 rounded bg-white/10" />
                        <div class="h-2 w-10/12 rounded bg-white/10" />
                        <div class="h-2 w-9/12 rounded bg-white/10" />
                      </div>
                    </div>
                    <div class="col-span-12 rounded-xl bg-white/5 p-4 sm:col-span-5">
                      <div class="text-xs text-white/55">{{ t('landing.preview.score') }}</div>
                      <div class="mt-3 flex items-end gap-2">
                        <div class="text-3xl text-white font-semibold">92</div>
                        <div class="pb-1 text-xs text-emerald-300">{{ t('landing.preview.scoreTag') }}</div>
                      </div>
                      <div class="mt-3 h-2 w-full rounded bg-white/10" />
                      <div class="mt-2 h-2 w-10/12 rounded bg-white/10" />
                    </div>
                  </div>
                  <div class="grid grid-cols-12 gap-4">
                    <div class="col-span-12 rounded-xl bg-white/5 p-4 sm:col-span-6">
                      <div class="text-xs text-white/55">{{ t('landing.preview.chat') }}</div>
                      <div class="mt-3 space-y-2">
                        <div class="h-8 w-11/12 rounded-lg bg-white/10" />
                        <div class="h-8 w-9/12 rounded-lg bg-white/10" />
                      </div>
                    </div>
                    <div class="col-span-12 rounded-xl bg-white/5 p-4 sm:col-span-6">
                      <div class="text-xs text-white/55">{{ t('landing.preview.notifications') }}</div>
                      <div class="mt-3 space-y-2">
                        <div class="h-10 w-full rounded-lg bg-white/10" />
                        <div class="h-10 w-full rounded-lg bg-white/10" />
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Features -->
      <section id="features" class="mx-auto max-w-7xl px-5 py-18 sm:py-22">
        <div class="mb-10">
          <h2 class="text-2xl font-bold tracking-tight font-serif sm:text-3xl">
            {{ t('landing.section.featuresTitle') }}
          </h2>
          <p class="mt-3 max-w-2xl text-sm text-stone-600 leading-relaxed sm:text-base dark:text-stone-300">
            {{ t('landing.section.featuresSubtitle') }}
          </p>
        </div>

        <div class="grid gap-5 md:grid-cols-2 xl:grid-cols-4">
          <div
            v-for="(f, idx) in features"
            :key="idx"
            class="group rounded-2xl border border-stone-200 bg-white/60 p-6 transition hover:-translate-y-0.5 hover:bg-white/80 dark:border-white/10 dark:bg-white/5 dark:hover:bg-white/7"
          >
            <div class="mb-5 inline-flex h-11 w-11 items-center justify-center rounded-xl" :class="f.bg">
              <el-icon :size="22" :class="f.accent">
                <component :is="f.icon" />
              </el-icon>
            </div>
            <div class="text-base font-semibold">
              {{ f.title }}
            </div>
            <div class="mt-2 text-sm text-stone-600 leading-relaxed dark:text-stone-300">
              {{ f.desc }}
            </div>
          </div>
        </div>
      </section>

      <!-- How it works -->
      <section id="how" class="border-y border-stone-200/70 bg-white/20 py-18 dark:border-white/10 dark:bg-white/3">
        <div class="mx-auto max-w-7xl px-5">
          <div class="grid gap-10 lg:grid-cols-12 lg:items-start">
            <div class="lg:col-span-5">
              <h2 class="text-2xl font-bold tracking-tight font-serif sm:text-3xl">
                {{ t('landing.section.howTitle') }}
              </h2>
              <p class="mt-3 text-sm text-stone-600 leading-relaxed sm:text-base dark:text-stone-300">
                {{ t('landing.section.howSubtitle') }}
              </p>
            </div>
            <div class="lg:col-span-7">
              <div class="grid gap-4">
                <div
                  v-for="(s, idx) in steps"
                  :key="idx"
                  class="rounded-2xl border border-stone-200 bg-white/60 p-6 dark:border-white/10 dark:bg-white/5"
                >
                  <div class="flex items-start gap-5">
                    <div class="h-10 w-10 flex items-center justify-center rounded-full bg-stone-900 text-white font-mono text-sm dark:bg-white dark:text-black">
                      {{ s.num }}
                    </div>
                    <div class="min-w-0">
                      <div class="text-base font-semibold">
                        {{ s.title }}
                      </div>
                      <div class="mt-2 text-sm text-stone-600 leading-relaxed dark:text-stone-300">
                        {{ s.desc }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- CTA -->
      <section id="cta" class="mx-auto max-w-7xl px-5 py-20">
        <div class="rounded-3xl border border-stone-200 bg-white/60 p-8 dark:border-white/10 dark:bg-white/5 sm:p-12">
          <div class="grid gap-8 lg:grid-cols-12 lg:items-center">
            <div class="lg:col-span-7">
              <h3 class="text-2xl font-bold tracking-tight font-serif sm:text-3xl">
                {{ t('landing.cta.title') }}
              </h3>
              <p class="mt-3 max-w-2xl text-sm text-stone-600 leading-relaxed sm:text-base dark:text-stone-300">
                {{ t('landing.cta.subtitle') }}
              </p>
            </div>
            <div class="lg:col-span-5 lg:justify-self-end">
              <div class="flex flex-col gap-3 sm:flex-row lg:flex-col">
                <el-button type="primary" size="large" class="!h-12 !px-8" @click="goRegister">
                  {{ t('landing.cta.primary') }}
                </el-button>
                <el-button size="large" class="!h-12 !px-8" plain @click="goLogin">
                  {{ t('landing.cta.secondary') }}
                </el-button>
              </div>
            </div>
          </div>

          <div class="mt-10 flex flex-col gap-3 border-t border-stone-200/70 pt-6 text-xs text-stone-500 sm:flex-row sm:items-center sm:justify-between dark:border-white/10 dark:text-stone-400">
            <div>© {{ new Date().getFullYear() }} PaperInsight</div>
            <div class="flex items-center gap-5">
              <span class="cursor-default">{{ t('landing.footer.privacy') }}</span>
              <span class="cursor-default">{{ t('landing.footer.terms') }}</span>
              <span class="cursor-default">{{ t('landing.footer.contact') }}</span>
            </div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<route lang="yaml">
meta:
  layout: blank
</route>
