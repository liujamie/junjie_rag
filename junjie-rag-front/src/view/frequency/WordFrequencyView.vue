<template>
  <div class="page-container">
    <!-- Gradient Header -->
    <div class="page-header">
      <div class="page-header-content">
        <div>
          <h1 class="page-title">词频统计</h1>
          <p class="page-desc">查看对话中的热词分布和词云展示</p>
        </div>
        <div class="page-header-actions">
          <el-button type="primary" @click="loadWordFrequencyData">刷新数据</el-button>
          <el-button type="danger" @click="handleClean">清空数据</el-button>
        </div>
      </div>
    </div>

    <!-- Content -->
    <div class="page-body">
      <div class="charts-grid">
        <div class="chart-card">
          <div class="chart-header">
            <h3>词云展示</h3>
          </div>
          <div ref="wordCloudRef" v-loading="isLoading" class="chart-box"></div>
        </div>

        <div class="chart-card">
          <div class="chart-header">
            <h3>TOP10 热词统计</h3>
          </div>
          <div ref="barChartRef" v-loading="isLoading" class="chart-box"></div>
        </div>

        <div class="chart-card">
          <div class="chart-header">
            <h3>词频分布</h3>
          </div>
          <div ref="pieChartRef" v-loading="isLoading" class="chart-box"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { frequencyPageApi, getList, cleanApi } from '@/api/FrequencyApi'
import { WordFreq } from "@/api/data.ts"
import * as echarts from 'echarts'
import 'echarts-wordcloud'
import type { EChartsOption } from 'echarts'

const wordCloudRef = ref<HTMLElement>()
const barChartRef = ref<HTMLElement>()
const pieChartRef = ref<HTMLElement>()
const isLoading = ref(false)
let wordCloudChart: echarts.ECharts | null = null
let barChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

const loadWordFrequencyData = async () => {
  isLoading.value = true
  try {
    const res = await getList()
    if (res.code === 0) {
      const list: WordFreq[] = res.data ?? []
      updateCharts(list)
    }
  } catch (e) {
    console.error('获取词频数据失败', e)
  } finally {
    isLoading.value = false
  }
}

const handleClean = () => {
  ElMessageBox.confirm('确定要清空所有词频数据吗？', '确认', { type: 'warning' })
    .then(async () => {
      const res = await cleanApi()
      if (res.code === 0) { ElMessage.success('已清空'); loadWordFrequencyData() }
    }).catch(() => {})
}

const updateCharts = (list: WordFreq[]) => {
  const top10 = list.slice(0, 10)
  const others = list.slice(10)

  // Word Cloud
  if (wordCloudRef.value) {
    wordCloudChart?.dispose()
    wordCloudChart = echarts.init(wordCloudRef.value)
    const wordCloudData = top10.map((item, i) => ({
      name: item.word,
      value: item.count,
      textStyle: { color: ['#0891B2', '#6366F1', '#8B5CF6', '#EC4899', '#F59E0B', '#10B981', '#14B8A6', '#F97316', '#E11D48', '#0E7490'][i % 10] }
    }))
    wordCloudChart.setOption({
      series: [{ type: 'wordCloud', shape: 'circle', sizeRange: [14, 48], rotationRange: [-30, 30], gridSize: 8, drawOutOfBound: false, data: wordCloudData }]
    } as EChartsOption)
  }

  // Bar Chart
  if (barChartRef.value) {
    barChart?.dispose()
    barChart = echarts.init(barChartRef.value)
    barChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 50, right: 20, top: 20, bottom: 40 },
      xAxis: { type: 'category', data: top10.map(i => i.word), axisLabel: { rotate: 30, fontSize: 11 } },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: top10.map(i => i.count), itemStyle: { color: '#0891B2', borderRadius: [4, 4, 0, 0] }, barWidth: '60%' }]
    })
  }

  // Pie Chart
  if (pieChartRef.value) {
    pieChart?.dispose()
    pieChart = echarts.init(pieChartRef.value)
    const pieData = top10.map(i => ({ name: i.word, value: i.count }))
    if (others.length > 0) pieData.push({ name: '其他', value: others.reduce((s, i) => s + i.count, 0) })
    pieChart.setOption({
      tooltip: { trigger: 'item' },
      series: [{ type: 'pie', radius: ['30%', '60%'], center: ['50%', '50%'], data: pieData, label: { fontSize: 11 }, itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 } }]
    })
  }
}

const handleResize = () => { wordCloudChart?.resize(); barChart?.resize(); pieChart?.resize() }

onMounted(() => {
  loadWordFrequencyData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  wordCloudChart?.dispose()
  barChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped lang="less">
.page-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #F8FAFC;
}

.page-header {
  background: linear-gradient(135deg, #0F172A 0%, #1E293B 100%);
  padding: 24px 32px;
  flex-shrink: 0;
}

.page-header-content {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #FFFFFF;
  margin: 0 0 4px;
}

.page-desc {
  font-size: 13px;
  color: #94A3B8;
  margin: 0;
}

.page-header-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.page-body {
  flex: 1;
  padding: 20px 32px;
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
  box-sizing: border-box;
  overflow-y: auto;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  height: 100%;
}

.chart-card {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  &:nth-child(3) {
    grid-column: 1 / -1;
  }
}

.chart-header {
  padding: 16px 20px 0;

  h3 {
    font-size: 15px;
    font-weight: 600;
    color: #0F172A;
    margin: 0;
  }
}

.chart-box {
  flex: 1;
  min-height: 300px;
}
</style>
