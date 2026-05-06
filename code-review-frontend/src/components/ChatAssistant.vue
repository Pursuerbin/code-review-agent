<template>
  <div class="chat-container">
    <div class="chat-header">
      <div class="header-info">
        <span class="chat-icon">💬</span>
        <span class="chat-title">智能研发助理</span>
      </div>
      <div class="header-actions">
        <el-button @click="clearChat" text type="primary">
          <el-icon><RefreshLeft /></el-icon>
          清空对话
        </el-button>
      </div>
    </div>

    <div class="chat-messages" ref="messagesContainer">
      <div class="welcome-message">
        <div class="welcome-icon">🤖</div>
        <div class="welcome-content">
          <p>你好！我是你的智能研发助理。</p>
          <p>我可以帮助你：</p>
          <ul>
            <li>✨ 根据需求描述生成代码</li>
            <li>🔍 逐行分析代码问题</li>
            <li>🚀 优化代码性能和可读性</li>
            <li>🧪 生成单元测试</li>
            <li>📊 分析代码复杂度</li>
          </ul>
          <p>请直接输入你的需求，或者选择下方的快捷功能！</p>
        </div>
      </div>

      <div 
        v-for="(msg, index) in messages" 
        :key="index" 
        class="message-item"
        :class="{ 'user-message': msg.isUser, 'assistant-message': !msg.isUser }"
      >
        <div class="message-avatar">
          {{ msg.isUser ? '👤' : '🤖' }}
        </div>
        <div class="message-content">
          <div class="message-header">
            <span class="sender-name">{{ msg.isUser ? '我' : '智能助理' }}</span>
            <span class="message-time">{{ msg.time }}</span>
          </div>
          <div class="message-body">
            <div v-if="msg.isUser" class="user-input">
              <div class="input-meta">
                <span v-if="msg.actionType" class="action-tag">{{ getActionName(msg.actionType) }}</span>
                <span v-if="msg.language" class="lang-tag">{{ msg.language }}</span>
              </div>
              <pre>{{ msg.content }}</pre>
            </div>
            <div v-else class="assistant-response">
              <div class="markdown-body" v-html="marked(msg.content)"></div>
            </div>
          </div>
          <div v-if="msg.status === 'loading'" class="loading-indicator">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>正在思考...</span>
          </div>
          <div v-if="msg.status === 'error'" class="error-indicator">
            <el-icon><Warning /></el-icon>
            <span>处理失败，请重试</span>
          </div>
        </div>
      </div>
    </div>

    <div class="chat-input-area">
      <div class="function-bar">
        <el-tooltip v-for="func in quickFunctions" :key="func.type" content="点击使用" placement="top">
          <el-button 
            @click="useQuickFunction(func)" 
            class="func-btn"
            :class="{ 'active': selectedFunction === func.type }"
          >
            <span class="btn-icon">{{ func.icon }}</span>
            <span class="btn-text">{{ func.name }}</span>
          </el-button>
        </el-tooltip>
      </div>

      <div class="input-row">
        <div class="input-controls">
          <el-select v-model="currentLanguage" placeholder="选择语言" size="small">
            <el-option label="☕ Java" value="java"></el-option>
            <el-option label="🐍 Python" value="python"></el-option>
            <el-option label="🌐 JavaScript" value="javascript"></el-option>
            <el-option label="⚙️ Go" value="go"></el-option>
            <el-option label="💎 Ruby" value="ruby"></el-option>
          </el-select>
        </div>
        
        <div class="input-wrapper">
          <textarea
            v-model="inputContent"
            @keydown.enter.exact.prevent="sendMessage"
            placeholder="输入代码或描述你的需求..."
            class="chat-input"
            :rows="3"
          ></textarea>
        </div>

        <el-button 
          @click="sendMessage" 
          :disabled="!inputContent.trim() || isLoading"
          type="primary" 
          class="send-btn"
          :loading="isLoading"
        >
          <el-icon><Paperclip /></el-icon>
          {{ isLoading ? '发送中...' : '发送' }}
        </el-button>
      </div>

      <div v-if="selectedFunction && showCodeInput" class="code-input-area">
        <label class="code-label">代码内容：</label>
        <textarea
          v-model="codeContent"
          placeholder="请粘贴或输入代码..."
          class="code-input"
          :rows="6"
        ></textarea>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading, RefreshLeft, Paperclip, Warning } from '@element-plus/icons-vue'
import { executeAssistant } from '@/api/review'
import { marked } from 'marked'

const messages = ref([])
const inputContent = ref('')
const codeContent = ref('')
const currentLanguage = ref('java')
const selectedFunction = ref(null)
const isLoading = ref(false)
const showCodeInput = ref(false)
const messagesContainer = ref(null)

const quickFunctions = [
  { type: 'CodeGenerationAgent', name: '代码生成', icon: '✨', needCode: false },
  { type: 'LineByLineAnalysisAgent', name: '逐行分析', icon: '🔍', needCode: true },
  { type: 'CodeOptimizationAgent', name: '代码优化', icon: '🚀', needCode: true },
  { type: 'CodeCompletionAgent', name: '代码补全', icon: '💡', needCode: true },
  { type: 'UnitTestGenerationAgent', name: '测试生成', icon: '🧪', needCode: true },
  { type: 'ComplexityAnalysisAgent', name: '复杂度分析', icon: '📊', needCode: true }
]

const getActionName = (actionType) => {
  const func = quickFunctions.find(f => f.type === actionType)
  return func ? func.name : actionType
}

const useQuickFunction = (func) => {
  if (selectedFunction.value === func.type) {
    selectedFunction.value = null
    showCodeInput.value = false
  } else {
    selectedFunction.value = func.type
    showCodeInput.value = func.needCode
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const sendMessage = async () => {
  if (!inputContent.value.trim()) return
  
  const userInput = inputContent.value.trim()
  const finalContent = selectedFunction.value && showCodeInput.value && codeContent.value.trim() 
    ? codeContent.value 
    : userInput

  if (!finalContent.trim()) {
    ElMessage.warning(selectedFunction.value ? '请输入代码内容' : '请输入内容')
    return
  }

  isLoading.value = true
  
  const message = {
    isUser: true,
    content: finalContent,
    time: new Date().toLocaleTimeString('zh-CN'),
    actionType: selectedFunction.value,
    language: currentLanguage.value
  }
  messages.value.push(message)
  
  const responseMessage = {
    isUser: false,
    content: '',
    time: '',
    status: 'loading'
  }
  messages.value.push(responseMessage)
  
  await scrollToBottom()

  try {
    const res = await executeAssistant(
      selectedFunction.value || 'CodeGenerationAgent',
      finalContent,
      currentLanguage.value,
      selectedFunction.value && !showCodeInput.value ? userInput : ''
    )
    
    const taskId = res.data.taskId
    await pollTask(taskId, responseMessage)
    
  } catch (error) {
    responseMessage.status = 'error'
    responseMessage.content = '请求失败：' + error.message
    ElMessage.error('请求失败：' + error.message)
  } finally {
    isLoading.value = false
    inputContent.value = ''
    codeContent.value = ''
    await scrollToBottom()
  }
}

const pollTask = async (taskId, message) => {
  const pollingInterval = 2000
  const maxAttempts = 30
  let attempts = 0
  let isCompleted = false

  while (!isCompleted && attempts < maxAttempts) {
    attempts++
    try {
      const res = await fetch(`/api/review/task/${taskId}`)
      const data = await res.json()
      
      if (data.status === 2) {
        message.status = 'success'
        message.content = data.resultSummary || '处理完成'
        message.time = new Date().toLocaleTimeString('zh-CN')
        isCompleted = true
      } else if (data.status === 3) {
        message.status = 'error'
        message.content = data.resultSummary || '处理失败'
        message.time = new Date().toLocaleTimeString('zh-CN')
        isCompleted = true
      }
      
      if (!isCompleted) {
        await new Promise(resolve => setTimeout(resolve, pollingInterval))
      }
    } catch (error) {
      if (attempts >= maxAttempts) {
        message.status = 'error'
        message.content = '请求失败：' + error.message
        message.time = new Date().toLocaleTimeString('zh-CN')
        isCompleted = true
      } else {
        await new Promise(resolve => setTimeout(resolve, pollingInterval))
      }
    }
    messages.value = [...messages.value]
    await scrollToBottom()
  }
  
  if (!isCompleted && attempts >= maxAttempts) {
    message.status = 'error'
    message.content = '请求超时'
    message.time = new Date().toLocaleTimeString('zh-CN')
    messages.value = [...messages.value]
  }
}

const clearChat = () => {
  messages.value = []
  selectedFunction.value = null
  showCodeInput.value = false
}

onMounted(() => {
  scrollToBottom()
})
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f5f7fa;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-icon {
  font-size: 24px;
}

.chat-title {
  font-size: 18px;
  font-weight: 600;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: white;
}

.welcome-message {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px;
  background: linear-gradient(135deg, #f8f9ff 0%, #f0f2ff 100%);
  border-radius: 16px;
  margin-bottom: 20px;
}

.welcome-icon {
  font-size: 48px;
}

.welcome-content {
  flex: 1;
}

.welcome-content p {
  margin: 8px 0;
  color: #333;
}

.welcome-content ul {
  margin: 12px 0;
  padding-left: 20px;
}

.welcome-content li {
  margin: 6px 0;
  color: #555;
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.message-item.user-message {
  flex-direction: row-reverse;
}

.message-item.user-message .message-content {
  align-items: flex-end;
}

.message-avatar {
  font-size: 32px;
  flex-shrink: 0;
}

.message-content {
  max-width: 75%;
  display: flex;
  flex-direction: column;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.sender-name {
  font-weight: 600;
  font-size: 14px;
}

.user-message .sender-name {
  color: #667eea;
}

.assistant-message .sender-name {
  color: #764ba2;
}

.message-time {
  font-size: 12px;
  color: #999;
}

.message-body {
  padding: 12px 16px;
  border-radius: 16px;
  line-height: 1.6;
}

.user-message .message-body {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 16px 4px 16px 16px;
}

.assistant-message .message-body {
  background: #f5f7fa;
  color: #333;
  border-radius: 4px 16px 16px 16px;
}

.user-input {
  word-break: break-all;
}

.input-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.action-tag, .lang-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.2);
}

.lang-tag {
  background: rgba(255, 255, 255, 0.3);
}

.user-input pre {
  margin: 0;
  font-family: 'Fira Code', monospace;
  font-size: 14px;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.assistant-response {
  word-break: break-all;
}

.markdown-body {
  font-size: 14px;
  line-height: 1.8;
}

.markdown-body h1, .markdown-body h2, .markdown-body h3 {
  margin-top: 12px;
  margin-bottom: 8px;
  font-weight: 600;
}

.markdown-body h1 { font-size: 18px; }
.markdown-body h2 { font-size: 16px; }
.markdown-body h3 { font-size: 14px; }

.markdown-body p { margin-bottom: 8px; }

.markdown-body code {
  background: #e8eaf0;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Fira Code', monospace;
  font-size: 13px;
}

.markdown-body pre {
  background: #1e1e1e;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
}

.markdown-body pre code {
  background: transparent;
  color: #d4d4d4;
  padding: 0;
}

.markdown-body table {
  width: 100%;
  border-collapse: collapse;
  margin: 8px 0;
  font-size: 13px;
}

.markdown-body table th,
.markdown-body table td {
  border: 1px solid #ddd;
  padding: 6px 10px;
  text-align: left;
}

.markdown-body table th {
  background: #eee;
}

.loading-indicator, .error-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  color: #666;
}

.error-indicator {
  color: #f56c6c;
}

.chat-input-area {
  padding: 12px 16px;
  background: white;
  border-top: 1px solid #eee;
  max-height: 280px;
}

.function-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  overflow-x: auto;
  padding-bottom: 8px;
}

.func-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  transition: all 0.3s ease;
}

.func-btn:hover {
  background: #f0f2ff;
  border-color: #667eea;
}

.func-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: transparent;
  color: white;
}

.btn-icon {
  font-size: 14px;
}

.btn-text {
  white-space: nowrap;
}

.input-row {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.input-controls {
  flex-shrink: 0;
}

.input-wrapper {
  flex: 1;
}

.chat-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  font-size: 14px;
  resize: none;
  transition: all 0.3s ease;
}

.chat-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
}

.send-btn {
  flex-shrink: 0;
  padding: 12px 24px;
  border-radius: 12px;
}

.code-input-area {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #e4e7ed;
}

.code-label {
  font-size: 12px;
  font-weight: 500;
  color: #666;
  margin-bottom: 6px;
  display: block;
}

.code-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  font-family: 'Fira Code', monospace;
  font-size: 13px;
  background: #1e1e1e;
  color: #d4d4d4;
  resize: none;
  transition: all 0.3s ease;
  max-height: 120px;
}

.code-input:focus {
  outline: none;
  border-color: #667eea;
}

.code-input::placeholder {
  color: #666;
}
</style>