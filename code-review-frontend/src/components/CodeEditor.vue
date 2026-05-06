<template>
  <div class="simple-editor">
    <textarea
      :value="modelValue"
      @input="handleInput"
      :readonly="readOnly"
      :placeholder="placeholder"
      class="editor-textarea"
      :rows="rows"
    ></textarea>
  </div>
</template>

<script setup>
const props = defineProps({
  modelValue: String,
  language: { type: String, default: 'java' },
  readOnly: { type: Boolean, default: false },
  placeholder: { type: String, default: '请输入代码...' },
  rows: { type: Number, default: 12 }
})

const emit = defineEmits(['update:modelValue'])

const handleInput = (event) => {
  emit('update:modelValue', event.target.value)
}
</script>

<style scoped>
.simple-editor {
  width: 100%;
}

.editor-textarea {
  width: 100%;
  min-height: 300px;
  padding: 16px;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 14px;
  background: #1e1e1e;
  color: #d4d4d4;
  border: none;
  border-radius: 10px;
  resize: vertical;
  line-height: 1.6;
}

.editor-textarea::placeholder {
  color: #666;
}

.editor-textarea:focus {
  outline: none;
  box-shadow: 0 0 0 2px #667eea;
}

.editor-textarea:read-only {
  background: #2d2d2d;
  cursor: not-allowed;
}
</style>