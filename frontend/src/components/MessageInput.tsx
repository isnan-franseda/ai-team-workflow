import React, { useState, useRef, type FormEvent } from "react";

interface MessageInputProps {
  onSendMessage: (message: string) => void;
  disabled?: boolean;
  rateLimited?: boolean;
}

export const MessageInput: React.FC<MessageInputProps> = ({ onSendMessage, disabled, rateLimited }) => {
  const [inputValue, setInputValue] = useState("");
  const inputRef = useRef<HTMLTextAreaElement>(null);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (disabled || rateLimited || !inputValue.trim()) return;

    onSendMessage(inputValue.trim());
    setInputValue("");

    // Refocus input after sending
    inputRef.current?.focus();
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    // Submit on Enter (without Shift)
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e);
    }
  };

  const isDisabled = disabled || rateLimited;

  return (
    <form
      onSubmit={handleSubmit}
      className="border-t border-gray-200 bg-white px-4 py-3"
    >
      {rateLimited && (
        <div
          className="mb-2 px-3 py-2 bg-amber-50 border border-amber-200 rounded-lg text-amber-700 text-sm"
          role="alert"
          aria-live="assertive"
        >
          <span className="font-medium">Peringatan:</span> Anda telah mencapai batas pengiriman. Silakan tunggu sebentar.
        </div>
      )}

      <div className="flex items-start gap-3">
        <div className="flex-1 relative">
          <textarea
            ref={inputRef}
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Ketik pesan Anda di sini..."
            disabled={isDisabled}
            rows={1}
            className="w-full px-4 py-3 border border-gray-300 rounded-xl resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:bg-gray-100 disabled:cursor-not-allowed"
            aria-label="Input pesan chat"
            aria-describedby={rateLimited ? "rate-limit-warning" : undefined}
            style={{ minHeight: "48px", maxHeight: "120px" }}
          />
        </div>

        <button
          type="submit"
          disabled={isDisabled || !inputValue.trim()}
          className="shrink-0 p-3 bg-blue-600 text-white rounded-xl hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          aria-label="Kirim pesan"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            className="w-7 h-7"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            aria-hidden="true"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"
            />
          </svg>
        </button>
      </div>

      <p className="mt-1 text-xs text-gray-400" aria-hidden="true">
        Tekan Enter untuk mengirim, Shift+Enter untuk baris baru
      </p>
    </form>
  );
};