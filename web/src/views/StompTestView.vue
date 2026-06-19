<script setup lang="ts">
import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs'
import { computed, onBeforeUnmount, ref } from 'vue'

type ConnectionStatus = 'disconnected' | 'connecting' | 'connected'

interface StompMessage {
  id: number
  time: string
  type: 'info' | 'sent' | 'received' | 'error'
  text: string
}

const defaultUrl = `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}/ws`

const wsUrl = ref(defaultUrl)
const connectHeaders = ref('')
const subscribeDestination = ref('/topic/test')
const sendDestination = ref('/app/test')
const messageBody = ref('{"message":"hello"}')
const status = ref<ConnectionStatus>('disconnected')
const clientId = ref('')
const subscriptionId = ref('sub-0')
const logs = ref<StompMessage[]>([])

let stompClient: Client | null = null
let subscription: StompSubscription | null = null
let logId = 0

const statusText = computed(() => {
  if (status.value === 'connecting') return '连接中'
  if (status.value === 'connected') return '已连接'
  return '未连接'
})

const canSend = computed(() => status.value === 'connected')

function now() {
  return new Date().toLocaleTimeString()
}

function addLog(type: StompMessage['type'], text: string) {
  logs.value.unshift({
    id: ++logId,
    time: now(),
    type,
    text,
  })
}

function parseHeaders(input: string) {
  return input
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .reduce<Record<string, string>>((headers, line) => {
      const separatorIndex = line.indexOf(':')

      if (separatorIndex > -1) {
        const key = line.slice(0, separatorIndex).trim()
        const value = line.slice(separatorIndex + 1).trim()

        if (key) headers[key] = value
      }

      return headers
    }, {})
}

function connect() {
  if (status.value !== 'disconnected') return

  status.value = 'connecting'
  clientId.value = ''
  subscription = null
  stompClient = new Client({
    connectHeaders: parseHeaders(connectHeaders.value),
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    reconnectDelay: 0,
    webSocketFactory: () => new WebSocket(wsUrl.value),
    debug: (message) => addLog('info', message),
    onConnect: (frame) => {
      status.value = 'connected'
      clientId.value = frame.headers.session ?? ''
      addLog('info', 'STOMP 连接成功。')
    },
    onStompError: (frame) => {
      addLog('error', frame.body || frame.headers.message || '服务端返回 STOMP ERROR。')
    },
    onWebSocketError: () => {
      status.value = 'disconnected'
      addLog('error', 'WebSocket 连接发生错误。')
    },
    onWebSocketClose: () => {
      status.value = 'disconnected'
      subscription = null
      stompClient = null
      addLog('info', '连接已关闭。')
    },
  })

  stompClient.activate()
}

function disconnect() {
  subscription = null
  void stompClient?.deactivate()
}

function subscribe() {
  if (!subscribeDestination.value.trim()) {
    addLog('error', '请填写订阅地址。')
    return
  }

  subscription?.unsubscribe()
  subscription = stompClient?.subscribe(
    subscribeDestination.value.trim(),
    (message: IMessage) => {
      addLog('received', message.body)
    },
    {
      id: subscriptionId.value || 'sub-0',
      ack: 'auto',
    },
  ) ?? null
  addLog('sent', `SUBSCRIBE ${subscribeDestination.value.trim()}`)
}

function sendMessage() {
  if (!sendDestination.value.trim()) {
    addLog('error', '请填写发送地址。')
    return
  }

  stompClient?.publish({
    destination: sendDestination.value.trim(),
    body: messageBody.value,
    headers: {
      'content-type': 'application/json;charset=UTF-8',
    },
  })
  addLog('sent', `SEND ${sendDestination.value.trim()}\n\n${messageBody.value}`)
}

function clearLogs() {
  logs.value = []
}

onBeforeUnmount(() => {
  void stompClient?.deactivate()
  stompClient = null
  subscription = null
})
</script>

<template>
  <main class="stomp-page">
    <section class="toolbar">
      <div>
        <p class="eyebrow">STOMP 协议测试</p>
        <h1>WebSocket STOMP 调试台</h1>
      </div>
      <span class="status" :class="status">{{ statusText }}</span>
    </section>

    <section class="panel connection-panel">
      <label>
        WebSocket 地址
        <input v-model="wsUrl" type="text" placeholder="ws://localhost:8080/ws" />
      </label>

      <label>
        CONNECT 头
        <textarea
          v-model="connectHeaders"
          rows="4"
          placeholder="Authorization: Bearer token&#10;login: user&#10;passcode: password"
        />
      </label>

      <div class="actions">
        <button type="button" class="primary" :disabled="status !== 'disconnected'" @click="connect">
          连接
        </button>
        <button type="button" :disabled="status === 'disconnected'" @click="disconnect">
          断开
        </button>
      </div>

      <p v-if="clientId" class="session">Session: {{ clientId }}</p>
    </section>

    <section class="grid">
      <div class="panel">
        <h2>订阅</h2>
        <label>
          订阅地址
          <input v-model="subscribeDestination" type="text" placeholder="/topic/test" />
        </label>
        <label>
          订阅 ID
          <input v-model="subscriptionId" type="text" placeholder="sub-0" />
        </label>
        <button type="button" :disabled="!canSend" @click="subscribe">订阅</button>
      </div>

      <div class="panel">
        <h2>发送</h2>
        <label>
          发送地址
          <input v-model="sendDestination" type="text" placeholder="/app/test" />
        </label>
        <label>
          消息内容
          <textarea v-model="messageBody" rows="7" />
        </label>
        <button type="button" class="primary" :disabled="!canSend" @click="sendMessage">
          发送
        </button>
      </div>
    </section>

    <section class="panel log-panel">
      <div class="log-heading">
        <h2>帧日志</h2>
        <button type="button" @click="clearLogs">清空</button>
      </div>

      <ol v-if="logs.length" class="logs">
        <li v-for="log in logs" :key="log.id" :class="log.type">
          <span>{{ log.time }}</span>
          <pre>{{ log.text }}</pre>
        </li>
      </ol>
      <p v-else class="empty">暂无日志</p>
    </section>
  </main>
</template>

<style scoped>
.stomp-page {
  width: min(980px, 100%);
  padding: 24px 0;
}

.toolbar,
.log-heading,
.actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar,
.log-heading {
  justify-content: space-between;
}

.eyebrow,
.session,
.empty {
  color: var(--color-text);
  opacity: 0.72;
}

.eyebrow {
  margin: 0 0 6px;
  font-size: 13px;
}

h1,
h2 {
  margin: 0;
  line-height: 1.25;
}

h1 {
  font-size: 28px;
}

h2 {
  font-size: 18px;
}

.status {
  min-width: 72px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 6px 10px;
  text-align: center;
  font-size: 13px;
}

.status.connected {
  border-color: #1f8f5f;
  color: #1f8f5f;
}

.status.connecting {
  border-color: #b77b00;
  color: #b77b00;
}

.panel {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 18px;
  background: var(--color-background-soft);
}

.connection-panel,
.grid,
.log-panel {
  margin-top: 18px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

label {
  display: grid;
  gap: 8px;
  margin-top: 14px;
  font-size: 14px;
}

input,
textarea {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 10px 12px;
  color: var(--color-text);
  background: var(--color-background);
  font: inherit;
}

textarea {
  resize: vertical;
}

button {
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 9px 14px;
  color: var(--color-text);
  background: var(--color-background);
  cursor: pointer;
}

button.primary {
  border-color: #1f8f5f;
  color: #ffffff;
  background: #1f8f5f;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.actions {
  margin-top: 16px;
}

.session {
  margin: 12px 0 0;
  font-size: 13px;
}

.logs {
  display: grid;
  gap: 10px;
  max-height: 420px;
  margin: 16px 0 0;
  padding: 0;
  overflow: auto;
  list-style: none;
}

.logs li {
  border-left: 3px solid var(--color-border);
  padding: 10px 12px;
  background: var(--color-background);
}

.logs li.sent {
  border-color: #2f72c4;
}

.logs li.received {
  border-color: #1f8f5f;
}

.logs li.error {
  border-color: #c23838;
}

.logs span {
  display: block;
  margin-bottom: 6px;
  color: var(--color-text);
  opacity: 0.66;
  font-size: 12px;
}

pre {
  margin: 0;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
}

@media (max-width: 760px) {
  .toolbar,
  .grid {
    display: block;
  }

  .status {
    display: inline-block;
    margin-top: 12px;
  }

  .grid .panel + .panel {
    margin-top: 18px;
  }
}
</style>
