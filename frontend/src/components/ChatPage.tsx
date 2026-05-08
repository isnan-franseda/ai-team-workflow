import { useState, useEffect, useCallback } from "react";
import { MessageList } from "./MessageList";
import { MessageInput } from "./MessageInput";
import { NewChatButton } from "./NewChatButton";
import { chatApiClient, isRateLimitError } from "../api/chatClient";
import { useSessionPersistence } from "../hooks/useSessionPersistence";
import type { ChatMessage } from "../types/chat";

const generateMessageId = () => `msg_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`;

export const ChatPage: React.FC = () => {
  const { sessionId, isInitializing, createSession, clearSession, loadChatHistory } = useSessionPersistence();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [isTyping, setIsTyping] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isRateLimited, setIsRateLimited] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Initialize session on mount if no session exists
  useEffect(() => {
    if (!isInitializing && !sessionId) {
      createSession();
    }
  }, [isInitializing, sessionId, createSession]);

  // Load chat history when session exists
  useEffect(() => {
    if (!isInitializing && sessionId) {
      loadChatHistory().then((history) => {
        setMessages(history);
      });
    }
  }, [isInitializing, sessionId, loadChatHistory]);

  const handleSendMessage = useCallback(
    async (content: string) => {
      if (!sessionId) return;

      const userMessage: ChatMessage = {
        id: generateMessageId(),
        role: "user",
        content,
        timestamp: new Date(),
      };

      setMessages((prev) => [...prev, userMessage]);
      setIsTyping(true);
      setError(null);
      setIsRateLimited(false);

      try {
        const response = await chatApiClient.sendMessage(sessionId, content);

        const assistantMessage: ChatMessage = {
          id: generateMessageId(),
          role: "assistant",
          content: response.response,
          sources: response.citations?.map((c) => ({
            source: c.source,
            type: "document" as const,
          })),
          timestamp: new Date(),
        };

        setMessages((prev) => [...prev, assistantMessage]);
      } catch (err) {
        if (isRateLimitError(err)) {
          setIsRateLimited(true);
          setError("Anda telah mencapai batas pengiriman. Silakan tunggu sebentar.");
        } else {
          const errorMessage = err instanceof Error ? err.message : "Gagal mengirim pesan.";
          setError(errorMessage);
        }
      } finally {
        setIsTyping(false);
      }
    },
    [sessionId]
  );

  const handleNewChat = useCallback(async () => {
    setIsLoading(true);
    try {
      // Clear old session and create new one
      clearSession();
      await createSession();
      setMessages([]);
      setError(null);
      setIsRateLimited(false);
    } finally {
      setIsLoading(false);
    }
  }, [clearSession, createSession]);

  return (
    <div className="flex flex-col h-[calc(100vh-4rem)] bg-gray-50">
      {/* Page Header */}
      <div className="bg-white border-b border-gray-200 px-4 py-4">
        <div className="max-w-3xl mx-auto flex items-center justify-between">
          <div>
            <h2 className="text-xl font-semibold text-gray-900">Chat dengan Kredit Pintar</h2>
            <p className="text-sm text-gray-500">Assisten virtual 24/7 untuk pertanyaan seputar kredit</p>
          </div>
          <NewChatButton onClick={handleNewChat} disabled={isLoading} />
        </div>
      </div>

      {/* Error Banner */}
      {error && (
        <div
          className="mx-4 mt-4 px-4 py-3 bg-red-50 border border-red-200 rounded-lg text-red-700"
          role="alert"
          aria-live="assertive"
        >
          {error}
        </div>
      )}

      {/* Message List */}
      <MessageList
        messages={messages}
        typingIndicatorVisible={isTyping}
      />

      {/* Message Input */}
      <MessageInput
        onSendMessage={handleSendMessage}
        disabled={!sessionId || isLoading}
        rateLimited={isRateLimited}
      />
    </div>
  );
};