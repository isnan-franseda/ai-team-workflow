import { useState, useEffect, useCallback } from "react";
import { chatApiClient } from "../api/chatClient";
import type { ChatSession, ChatMessage } from "../types/chat";

const SESSION_STORAGE_KEY = "kp_chat_session_id";

interface StoredSession {
  sessionId: string;
  createdAt: string;
}

export function useSessionPersistence() {
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [isInitializing, setIsInitializing] = useState(true);

  // Load session from localStorage on mount
  useEffect(() => {
    const stored = localStorage.getItem(SESSION_STORAGE_KEY);
    if (stored) {
      try {
        const parsed: StoredSession = JSON.parse(stored);
        setSessionId(parsed.sessionId);
      } catch {
        // Invalid stored data, clear it
        localStorage.removeItem(SESSION_STORAGE_KEY);
      }
    }
    setIsInitializing(false);
  }, []);

  // Save session to localStorage whenever it changes
  const saveSession = useCallback((session: ChatSession) => {
    const stored: StoredSession = {
      sessionId: session.session_id,
      createdAt: session.created_at,
    };
    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(stored));
    setSessionId(session.session_id);
  }, []);

  const createSession = useCallback(async (): Promise<ChatSession> => {
    const session = await chatApiClient.createSession();
    saveSession(session);
    return session;
  }, [saveSession]);

  const clearSession = useCallback(() => {
    localStorage.removeItem(SESSION_STORAGE_KEY);
    setSessionId(null);
  }, []);

  const loadChatHistory = useCallback(async (): Promise<ChatMessage[]> => {
    if (!sessionId) return [];
    try {
      const history = await chatApiClient.getChatHistory(sessionId);
      return history;
    } catch (err) {
      console.error("Gagal memuat riwayat chat:", err);
      return [];
    }
  }, [sessionId]);

  return {
    sessionId,
    isInitializing,
    createSession,
    clearSession,
    loadChatHistory,
  };
}