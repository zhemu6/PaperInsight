<script setup lang="ts">
import { use } from 'echarts/core'
import { RadarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import { ref, watch, computed } from 'vue'
import { useI18n } from 'vue-i18n'

use([
  CanvasRenderer,
  RadarChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
])

const { t } = useI18n()

const props = defineProps<{
  data: Record<string, any>
  score: number
}>()

const option = computed(() => {
  // 维度中文映射
  const labelMap: Record<string, string> = {
    writing: t('paperDetail.radar.dimensions.writing'),
    technical: t('paperDetail.radar.dimensions.technical'),
    innovation: t('paperDetail.radar.dimensions.innovation'),
    practicality: t('paperDetail.radar.dimensions.practicality')
  }

  // 维度满分映射
  const maxMap: Record<string, number> = {
    writing: 20,
    technical: 30,
    innovation: 30,
    practicality: 20
  }

  // 默认维度 (如果 data 为空)
  const defaultDimensions = [
    { name: t('paperDetail.radar.dimensions.innovation'), max: 30 },
    { name: t('paperDetail.radar.dimensions.technical'), max: 30 },
    { name: t('paperDetail.radar.dimensions.practicality'), max: 20 },
    { name: t('paperDetail.radar.dimensions.writing'), max: 20 },
  ]
  
  // 尝试从 props.data 解析维度
  const indicator = Object.keys(props.data || {}).length > 0 
    ? Object.keys(props.data).map(key => ({ 
        name: labelMap[key] || key, 
        max: maxMap[key] || 100 
      }))
    : defaultDimensions

  const values = Object.keys(props.data || {}).length > 0
    ? Object.values(props.data)
    : [0, 0, 0, 0, 0]

  return {
    backgroundColor: 'transparent',
    title: {
      text: t('paperDetail.radar.title', { score: props.score || '-' }),
      left: 'center',
      top: '0',
      textStyle: {
        color: '#333',
        fontSize: 16
      }
    },
    tooltip: {},
    radar: {
      indicator: indicator,
      radius: '65%',
      center: ['50%', '55%'],
      splitNumber: 5,
      axisName: {
        color: '#666'
      },
      splitLine: {
        lineStyle: {
          color: [
            'rgba(238, 197, 102, 0.1)',
            'rgba(238, 197, 102, 0.2)',
            'rgba(238, 197, 102, 0.4)',
            'rgba(238, 197, 102, 0.6)',
            'rgba(238, 197, 102, 0.8)',
            'rgba(238, 197, 102, 1)'
          ].reverse()
        }
      },
      splitArea: {
        show: false
      },
      axisLine: {
        lineStyle: {
          color: 'rgba(238, 197, 102, 0.5)'
        }
      }
    },
    series: [
      {
        name: t('paperDetail.radar.seriesName'),
        type: 'radar',
        data: [
          {
            value: values,
            name: 'Paper Score',
            symbol: 'none',
            itemStyle: {
              color: '#F97316'
            },
            areaStyle: {
              opacity: 0.2
            }
          }
        ]
      }
    ]
  }
})
</script>

<template>
  <div class="h-60 w-full flex justify-center items-center">
    <VChart class="chart" :option="option" autoresize />
  </div>
</template>

<style scoped>
.chart {
  height: 100%;
  width: 100%;
}
</style>
