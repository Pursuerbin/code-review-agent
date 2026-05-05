import axios from 'axios'

const api = axios.create({
  baseURL: '/api',   // 代理会转发到后端
  timeout: 30000
})

// 提交审查任务
export const submitTask = (codeContent, lang = 'java') => {
  return api.post('/review/submit', { codeContent, lang })
}

// 查询任务状态和结果
export const getTask = (taskId) => {
  return api.get(`/review/task/${taskId}`)
}