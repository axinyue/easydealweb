<script setup lang="ts">
import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

interface TempUser {
  id: number
  username: string
  locale: string
}

interface RoomMember {
  id: number
  tempUserId: number
  memberNo: number
  role: string
  username: string
  joinedAt: string
}

interface RoomBid {
  id: number
  memberId: number
  memberNo: number
  username: string
  amount: string
  createdAt: string
}

interface RoomItem {
  id: number
  title: string
  description: string | null
  saleMode: string
  biddingOpen: boolean
  topBid: RoomBid | null
  bids: RoomBid[]
  createdAt: string
}

interface RoomInviteCode {
  id: number
  code: string
  inviteType: 'ONE_PERSON' | 'ROOM_SHARED' | 'MULTI_PERSON'
  maxUses: number
  usedCount: number
  status: string
  createdAt: string
}

interface DealRoom {
  id: number
  name: string
  hostTempUserId: number
  inviteCode: string
  inviteMaxUses: number
  inviteUsedCount: number
  status: string
  inviteCodes: RoomInviteCode[]
  members: RoomMember[]
  items: RoomItem[]
  createdAt: string
}

const route = useRoute()
const router = useRouter()
const currentUser = ref<TempUser | null>(null)
const room = ref<DealRoom | null>(null)
const ITEM_SALE_MODE_STORAGE_KEY = 'easy_deal_item_sale_mode'
const inviteCode = ref('')
const itemSaleMode = ref<'AUCTION' | 'TENDER'>('AUCTION')
const itemStatusFilter = ref<'OPEN' | 'CLOSED' | 'ALL'>('OPEN')
const bidAmounts = ref<Record<number, string>>({})
const notice = ref('')
const inviteManagerOpen = ref(false)
const memberManagerOpen = ref(false)
const roomSettingsOpen = ref(false)
const bidListItemId = ref<number | null>(null)
const stopNewMembers = ref(false)
const onePersonQuantity = ref(1)
const multiPersonMaxUses = ref(2)
const multiPersonQuantity = ref(1)
const copiedInviteCodes = ref<Record<string, boolean>>({})
const stompStatus = ref<'未连接' | '连接中' | '已连接'>('未连接')

let stompClient: Client | null = null
let roomSubscription: StompSubscription | null = null
let roomEventSubscription: StompSubscription | null = null
let activeStompRoomId: number | null = null

const currentMember = computed(() => {
  if (!room.value || !currentUser.value) return null
  return room.value.members.find((member) => member.tempUserId === currentUser.value?.id) ?? null
})

const isHost = computed(() => currentMember.value?.role === 'HOST')
const roomSharedInvites = computed(() => room.value?.inviteCodes.filter((invite) => invite.inviteType === 'ROOM_SHARED') ?? [])
const onePersonInvites = computed(() => room.value?.inviteCodes.filter((invite) => invite.inviteType === 'ONE_PERSON') ?? [])
const multiPersonInvites = computed(() => room.value?.inviteCodes.filter((invite) => invite.inviteType === 'MULTI_PERSON') ?? [])
const defaultInvite = computed(() => {
  if (!room.value) return null
  return roomSharedInvites.value.find((invite) => invite.code === room.value?.inviteCode) ?? roomSharedInvites.value[0] ?? null
})
const bidListItem = computed(() => {
  if (!room.value || bidListItemId.value === null) return null
  return room.value.items.find((item) => item.id === bidListItemId.value) ?? null
})
const filteredItems = computed(() => {
  if (!room.value) return []
  if (itemStatusFilter.value === 'OPEN') {
    return room.value.items.filter((item) => item.biddingOpen)
  }
  if (itemStatusFilter.value === 'CLOSED') {
    return room.value.items.filter((item) => !item.biddingOpen)
  }
  return room.value.items
})

async function api<T>(url: string, options: RequestInit = {}) {
  const response = await fetch(url, {
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers ?? {}),
    },
    ...options,
  })

  const data = await response.json().catch(() => null)
  if (!response.ok) {
    throw new Error(data?.message ?? '请求失败')
  }
  return data as T
}

async function ensureUser() {
  currentUser.value = await api<TempUser>('/api/temp-users', { method: 'POST' })
}

async function createRoom() {
  await ensureUser()
  const nextRoom = await api<DealRoom>('/api/rooms', {
    method: 'POST',
    body: JSON.stringify({}),
  })
  setRoom(nextRoom)
  notice.value = '房间已创建，复制邀请码给参与者即可加入。'
}

async function joinRoom(roomId?: number, code = inviteCode.value) {
  await ensureUser()
  const nextRoom = await api<DealRoom>('/api/rooms/join', {
    method: 'POST',
    body: JSON.stringify({ roomId, inviteCode: code }),
  })
  setRoom(nextRoom)
  notice.value = '已加入房间。'
}

async function joinRoomFromRoute() {
  const roomId = Number(routeParam(route.params.roomId))
  if (!Number.isInteger(roomId) || roomId <= 0) return

  const code = routeParam(route.params.inviteCode)
  if (room.value?.id === roomId && (!code || room.value.inviteCode.toLowerCase() === code.toLowerCase())) {
    return
  }

  try {
    await ensureUser()
    const nextRoom = await api<DealRoom>('/api/rooms/join', {
      method: 'POST',
      body: JSON.stringify({ roomId, inviteCode: code }),
    })
    inviteCode.value = code
    setRoom(nextRoom, false)
    notice.value = code ? '已通过邀请链接加入房间。' : '已通过房间编号加入房间。'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '加入房间失败。'
  }
}

async function addItem() {
  if (!room.value) return
  const nextRoom = await api<DealRoom>(`/api/rooms/${room.value.id}/items`, {
    method: 'POST',
    body: JSON.stringify({
      saleMode: itemSaleMode.value,
    }),
  })
  setRoom(nextRoom)
}

async function placeBid(itemId: number) {
  if (!room.value) return
  const amount = bidAmounts.value[itemId]
  const nextRoom = await api<DealRoom>(`/api/rooms/${room.value.id}/items/${itemId}/bids`, {
    method: 'POST',
    body: JSON.stringify({ amount }),
  })
  bidAmounts.value[itemId] = ''
  setRoom(nextRoom)
}

async function closeBidding(itemId: number) {
  if (!room.value) return
  const nextRoom = await api<DealRoom>(`/api/rooms/${room.value.id}/items/${itemId}/close`, {
    method: 'POST',
  })
  setRoom(nextRoom)
}

async function removeMember(member: RoomMember) {
  if (!room.value) return
  const confirmed = window.confirm(`确定要将 ${member.memberNo}号 ${member.username} 移出房间吗？`)
  if (!confirmed) return

  const nextRoom = await api<DealRoom>(`/api/rooms/${room.value.id}/members/${member.id}`, {
    method: 'DELETE',
  })
  setRoom(nextRoom, false)
  notice.value = '成员已移出房间。'
}

async function createInviteCodes(inviteType: RoomInviteCode['inviteType'], quantity: number, maxUses: number) {
  if (!room.value) return
  const nextRoom = await api<DealRoom>(`/api/rooms/${room.value.id}/invites`, {
    method: 'POST',
    body: JSON.stringify({
      inviteType,
      maxUses,
      quantity,
    }),
  })
  setRoom(nextRoom, false)
  notice.value = '邀请码已生成。'
}

async function createOnePersonInvites() {
  await createInviteCodes('ONE_PERSON', onePersonQuantity.value, 1)
}

async function createMultiPersonInvites() {
  await createInviteCodes('MULTI_PERSON', multiPersonQuantity.value, multiPersonMaxUses.value)
}

async function resetDefaultInviteCode() {
  if (!room.value) return
  const nextRoom = await api<DealRoom>(`/api/rooms/${room.value.id}/invites/default/reset`, {
    method: 'POST',
  })
  setRoom(nextRoom, false)
  notice.value = '默认邀请码已重置。'
}

function openRoomSettings() {
  if (!room.value) return
  stopNewMembers.value = room.value.status !== 'OPEN'
  roomSettingsOpen.value = true
}

async function updateRoomSettings() {
  if (!room.value) return
  const nextRoom = await api<DealRoom>(`/api/rooms/${room.value.id}/settings`, {
    method: 'POST',
    body: JSON.stringify({
      allowNewMembers: !stopNewMembers.value,
    }),
  })
  setRoom(nextRoom, false)
  roomSettingsOpen.value = false
  notice.value = stopNewMembers.value ? '已停止新用户进入。' : '已允许新用户进入。'
}

async function clearRoom() {
  if (!room.value) return
  const confirmed = window.confirm('清理房间会退出并清理房间所有数据，且不可恢复，是否确认清理？')
  if (!confirmed) return

  const roomId = room.value.id
  await api<void>(`/api/rooms/${roomId}`, {
    method: 'DELETE',
  })
  exitClearedRoom('房间已清理。', false)
}

function exitClearedRoom(message: string, showAlert = true) {
  if (showAlert) {
    window.alert(message)
  }
  activeStompRoomId = null
  roomSubscription?.unsubscribe()
  roomSubscription = null
  roomEventSubscription?.unsubscribe()
  roomEventSubscription = null
  void stompClient?.deactivate()
  stompClient = null
  stompStatus.value = '未连接'
  room.value = null
  currentUser.value = null
  roomSettingsOpen.value = false
  inviteManagerOpen.value = false
  memberManagerOpen.value = false
  bidListItemId.value = null
  notice.value = message
  void router.replace('/')
}

async function copyInviteLink(invite: RoomInviteCode) {
  if (!room.value) return
  const link = new URL(withBasePath(`room/${room.value.id}/${invite.code}`), window.location.origin).toString()

  try {
    await navigator.clipboard.writeText(link)
    copiedInviteCodes.value = {
      ...copiedInviteCodes.value,
      [invite.code]: true,
    }
    notice.value = `${invite.code} 的邀请链接已复制。`
  } catch {
    notice.value = '复制失败，请稍后再试。'
  }
}

function setRoom(nextRoom: DealRoom, syncAddress = true) {
  room.value = nextRoom
  if (syncAddress) {
    void router.replace(`/room/${nextRoom.id}/${nextRoom.inviteCode}`)
  }
  if (activeStompRoomId !== nextRoom.id) {
    connectStomp(nextRoom.id)
  }
}

function routeParam(param: string | string[] | undefined) {
  if (Array.isArray(param)) return param[0] ?? ''
  return param ?? ''
}

function withBasePath(path: string) {
  const base = import.meta.env.BASE_URL.endsWith('/') ? import.meta.env.BASE_URL : `${import.meta.env.BASE_URL}/`
  return `${base}${path}`.replace(/\/{2,}/g, '/')
}

function connectStomp(roomId: number) {
  if (stompClient) {
    void stompClient.deactivate()
    stompClient = null
    roomSubscription = null
    roomEventSubscription = null
  }

  activeStompRoomId = roomId
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const client = new Client({
    brokerURL: `${protocol}://${window.location.host}/ws`,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    reconnectDelay: 5000,
    onConnect: () => {
      stompStatus.value = '已连接'
      roomSubscription = client.subscribe(`/user/queue/rooms/${roomId}`, (message: IMessage) => {
        room.value = JSON.parse(message.body) as DealRoom
        notice.value = '房间状态已同步。'
      })
      roomEventSubscription = client.subscribe(`/user/queue/rooms/${roomId}/events`, (message: IMessage) => {
        const event = JSON.parse(message.body) as { type?: string; message?: string }
        if (event.type === 'ROOM_CLEARED') {
          exitClearedRoom(event.message ?? '房间已被房主清理，请退出。')
        }
      })
    },
    onStompError: (frame) => {
      notice.value = frame.body || frame.headers.message || 'STOMP 连接返回错误。'
    },
    onWebSocketClose: () => {
      if (stompClient === client) {
        stompStatus.value = '未连接'
        roomSubscription = null
        roomEventSubscription = null
        activeStompRoomId = null
      }
    },
  })

  stompClient = client
  stompStatus.value = '连接中'
  client.activate()
}

function formatMoney(value: string) {
  return Number(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

function shortTime(value: string) {
  return new Date(value).toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  })
}

function inviteUsageText(invite: RoomInviteCode) {
  return `${invite.usedCount} / ${invite.maxUses} 次`
}

function saleModeText(saleMode: string) {
  return saleMode === 'TENDER' ? '竞标' : '拍卖'
}

function bidSummaryTitle(item: RoomItem) {
  if (item.saleMode === 'TENDER') {
    return item.biddingOpen ? '竞标状态' : '竞标结果'
  }
  return '当前最高价'
}

function canShowBidResult(item: RoomItem) {
  return item.saleMode !== 'TENDER' || !item.biddingOpen
}

function emptyBidText(item: RoomItem) {
  if (item.saleMode === 'TENDER' && item.biddingOpen) {
    return '未开标'
  }
  return '暂无出价'
}

function bidActionText(item: RoomItem) {
  return item.saleMode === 'TENDER' ? '提交竞标' : '出价'
}

function openBidList(item: RoomItem) {
  bidListItemId.value = item.id
}

function bidListEmptyText(item: RoomItem) {
  if (item.saleMode === 'TENDER' && item.biddingOpen) {
    return '未开标'
  }
  return '暂无出价记录'
}

function visibleBids(item: RoomItem) {
  return canShowBidResult(item) ? item.bids : []
}

onBeforeUnmount(() => {
  activeStompRoomId = null
  roomSubscription = null
  roomEventSubscription = null
  void stompClient?.deactivate()
  stompClient = null
})

onMounted(() => {
  const savedSaleMode = window.localStorage.getItem(ITEM_SALE_MODE_STORAGE_KEY)
  if (savedSaleMode === 'AUCTION' || savedSaleMode === 'TENDER') {
    itemSaleMode.value = savedSaleMode
  }
  void joinRoomFromRoute()
})

watch(itemSaleMode, (nextSaleMode) => {
  window.localStorage.setItem(ITEM_SALE_MODE_STORAGE_KEY, nextSaleMode)
})

watch(
  () => [route.params.roomId, route.params.inviteCode],
  () => {
    void joinRoomFromRoute()
  },
)
</script>

<template>
  <main class="deal-page">
    <section class="workspace">
      <aside class="side-panel">
        <div>
          <h1>线下交易房间</h1>
          <p class="muted">适合线下拍卖和隐藏竞价场景：创建房间后，参与者扫码或输入邀请码加入，可实时公开出价，也可隐藏报价后统一开标。</p>
        </div>

        <div class="field-group">
          <button type="button" class="primary" @click="createRoom">创建房间</button>
        </div>

        <div class="field-group">
          <label>
            邀请码
            <input v-model="inviteCode" type="text" placeholder="输入房主给的邀请码" />
          </label>
          <button type="button" @click="() => joinRoom()">加入房间</button>
        </div>

        <dl class="identity">
          <div>
            <dt>临时用户</dt>
            <dd>{{ currentUser?.username ?? '未创建' }}</dd>
          </div>
          <div>
            <dt>网络连接</dt>
            <dd>{{ stompStatus }}</dd>
          </div>
        </dl>

        <p v-if="notice" class="notice">{{ notice }}</p>
      </aside>

      <section class="room-panel">
        <div v-if="room" class="room-content">
          <div class="room-heading">
            <div>
              <p class="eyebrow">房间 #{{ room.id }}</p>
              <h2>{{ room.name }}</h2>
            </div>
            <div v-if="isHost" class="room-actions">
              <button type="button" class="primary" @click="inviteManagerOpen = true">邀请</button>
              <button type="button" @click="openRoomSettings">设置</button>
            </div>
          </div>

          <section class="members">
            <div class="section-heading">
              <h3>成员</h3>
              <button type="button" @click="memberManagerOpen = true">查看成员</button>
            </div>
            <div class="member-count">
              <span>当前成员</span>
              <strong>{{ room.members.length }}</strong>
              <em>人</em>
            </div>
          </section>

          <section v-if="isHost" class="item-form">
            <h3>快速上架</h3>
            <div class="item-fields">
              <select v-model="itemSaleMode" aria-label="出价模式">
                <option value="AUCTION">拍卖</option>
                <option value="TENDER">竞标</option>
              </select>
              <button type="button" class="primary" @click="addItem">添加商品</button>
            </div>
          </section>

          <section class="items">
            <div class="section-heading">
              <h3>商品与出价</h3>
              <div class="item-filter">
                <button
                  type="button"
                  :class="{ active: itemStatusFilter === 'OPEN' }"
                  @click="itemStatusFilter = 'OPEN'"
                >
                  出价中
                </button>
                <button
                  type="button"
                  :class="{ active: itemStatusFilter === 'CLOSED' }"
                  @click="itemStatusFilter = 'CLOSED'"
                >
                  已关闭
                </button>
                <button
                  type="button"
                  :class="{ active: itemStatusFilter === 'ALL' }"
                  @click="itemStatusFilter = 'ALL'"
                >
                  全部
                </button>
              </div>
            </div>
            <article v-for="item in filteredItems" :key="item.id" class="item-card">
              <div class="item-main">
                <div>
                  <h4>{{ item.title }}</h4>
                  <p>{{ saleModeText(item.saleMode) }}</p>
                </div>
                <span :class="['state', item.biddingOpen ? 'open' : 'closed']">
                  {{ item.biddingOpen ? '出价中' : '已关闭' }}
                </span>
              </div>

              <div class="top-bid">
                <span>{{ bidSummaryTitle(item) }}</span>
                <strong v-if="canShowBidResult(item) && item.topBid">￥{{ formatMoney(item.topBid.amount) }}</strong>
                <strong v-else>{{ emptyBidText(item) }}</strong>
                <small v-if="canShowBidResult(item) && item.topBid">
                  {{ item.topBid.memberNo }}号 {{ item.topBid.username }} · {{ shortTime(item.topBid.createdAt) }}
                </small>
              </div>

              <div class="bid-actions">
                <input
                  v-model="bidAmounts[item.id]"
                  :disabled="!item.biddingOpen"
                  type="number"
                  min="0"
                  step="0.01"
                  placeholder="输入金额"
                />
                <button type="button" :disabled="!item.biddingOpen" @click="placeBid(item.id)">
                  {{ bidActionText(item) }}
                </button>
                <button type="button" @click="openBidList(item)">出价记录</button>
                <button v-if="isHost" type="button" :disabled="!item.biddingOpen" @click="closeBidding(item.id)">
                  关闭出价
                </button>
              </div>
            </article>

            <p v-if="room.items.length === 0" class="empty">房主上架商品后，参与者会实时看到。</p>
            <p v-else-if="filteredItems.length === 0" class="empty">当前筛选下没有商品。</p>
          </section>
        </div>

        <div v-else class="empty-room">
          <h2>先创建或加入一个房间</h2>
          <p>房主可先创建房间并分享邀请码，参与者输入邀请码加入后即可查看商品并参与出价。</p>
        </div>
      </section>
    </section>

    <div v-if="bidListItem" class="modal-backdrop" @click.self="bidListItemId = null">
      <section class="share-dialog" role="dialog" aria-modal="true" aria-labelledby="bid-list-title">
        <div class="share-heading">
          <div>
            <p class="eyebrow">{{ saleModeText(bidListItem.saleMode) }}</p>
            <h2 id="bid-list-title">{{ bidListItem.title }} 出价记录</h2>
          </div>
          <button type="button" class="plain-button" aria-label="关闭出价记录" @click="bidListItemId = null">
            关闭
          </button>
        </div>

        <ul v-if="visibleBids(bidListItem).length" class="bid-dialog-list">
          <li v-for="bid in visibleBids(bidListItem)" :key="bid.id">
            <div>
              <span>{{ bid.memberNo }}号 {{ bid.username }}</span>
              <strong>￥{{ formatMoney(bid.amount) }}</strong>
            </div>
            <time>{{ shortTime(bid.createdAt) }}</time>
          </li>
        </ul>
        <p v-else class="empty">{{ bidListEmptyText(bidListItem) }}</p>
      </section>
    </div>

    <div v-if="roomSettingsOpen && room" class="modal-backdrop" @click.self="roomSettingsOpen = false">
      <section class="share-dialog" role="dialog" aria-modal="true" aria-labelledby="room-settings-title">
        <div class="share-heading">
          <div>
            <p class="eyebrow">房间 #{{ room.id }}</p>
            <h2 id="room-settings-title">设置</h2>
          </div>
          <button type="button" class="plain-button" aria-label="关闭设置" @click="roomSettingsOpen = false">
            关闭
          </button>
        </div>

        <label class="setting-option">
          <input v-model="stopNewMembers" type="checkbox" />
          <span>
            <strong>停止新用户进入</strong>
            <small>开启后，未加入过房间的新用户将无法通过房间号或邀请码进入。</small>
          </span>
        </label>

        <div class="danger-zone">
          <div>
            <strong>清理房间</strong>
            <small>退出并清理房间所有数据，且不可恢复。</small>
          </div>
          <button type="button" class="danger-button" @click="clearRoom">清理房间</button>
        </div>

        <div class="modal-actions">
          <button type="button" @click="roomSettingsOpen = false">取消</button>
          <button type="button" class="primary" @click="updateRoomSettings">保存</button>
        </div>
      </section>
    </div>

    <div v-if="memberManagerOpen && room" class="modal-backdrop" @click.self="memberManagerOpen = false">
      <section class="share-dialog" role="dialog" aria-modal="true" aria-labelledby="member-manager-title">
        <div class="share-heading">
          <div>
            <p class="eyebrow">房间 #{{ room.id }}</p>
            <h2 id="member-manager-title">成员列表</h2>
          </div>
          <button type="button" class="plain-button" aria-label="关闭成员列表" @click="memberManagerOpen = false">
            关闭
          </button>
        </div>

        <ul class="member-dialog-list">
          <li v-for="member in room.members" :key="member.id">
            <div>
              <span>{{ member.memberNo }}号</span>
              <strong>{{ member.username }}</strong>
              <em>{{ member.role === 'HOST' ? '房主' : '参与者' }}</em>
            </div>
            <button
              v-if="isHost && member.role !== 'HOST'"
              type="button"
              class="danger-button"
              @click="removeMember(member)"
            >
              移除
            </button>
          </li>
        </ul>
      </section>
    </div>

    <div v-if="inviteManagerOpen && room" class="modal-backdrop" @click.self="inviteManagerOpen = false">
      <section class="share-dialog wide-dialog" role="dialog" aria-modal="true" aria-labelledby="invite-manager-title">
        <div class="share-heading">
          <div>
            <p class="eyebrow">房间 #{{ room.id }}</p>
            <h2 id="invite-manager-title">邀请</h2>
          </div>
          <button type="button" class="plain-button" aria-label="关闭邀请" @click="inviteManagerOpen = false">
            关闭
          </button>
        </div>

        <div class="invite-columns">
          <section class="invite-column">
            <div>
              <h3>一码一房</h3>
              <p class="muted">房间默认邀请码，适合给所有人使用。</p>
            </div>

            <div v-if="defaultInvite" class="default-invite">
              <span>默认邀请码</span>
              <strong>{{ defaultInvite.code }}</strong>
              <small>{{ inviteUsageText(defaultInvite) }}</small>
              <div class="invite-row-actions">
                <button type="button" @click="copyInviteLink(defaultInvite)">
                  {{ copiedInviteCodes[defaultInvite.code] ? '已复制' : '复制链接' }}
                </button>
                <button type="button" @click="resetDefaultInviteCode">重置</button>
              </div>
            </div>
          </section>

          <section class="invite-column">
            <div>
              <h3>一码一人</h3>
              <p class="muted">每个邀请码仅允许一个人加入。</p>
            </div>

            <div class="invite-generate-row">
              <label>
                生成数量
                <input v-model.number="onePersonQuantity" type="number" min="1" max="50" />
              </label>
              <button type="button" class="primary" @click="createOnePersonInvites">生成</button>
            </div>

            <ul v-if="onePersonInvites.length" class="compact-invite-list">
              <li v-for="invite in onePersonInvites" :key="invite.id">
                <div>
                  <strong>{{ invite.code }}</strong>
                  <small>{{ inviteUsageText(invite) }}</small>
                </div>
                <button type="button" @click="copyInviteLink(invite)">
                  {{ copiedInviteCodes[invite.code] ? '已复制' : '复制链接' }}
                </button>
              </li>
            </ul>
            <p v-else class="empty">还没有一码一人邀请码。</p>
          </section>

          <section class="invite-column">
            <div>
              <h3>一码多人</h3>
              <p class="muted">每个邀请码允许指定人数先到先得。</p>
            </div>

            <div class="invite-generate-row two-inputs">
              <label>
                每码人数
                <input v-model.number="multiPersonMaxUses" type="number" min="1" max="500" />
              </label>
              <label>
                生成数量
                <input v-model.number="multiPersonQuantity" type="number" min="1" max="50" />
              </label>
              <button type="button" class="primary" @click="createMultiPersonInvites">生成</button>
            </div>

            <ul v-if="multiPersonInvites.length" class="compact-invite-list">
              <li v-for="invite in multiPersonInvites" :key="invite.id">
                <div>
                  <strong>{{ invite.code }}</strong>
                  <small>{{ inviteUsageText(invite) }}</small>
                </div>
                <button type="button" @click="copyInviteLink(invite)">
                  {{ copiedInviteCodes[invite.code] ? '已复制' : '复制链接' }}
                </button>
              </li>
            </ul>
            <p v-else class="empty">还没有一码多人邀请码。</p>
          </section>
        </div>
      </section>
    </div>
  </main>
</template>
