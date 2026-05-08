import React, { useRef, useEffect } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import type { ChatMessage } from "../types/chat";
import { TypingIndicator } from "./TypingIndicator";

interface MessageListProps {
  messages: ChatMessage[];
  typingIndicatorVisible: boolean;
}

export const MessageList: React.FC<MessageListProps> = ({ messages, typingIndicatorVisible }) => {
  const endRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (typeof endRef.current?.scrollIntoView === "function") {
      endRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [messages, typingIndicatorVisible]);

  return (
    <div
      className="flex-1 overflow-y-auto px-4 py-6 space-y-4"
      role="log"
      aria-label="Daftar pesan percakapan"
      aria-live="polite"
    >
      {messages.length === 0 && (
        <div className="flex flex-col items-center justify-center h-full text-center px-4">
          <div className="w-16 h-16 mb-4 bg-blue-100 rounded-full flex items-center justify-center">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="w-8 h-8 text-blue-600"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              aria-hidden="true"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574  3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"
              />
            </svg>
          </div>
          <h2 className="text-xl font-semibold text-gray-700 mb-2">Selamat Datang di Chatbot KP</h2>
          <p className="text-gray-500 max-w-sm">
            Tanyakan tentang produk pinjaman, syarat, atau hal lain yang ingin Anda ketahui. Saya siap membantu!
          </p>
        </div>
      )}

      {messages.map((message) => (
        <div
          key={message.id}
          className={`flex ${message.role === "user" ? "justify-end" : "justify-start"}`}
        >
          <div
            className={`max-w-xl px-4 py-3 rounded-2xl ${
              message.role === "user"
                ? "bg-blue-600 text-white rounded-tr-sm"
                : "bg-gray-100 text-gray-800 rounded-tl-sm"
            }`}
          >
            {message.role === "user" ? (
              <p className="whitespace-pre-wrap">{message.content}</p>
            ) : (
              <div className="prose prose-sm max-w-none">
                <ReactMarkdown
                  remarkPlugins={[remarkGfm]}
                  components={{
                    // Style tables
                    table: ({ children }) => (
                      <table className="min-w-full border border-gray-300 my-2 text-sm" style={{ borderCollapse: "collapse" }}>
                        {children}
                      </table>
                    ),
                    th: ({ children }) => (
                      <th className="border border-gray-300 px-3 py-1 bg-gray-50 font-medium text-left" style={{ borderCollapse: "collapse" }}>
                        {children}
                      </th>
                    ),
                    td: ({ children }) => (
                      <td className="border border-gray-300 px-3 py-1" style={{ borderCollapse: "collapse" }}>
                        {children}
                      </td>
                    ),
                    // Style blockquotes
                    blockquote: ({ children }) => (
                      <blockquote className="border-l-4 border-blue-400 pl-3 my-2 italic text-gray-600">
                        {children}
                      </blockquote>
                    ),
                    // Style lists
                    ul: ({ children }) => (
                      <ul className="list-disc list-inside my-1 space-y-1">{children}</ul>
                    ),
                    ol: ({ children }) => (
                      <ol className="list-decimal list-inside my-1 space-y-1">{children}</ol>
                    ),
                    // Style headings
                    h1: ({ children }) => <h1 className="text-lg font-bold mt-2 mb-1">{children}</h1>,
                    h2: ({ children }) => <h2 className="text-base font-bold mt-2 mb-1">{children}</h2>,
                    h3: ({ children }) => <h3 className="text-sm font-semibold mt-1 mb-1">{children}</h3>,
                    // Style bold and italic
                    strong: ({ children }) => <strong className="font-semibold">{children}</strong>,
                    em: ({ children }) => <em className="italic">{children}</em>,
                    // Style code
                    code: ({ children, className }) => {
                      const isInline = !className;
                      return isInline ? (
                        <code className="bg-gray-200 rounded px-1 py-0.5 text-xs font-mono">{children}</code>
                      ) : (
                        <code className="block bg-gray-200 rounded p-2 text-xs font-mono overflow-x-auto">{children}</code>
                      );
                    },
                    p: ({ children }) => <p className="my-1">{children}</p>,
                  }}
                >
                  {message.content}
                </ReactMarkdown>
              </div>
            )}
            <div
              className={`text-xs mt-2 ${
                message.role === "user" ? "text-blue-200" : "text-gray-400"
              }`}
            >
              {message.timestamp.toLocaleTimeString("id-ID", {
                hour: "2-digit",
                minute: "2-digit",
              })}
            </div>
          </div>
        </div>
      ))}

      {typingIndicatorVisible && (
        <div className="flex justify-start">
          <TypingIndicator visible={typingIndicatorVisible} />
        </div>
      )}

      <div ref={endRef} />
    </div>
  );
};