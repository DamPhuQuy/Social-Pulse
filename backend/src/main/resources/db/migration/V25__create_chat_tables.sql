-- Create conversations table for 1-on-1 chat
CREATE TABLE conversations (
    id BIGSERIAL PRIMARY KEY,
    participant1_id BIGINT NOT NULL REFERENCES users(id),
    participant2_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_message_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_conversation_participants UNIQUE (participant1_id, participant2_id),
    CONSTRAINT chk_different_participants CHECK (participant1_id < participant2_id)
);

-- Create messages table for chat messages
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(id),
    sender_id BIGINT NOT NULL REFERENCES users(id),
    content VARCHAR(2000) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    status VARCHAR(10) NOT NULL DEFAULT 'SENT',
    CONSTRAINT chk_message_status CHECK (status IN ('SENT', 'DELIVERED', 'READ'))
);

-- Indexes for conversations
CREATE INDEX idx_conv_participant1 ON conversations(participant1_id);
CREATE INDEX idx_conv_participant2 ON conversations(participant2_id);
CREATE INDEX idx_conv_last_message ON conversations(last_message_at DESC);

-- Indexes for messages
CREATE INDEX idx_msg_conversation_ts ON messages(conversation_id, timestamp DESC);
CREATE INDEX idx_msg_sender ON messages(sender_id);
CREATE INDEX idx_msg_unread ON messages(conversation_id, status, sender_id)
    WHERE status != 'READ';
