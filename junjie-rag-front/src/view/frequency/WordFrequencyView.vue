<template>
  <div class="page-container">
    <el-card class="page-card">
      <div class="toolbar">
        <el-button type="primary" @click="loadWordFrequencyData">刷新数据</el-button>
        <el-button type="danger" @click="handleClean">清空数据</el-button>
      </div>

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

        <div class="chart-card">
          <div class="chart-header">
            <h3>热词趋势</h3>
          </div>
          <div ref="lineChartRef" v-loading="isLoading" class="chart-box"></div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import 'echarts-wordcloud'
import { cleanFrequencyApi, listFrequencyApi } from '@/api/FrequencyApi'

const wordCloudRef = ref<HTMLElement | null>(null)
const barChartRef = ref<HTMLElement | null>(null)
const pieChartRef = ref<HTMLElement | null>(null)
const lineChartRef = ref<HTMLElement | null>(null)
const charts: Record<string, echarts.ECharts | null> = { wordCloud: null, barChart: null, pieChart: null, lineChart: null }
const isLoading = ref(false)

const initCharts = () => {
  if (wordCloudRef.value) charts.wordCloud = echarts.init(wordCloudRef.value)
  if (barChartRef.value) charts.barChart = echarts.init(barChartRef.value)
  if (pieChartRef.value) charts.pieChart = echarts.init(pieChartRef.value)
  if (lineChartRef.value) charts.lineChart = echarts.init(lineChartRef.value)
}

const loadWordFrequencyData = async () => {
  if (!charts.wordCloud) return
  isLoading.value = true
  try {
    const response = await listFrequencyApi()
    if (response.code === 0 && response.data) {
      const data = response.data.map((item: any) => ({ name: item.word, value: item.countNum, time: item.updateTime }))

      charts.wordCloud?.setOption({
        tooltip: { show: true },
        series: [{ type: 'wordCloud', shape: 'circle', left: 'center', top: 'center', width: '90%', height: '90%', sizeRange: [12, 60], rotationRange: [-90, 90], rotationStep: 45, gridSize: 8, textStyle: { fontFamily: 'Fira Sans, sans-serif', fontWeight: 'bold', color: () => 'rgb(' + [Math.round(Math.random() * 160), Math.round(Math.random() * 160), Math.round(Math.random() * 160)].join(',') + ')' }, emphasis: { focus: 'self', textStyle: { shadowBlur: 10, shadowColor: '#333' } }, data }]
      })

      const top10Data = [...data].sort((a, b) => b.value - a.value).slice(0, 10)
      charts.barChart?.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', data: top10Data.map(item => item.name), axisLabel: { interval: 0, rotate: 30 } },
        yAxis: { type: 'value' },
        series: [{ name: '词频', type: 'bar', data: top10Data.map(item => item.value), itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#0891B2' }, { offset: 1, color: '#22D3EE' }]), borderRadius: [4, 4, 0, 0] } }]
      })

      const frequencyRanges = [{ name: '极高频(>10000)', min: 10000, max: Infinity }, { name: '高频(1000-10000)', min: 1000, max: 10000 }, { name: '中频(100-1000)', min: 100, max: 1000 }, { name: '低频(10-100)', min: 10, max: 100 }, { name: '极低频(<10)', min: 0, max: 10 }]
      const pieData = frequencyRanges.map(range => ({ name: range.name, value: data.filter(item => item.value >= range.min && item.value < range.max).length }))
      charts.pieChart?.setOption({
        tooltip: { trigger: 'item' },
        legend: { orient: 'vertical', left: 'left', textStyle: { fontSize: 12 } },
        series: [{ name: '词频分布', type: 'pie', radius: ['30%', '60%'], avoidLabelOverlap: true, label: { show: false }, emphasis: { label: { show: true, fontSize: 14 } }, data: pieData }]
      })

      const top5Words = [...data].sort((a, b) => b.value - a.value).slice(0, 5).map(item => item.name)
      charts.lineChart?.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: top5Words, textStyle: { fontSize: 12 } },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', boundaryGap: false, data: ['最近7天', '最近6天', '最近5天', '最近4天', '最近3天', '最近2天', '今天'] },
        yAxis: { type: 'value' },
        series: top5Words.map((word, i) => ({
          name: word, type: 'line', smooth: true,
          data: Array(7).fill(null).map(() => Math.floor(data.find(item => item.name === word)?.value * (0.3 + Math.random() * 0.4) || 0)),
          itemStyle: { color: ['#0891B2', '#22D3EE', '#22C55E', '#F59E0B', '#EF4444'][i] }
        }))
      })
    } else { ElMessage.error(response.message || '获取数据失败') }
  } catch { ElMessage.error('获取数据失败') }
  finally { isLoading.value = false }
}

const handleClean = () => {
  ElMessageBox.confirm('确定要清空所有词频数据吗？', '确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    .then(async () => {
      try { const response = await cleanFrequencyApi(); if (response.code === 0) { ElMessage.success('清空数据成功'); loadWordFrequencyData() } else { ElMessage.error(response.message || '清空数据失败') } }
      catch { ElMessage.error('清空数据失败') }
    }).catch(() => { ElMessage.info('已取消清空操作') })
}

const handleResize = () => { Object.values(charts).forEach(chart => chart?.resize()) }

onMounted(() => { initCharts(); loadWordFrequencyData(); window.addEventListener('resize', handleResize) })
onUnmounted(() => { window.removeEventListener('resize', handleResize); Object.values(charts).forEach(chart => chart?.dispose()) })
</script>

<style scoped lang="less">
.page-container {
  height: calc(100vh - 32px);
  padding: 0;
  box-sizing: border-box;
}

.page-card {
  height: 100%;
  display: flex;
  flex-direction: column;

  :deep(.el-card__body) {
    flex: 1;
    display: flex;
    flex-direction: column;
    padding: 24px;
    overflow: hidden;
  }
}

.toolbar {
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
}

.charts-grid {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 1fr 1fr;
  gap: 16px;
  min-height: 0;
}

.chart-card {
  background: #F8FAFC;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .chart-header {
    margin-bottom: 12px;

    h3 {
      margin: 0;
      font-size: 14px;
      font-weight: 600;
      color: var(--text-primary);
    }
  }

  .chart-box {
    flex: 1;
    min-height: 0;
    width: 100%;
  }
}
</style>
