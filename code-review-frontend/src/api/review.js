import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

export const submitTask = (codeContent, lang = 'java') => {
  return api.post('/review/submit', { codeContent, lang })
}

export const getTask = (taskId) => {
  return api.get(`/review/task/${taskId}`)
}

export const executeAssistant = (actionType, input, lang = 'java', extraContext = '') => {
  return api.post('/assistant/execute', {
    actionType,
    input,
    language: lang,
    extraContext
  })
}

export const listAgents = () => {
  return api.get('/assistant/agents')
}