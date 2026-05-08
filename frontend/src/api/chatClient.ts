import ky from "ky";
import type { ChatMessage, ChatSession, SendMessageResponse } from "../types/chat";
import { mapErrorToBahasa } from "../utils/errorMapper";

const apiUrl = import.meta.env.VITE_API_URL || "http://localhost:8080";

interface ChatHistoryResponse {
  session_id: string;
  messages: Array<{
    role: "user" | "assistant";
    content: string;
    created_at: string;
  }>;
}

interface CreateSessionResponse {
  session_id: string;
  created_at: string;
}

interface SendMessageError {
  response?: {
    status?: number;
    message?: string;
  };
  message?: string;
}

export const chatApiClient = {
  async createSession(): Promise<ChatSession> {
    try {
      const response = await ky.post(`${apiUrl}/api/v1/chat/session`, {
        headers: { "Content-Type": "application/json" },
      });
      const data = await response.json() as CreateSessionResponse;
      return {
        session_id: data.session_id,
        created_at: data.created_at,
      };
    } catch (err) {
      throw new Error(mapErrorToBahasa(err), { cause: err });
    }
  },

  async sendMessage(sessionId: string, message: string): Promise<SendMessageResponse> {
    try {
      const response = await ky.post(`${apiUrl}/api/v1/chat/send`, {
        timeout: 6000000,
        headers: { "Content-Type": "application/json" },
        json: { session_id: sessionId, message },
      },);
      return response.json() as Promise<SendMessageResponse>;
    } catch (err) {
      const chatErr = err as SendMessageError;
      // Check for rate limit
      if (chatErr?.response?.status === 429) {
        const rateLimitError = new Error("Anda telah mencapai batas pengiriman. Silakan tunggu sebentar.") as Error & { rateLimitExceeded: boolean };
        rateLimitError.rateLimitExceeded = true;
        throw rateLimitError;
      }
      throw new Error(mapErrorToBahasa(err), { cause: err });
    }
  },

  async getChatHistory(sessionId: string): Promise<ChatMessage[]> {
    try {
      const response = await ky.get(`${apiUrl}/api/v1/chat/history/${sessionId}`);
      const data = await response.json() as ChatHistoryResponse;
      return data.messages.map((msg) => ({
        id: `msg_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`,
        role: msg.role,
        content: msg.content,
        timestamp: new Date(msg.created_at),
      }));
    } catch (err) {
      throw new Error(mapErrorToBahasa(err), { cause: err });
    }
  },
};

export function isRateLimitError(error: unknown): error is Error & { rateLimitExceeded: boolean } {
  return error instanceof Error && "rateLimitExceeded" in error && (error as { rateLimitExceeded: boolean }).rateLimitExceeded === true;
}