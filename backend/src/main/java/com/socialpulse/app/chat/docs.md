# Chat Module — WebSocket & STOMP Documentation

---

## 1. Architecture Overview

```
┌──────────────────────────────┐
│          Clients             │
│   React (browser) / Mobile   │
└──────────────┬───────────────┘
               │
               │  WebSocket + STOMP  (ws://host/ws?token=JWT)
               ▼
┌──────────────────────────────────────────┐
│           Spring Boot Application        │
├──────────────────────────────────────────┤
│  WebSocketAuthInterceptor  (JWT auth)    │
│  WebSocketSecurityConfig   (authz)       │
│  ChatWebSocketController   (STOMP entry) │
│  SendMessageService        (persist)     │
│  NotifyMessageService      (push)        │
│  AcknowledgeReadService    (read ack)    │
│  WebSocketSessionManager   (online state)│
│  ReconnectionService       (reconnect)   │
└──────────┬───────────────────────────────┘
           │                    │
           ▼                    ▼
    ┌─────────────┐      ┌─────────────┐
    │ PostgreSQL  │      │    Redis    │
    │  messages   │      │  sessions   │
    │  convs      │      │  unread     │
    └─────────────┘      │  pending    │
                         └─────────────┘
```

**Current broker**: Spring's in-memory `SimpleBroker` (`/topic`, `/queue`).
**Implication**: All WebSocket connections must land on the **same instance**. See §9 for the path to multi-instance scaling.

---

## 2. WebSocket

### What is it?

WebSocket is a **full-duplex, persistent TCP connection**. Unlike HTTP (request → response → close), WebSocket keeps the connection open so either side can push data at any time.

### HTTP Polling vs WebSocket

```
HTTP polling:
Client → "any new messages?" → Server   (every N seconds)
Client ← "no"               ← Server
Client → "any new messages?" → Server
Client ← "yes, here"        ← Server

WebSocket:
Client ──── connect ────────────────→ Server
       ←──── message (server push) ──
       ──── message (client send) ──→
       ←──── message ──────────────
```

### Handshake

```
Client → Server:
GET /ws HTTP/1.1
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==

Server → Client:
HTTP/1.1 101 Switching Protocols
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
```

After `101 Switching Protocols`, the TCP connection is no longer HTTP.

### Heartbeat

```java
heart-beat:4000,4000
```

Both client and server send a heartbeat frame every **4 seconds**. If either side misses a heartbeat beyond the threshold, Spring WebSocket triggers a `SessionDisconnectEvent` — which calls `SessionManager.removeSession()` to clean up Redis state. This prevents ghost sessions from accumulating when clients drop without a clean close.

---

## 3. STOMP (Simple Text Oriented Messaging Protocol)

### Why STOMP on top of WebSocket?

Raw WebSocket is a byte pipe — no routing, no topics, no subscriptions. STOMP adds a messaging layer:

| Feature         | Raw WebSocket | WebSocket + STOMP |
| --------------- | ------------- | ----------------- |
| Routing         | ❌ manual     | ✅ destinations   |
| Pub/Sub         | ❌ manual     | ✅ `/topic/`      |
| Point-to-point  | ❌ manual     | ✅ `/queue/`      |
| Message headers | ❌ none       | ✅ key-value      |
| Acknowledgment  | ❌ none       | ✅ ACK/NACK       |

### STOMP Frame Structure

```
COMMAND
header1:value1
header2:value2

Body^@
```

`^@` = null byte (frame terminator).

### STOMP Commands

| Direction       | Command       | Purpose                       |
| --------------- | ------------- | ----------------------------- |
| Client → Server | `CONNECT`     | Establish STOMP session       |
| Server → Client | `CONNECTED`   | Confirm session               |
| Client → Server | `SEND`        | Send message to destination   |
| Client → Server | `SUBSCRIBE`   | Subscribe to a destination    |
| Client → Server | `UNSUBSCRIBE` | Cancel subscription           |
| Server → Client | `MESSAGE`     | Deliver message to subscriber |
| Client → Server | `DISCONNECT`  | Close STOMP session           |
| Server → Client | `ERROR`       | Report error                  |

### Destination Conventions

| Prefix    | Meaning                                          | Example              |
| --------- | ------------------------------------------------ | -------------------- |
| `/app/`   | Routes to `@MessageMapping` handler              | `/app/chat.send`     |
| `/topic/` | Pub/Sub broadcast (many subscribers)             | `/topic/chat.42`     |
| `/queue/` | Point-to-point (one recipient)                   | `/queue/errors`      |
| `/user/`  | Per-user delivery (Spring converts to `/queue/`) | `/user/queue/errors` |

### Example Exchange

```
# 1. Client connects
CONNECT
accept-version:1.2
heart-beat:4000,4000
Authorization:Bearer eyJhbGci...
^@

# 2. Server confirms
CONNECTED
version:1.2
heart-beat:4000,4000
^@

# 3. Client subscribes to a conversation
SUBSCRIBE
id:sub-0
destination:/topic/chat.42
^@

# 4. Client sends a message
SEND
destination:/app/chat.send
content-type:application/json

{"conversationId":42,"content":"Hello!"}^@

# 5. Server pushes to all subscribers of /topic/chat.42
MESSAGE
subscription:sub-0
destination:/topic/chat.42

{"id":1,"senderId":7,"content":"Hello!","timestamp":"...","status":"SENT"}^@
```

---

## 4. Data Model

```
Conversation
├── id              BIGSERIAL PK
├── participant1Id  BIGINT FK → users.id
├── participant2Id  BIGINT FK → users.id
├── createdAt       TIMESTAMP
└── lastMessageAt   TIMESTAMP

Message
├── id              BIGSERIAL PK
├── conversationId  BIGINT FK → conversations.id
├── senderId        BIGINT FK → users.id
├── content         TEXT  (max 2000 chars)
├── status          ENUM(SENT, DELIVERED, READ)
└── timestamp       TIMESTAMP
```

**Ordering guarantee**: Messages are ordered by `timestamp` (DB commit time) + monotonic `id`. Within a single conversation, ordering is guaranteed. Clients should sort by `id` ascending as the canonical order.

---

## 5. Full Pipeline

### 5.1 Connection & Authentication

```
┌─────────┐                              ┌──────────────────────────┐
│ Browser │                              │        Backend           │
└────┬────┘                              └────────────┬─────────────┘
     │  HTTP GET /ws?token=JWT                        │
     │  (Upgrade: websocket)                          │
     │───────────────────────────────────────────────>│
     │                                                │ WebSocketAuthInterceptor
     │                                                │  - extract JWT from query param
     │                                                │  - validate & set principal
     │  101 Switching Protocols                       │
     │<───────────────────────────────────────────────│
     │                                                │
     │  STOMP CONNECT (heart-beat:4000,4000)          │
     │───────────────────────────────────────────────>│
     │                                                │ WebSocketEventListener.onConnect()
     │                                                │  - SessionManager.registerSession()
     │                                                │  - Redis: ws:sessions:{userId} += sessionId
     │                                                │  - ReconnectionService.scheduleDelivery()
     │  STOMP CONNECTED                               │    (500ms delay → deliver unread counts)
     │<───────────────────────────────────────────────│
     │                                                │
     │  SUBSCRIBE /topic/chat.{conversationId}        │
     │───────────────────────────────────────────────>│ WebSocketSecurityConfig validates principal
```

### 5.2 Sending a Message

```
┌─────────┐    ┌──────────────────┐    ┌──────────────────┐    ┌───────┐
│ Sender  │    │ ChatWS           │    │ SendMessage      │    │  DB   │
│ Browser │    │ Controller       │    │ Service          │    │       │
└────┬────┘    └────────┬─────────┘    └────────┬─────────┘    └───┬───┘
     │                  │                       │                  │
     │ SEND /app/chat.send                      │                  │
     │ {convId, content}│                       │                  │
     │─────────────────>│                       │                  │
     │                  │ sendMessage()         │                  │
     │                  │──────────────────────>│                  │
     │                  │                       │ validate content │
     │                  │                       │ find conversation│
     │                  │                       │ check participant│
     │                  │                       │ INSERT message   │
     │                  │                       │─────────────────>│
     │                  │                       │ UPDATE lastMsgAt │
     │                  │                       │─────────────────>│
     │                  │                       │ publish          │
     │                  │                       │ MessagePersisted │
     │                  │                       │ Event            │
     │                  │                       │ (AFTER_COMMIT)   │
```

### 5.3 Message Delivery

```
┌──────────────────┐    ┌──────────────────┐    ┌─────────┐    ┌──────────┐
│ NotifyMessage    │    │ SessionManager   │    │  Redis  │    │Recipient │
│ Service          │    │                  │    │         │    │ Browser  │
└────────┬─────────┘    └────────┬─────────┘    └────┬────┘    └────┬─────┘
         │                       │                   │              │
         │ isUserOnline(recipId) │                   │              │
         │──────────────────────>│ SCARD ws:sessions │              │
         │                       │──────────────────>│              │
         │                       │<──────────────────│              │
         │<──────────────────────│                   │              │
         │                       │                   │              │
         │ [online] STOMP push /topic/chat.{convId}  │              │
         │───────────────────────────────────────────────────────>  │
         │ UPDATE status = DELIVERED                 │              │
         │                                           │              │
         │ [offline] INCR chat:unread:{convId}:{id}  │              │
         │──────────────────────────────────────────>│              │
```

### 5.4 Read Acknowledgment

```
┌─────────┐    ┌──────────────────┐    ┌──────────────────┐    ┌──────────┐
│Recipient│    │ ChatWS           │    │ AcknowledgeRead  │    │  Sender  │
│ Browser │    │ Controller       │    │ Service          │    │ Browser  │
└────┬────┘    └────────┬─────────┘    └────────┬─────────┘    └────┬─────┘
     │                  │                       │                   │
     │ SEND /app/chat.read {conversationId}     │                   │
     │─────────────────>│                       │                   │
     │                  │──────────────────────>│                   │
     │                  │                       │ UPDATE → READ     │
     │                  │                       │ DEL unread key    │
     │                  │                       │ STOMP push status │
     │                  │                       │──────────────────>│
```

### 5.5 Reconnection

```
Client reconnects (after network drop / tab restore)
    │
    ├─ STOMP CONNECT
    ├─ WebSocketEventListener.onConnect()
    │    └─ ReconnectionService.scheduleReconnectionDelivery() [500ms delay]
    │         ├─ query all conversations for user
    │         ├─ read chat:unread:{convId}:{userId} from Redis
    │         ├─ push unread counts → /user/queue/unread-counts
    │         └─ pop chat:pending-status:{userId} → /user/queue/status-updates
    │
    └─ Client fetches missed messages via REST:
         GET /api/conversations/{id}/messages?page=0&size=20
```

### 5.6 Disconnect

```
Browser closes / network drops / heartbeat timeout
    → WebSocketEventListener.onDisconnect()
    → SessionManager.removeSession(sessionId)
    → Redis: DEL ws:session:{sessionId}
    → Redis: SREM ws:sessions:{userId} sessionId
    → if no remaining sessions → user is offline
```

---

## 6. Redis Key Schema

| Key                             | Type             | Purpose                                        |
| ------------------------------- | ---------------- | ---------------------------------------------- |
| `ws:sessions:{userId}`          | Set              | Active session IDs → online detection          |
| `ws:session:{sessionId}`        | String           | sessionId → userId (for disconnect cleanup)    |
| `chat:unread:{convId}:{userId}` | String (counter) | Unread count for offline users                 |
| `chat:pending-status:{userId}`  | List             | Pending status updates to deliver on reconnect |

---

## 7. Message Status Lifecycle

```
SENT ──────────────────────────────────────────► READ
  │                                               ▲
  └──► DELIVERED ────────────────────────────────┘

Transitions:
  SENT      → DELIVERED  when recipient is online at delivery time
  SENT      → READ       when recipient acks read (skips DELIVERED)
  DELIVERED → READ       when recipient acks read
```

Valid transitions enforced by `Message.canTransitionTo()`:

- `SENT` cannot be re-set
- `DELIVERED` only from `SENT`
- `READ` from `SENT` or `DELIVERED`

---

## 8. Security

### Authentication

JWT token passed as query parameter on WebSocket upgrade:

```
ws://host/ws?token=eyJhbGci...
```

`WebSocketAuthInterceptor` validates the token on the STOMP `CONNECT` frame and sets the `Authentication` principal on the session.

### Authorization

`WebSocketSecurityConfig` enforces on every STOMP frame:

| Destination pattern         | Allowed                  |
| --------------------------- | ------------------------ |
| `/topic/chat.*` (SUBSCRIBE) | Authenticated users only |
| `/user/queue/*` (SUBSCRIBE) | Authenticated users only |
| `/app/chat.*` (SEND)        | Authenticated users only |

Conversation-level authorization (participant check) is enforced inside `SendMessageService` and `AcknowledgeReadService` — not at the STOMP layer.

### Limits

| Constraint                 | Value      | Enforced in                                 |
| -------------------------- | ---------- | ------------------------------------------- |
| Max sessions per user      | 5          | `WebSocketSessionManager.registerSession()` |
| Max message content length | 2000 chars | `SendMessageService.validateContent()`      |
| Empty/whitespace message   | rejected   | `SendMessageService.validateContent()`      |

---

## 9. Multi-Instance Scaling (Current Limitation & Path Forward)

### Current State

The broker is configured as Spring's **in-memory SimpleBroker**:

```java
config.enableSimpleBroker("/topic", "/queue");
```

This means all WebSocket connections **must land on the same instance**. If User A is on Instance-1 and User B is on Instance-2, Instance-1 cannot push to User B's socket.

```
Instance-1 (User A connected)     Instance-2 (User B connected)
┌──────────────────────────┐      ┌──────────────────────────┐
│  SimpleBroker            │      │  SimpleBroker            │
│  knows User A's socket   │      │  knows User B's socket   │
└──────────────────────────┘      └──────────────────────────┘
         ↑                                    ↑
         │  Instance-1 tries to push to B     │
         │  ← FAILS: B's socket is here ──────┘
```

### Path to Multi-Instance: Redis Pub/Sub Broker Relay

Replace `enableSimpleBroker` with a **STOMP broker relay** backed by a message broker (Redis Pub/Sub or RabbitMQ):

```java
// Replace this:
config.enableSimpleBroker("/topic", "/queue");

// With this (requires RabbitMQ or ActiveMQ with STOMP support):
config.enableStompBrokerRelay("/topic", "/queue")
      .setRelayHost("rabbitmq-host")
      .setRelayPort(61613);
```

With a broker relay:

```
Instance-1 receives message from User A
    │
    └─► Publish to broker relay (RabbitMQ/Redis)
              │
    ┌─────────┴──────────┐
    ▼                    ▼
Instance-1           Instance-2
(User A's socket)    (User B's socket)
                         │
                         └─► push to User B ✓
```

**Until this is implemented**, deploy as a **single instance** or use sticky sessions (load balancer routes each user to the same instance).

---

## 10. Failure Scenarios

| Scenario                      | Current Behavior                                                                                                   |
| ----------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| Client disconnects mid-send   | Transaction still commits; message saved; `NotifyMessageService` increments unread counter                         |
| Recipient offline at delivery | Unread counter incremented in Redis; delivered on reconnect via `ReconnectionService`                              |
| STOMP push fails (exception)  | Caught in `NotifyMessageService`; falls back to incrementing unread counter                                        |
| Heartbeat timeout             | Spring fires `SessionDisconnectEvent`; session cleaned from Redis automatically                                    |
| Redis unavailable             | Session registration fails → `MaxSessionsExceededException` possible; unread counts lost until Redis recovers      |
| Backend restart               | Redis sessions persist; clients reconnect via exponential backoff; `ReconnectionService` re-delivers unread counts |
| Duplicate message send        | No idempotency key currently — client should debounce; duplicate prevention is a future improvement                |

---

## 11. Client Responsibilities

| Responsibility              | Notes                                                                          |
| --------------------------- | ------------------------------------------------------------------------------ |
| Maintain heartbeat          | Send `heart-beat:4000,4000` on CONNECT; respond to server pings                |
| Reconnect on disconnect     | Use exponential backoff (e.g. 1s, 2s, 4s, 8s, max 30s)                         |
| Resubscribe after reconnect | Re-issue SUBSCRIBE frames for all active conversations                         |
| Fetch missed messages       | After reconnect, call `GET /api/conversations/{id}/messages` to fill gaps      |
| Optimistic UI update        | Show message immediately on send; confirm on receiving echo from server        |
| Sort by message id          | Use `id` ascending as canonical order within a conversation                    |
| Deduplicate messages        | Guard against receiving the same message twice (e.g. during reconnect overlap) |

---

## 12. Event-Driven Architecture

### Why events?

`SendMessageService` must not push to WebSocket directly — if the push fails, the DB transaction would need to roll back, which is wrong. The event decouples **persistence** from **delivery**.

### Transactional Boundary

```
┌─────────────────────────────────────────────────────┐
│  DB Transaction                                     │
│                                                     │
│  SendMessageService                                 │
│    1. validate content                              │
│    2. INSERT message (status=SENT)                  │
│    3. UPDATE conversation.lastMessageAt             │
│    4. applicationEventPublisher.publishEvent(...)   │
│                          ↓                          │
│              MessagePersistedEvent                  │
│              { message, recipientId }               │
│                                                     │
└─────────────────────────────────────────────────────┘
                           │
              transaction COMMITS here
                           │
                           ▼
┌─────────────────────────────────────────────────────┐
│  @TransactionalEventListener(AFTER_COMMIT)          │
│                                                     │
│  NotifyMessageService.onMessagePersisted()          │
│    ├─ sessionManager.isUserOnline(recipientId)      │
│    │                                                │
│    ├─ [online]  STOMP push /topic/chat.{convId}     │
│    │            UPDATE status = DELIVERED           │
│    │                                                │
│    └─ [offline] INCR chat:unread:{convId}:{recip}   │
└─────────────────────────────────────────────────────┘
```

### Key guarantee

`AFTER_COMMIT` means `NotifyMessageService` only runs **after the message is durably written to PostgreSQL**. If the transaction rolls back (e.g. DB error), the event is never fired — the recipient is never notified of a message that doesn't exist.

Conversely, if the STOMP push fails after commit, the message is still saved. The unread counter fallback ensures the recipient gets it on reconnect.

```
Persist fails  → event never fires → no ghost notification ✓
Push fails     → message already in DB → unread counter fallback ✓
```

---

## 13. REST API Reference

| Method | Path                                           | Purpose                               |
| ------ | ---------------------------------------------- | ------------------------------------- |
| `GET`  | `/api/conversations`                           | List conversations with unread counts |
| `POST` | `/api/conversations`                           | Create a new 1-to-1 conversation      |
| `GET`  | `/api/conversations/{id}/messages?page=&size=` | Paginated message history             |
