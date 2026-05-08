export interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  sources?: SourceCitation[];
  timestamp: Date;
}

export interface SourceCitation {
  source: string;
  type: string;
}

export interface ChatSession {
  session_id: string;
  created_at: string;
}

export interface SendMessageRequest {
  session_id: string;
  message: string;
}

export interface SendMessageResponse {
  session_id: string;
  response: string;
  citations: Array<{
    source: string;
    type: string;
  }>;
  response_time_ms: number;
  timestamp: string;
}

export interface RateLimitError {
  response: {
    status: 429;
    message: string;
  };
}