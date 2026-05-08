import React from "react";

interface NewChatButtonProps {
  onClick: () => void;
  disabled?: boolean;
}

export const NewChatButton: React.FC<NewChatButtonProps> = ({ onClick, disabled }) => {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className="flex items-center gap-2 px-4 py-2 bg-kp-green text-white rounded-lg hover:bg-kp-green-dark disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
      aria-label="Mulai obrolan baru"
    >
      <svg
        xmlns="http://www.w3.org/2000/svg"
        className="w-5 h-5"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
        aria-hidden="true"
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={2}
          d="M12 4v16m8-8H4"
        />
      </svg>
      <span className="font-medium">Obrolan Baru</span>
    </button>
  );
};