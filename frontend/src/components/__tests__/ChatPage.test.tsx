import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { BrowserRouter } from "react-router-dom";
import { ChatPage } from "../ChatPage";
import * as chatClient from "../../api/chatClient";

vi.mock("../../api/chatClient", () => ({
  chatApiClient: {
    createSession: vi.fn(),
    sendMessage: vi.fn(),
    getChatHistory: vi.fn(),
  },
  isRateLimitError: vi.fn(),
}));

const renderWithRouter = (ui: React.ReactElement) => {
  return render(<BrowserRouter>{ui}</BrowserRouter>);
};

// Helper to mock localStorage for session persistence tests
const mockLocalStorageForSession = (sessionId: string | null) => {
  const store = sessionId ? JSON.stringify({ sessionId, createdAt: new Date().toISOString() }) : null;
  Object.defineProperty(window, "localStorage", {
    value: {
      getItem: vi.fn(() => store),
      setItem: vi.fn(),
      removeItem: vi.fn(),
      clear: vi.fn(),
    },
    writable: true,
  });
};

describe("ChatPage", () => {
  const mockCreateSession = vi.mocked(chatClient.chatApiClient.createSession);
  const mockSendMessage = vi.mocked(chatClient.chatApiClient.sendMessage);
  const mockGetChatHistory = vi.mocked(chatClient.chatApiClient.getChatHistory);

  beforeEach(() => {
    vi.clearAllMocks();
    // Default: no stored session, so createSession will be called
    mockLocalStorageForSession(null);
    mockCreateSession.mockResolvedValue({
      session_id: "test-session-123",
      created_at: new Date().toISOString(),
    });
    mockGetChatHistory.mockResolvedValue([]);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("should render chat page title and subtitle", async () => {
    renderWithRouter(<ChatPage />);

    await waitFor(() => {
      expect(screen.getByText("Chat dengan Kredit Pintar")).toBeInTheDocument();
    });
    expect(screen.getByText("Assisten virtual 24/7 untuk pertanyaan seputar kredit")).toBeInTheDocument();
  });

  it("should render New Chat button with correct aria-label", async () => {
    renderWithRouter(<ChatPage />);

    await waitFor(() => {
      expect(screen.getByLabelText("Mulai obrolan baru")).toBeInTheDocument();
    });
  });

  it("should render welcome message when no messages", async () => {
    renderWithRouter(<ChatPage />);

    await waitFor(() => {
      expect(screen.getByText("Selamat Datang di Chatbot KP")).toBeInTheDocument();
    });
  });

  it("should render message input", async () => {
    renderWithRouter(<ChatPage />);

    await waitFor(() => {
      expect(screen.getByLabelText("Input pesan chat")).toBeInTheDocument();
    });
  });

  it("should render send button", async () => {
    renderWithRouter(<ChatPage />);

    await waitFor(() => {
      expect(screen.getByLabelText("Kirim pesan")).toBeInTheDocument();
    });
  });

  it("should create session on mount when no stored session", async () => {
    renderWithRouter(<ChatPage />);

    await waitFor(() => {
      expect(mockCreateSession).toHaveBeenCalled();
    });
  });

  it("should send message when user submits", async () => {
    mockSendMessage.mockResolvedValue({
      session_id: "test-session-123",
      response: "Jawaban dari chatbot",
      citations: [],
      response_time_ms: 100,
      timestamp: new Date().toISOString(),
    });

    renderWithRouter(<ChatPage />);

    await waitFor(() => {
      expect(screen.getByLabelText("Input pesan chat")).toBeInTheDocument();
    });

    const textarea = screen.getByLabelText("Input pesan chat");
    fireEvent.change(textarea, { target: { value: "Apa itu Kredit Pintar?" } });

    const form = textarea.closest("form") as HTMLFormElement;
    fireEvent.submit(form);

    await waitFor(() => {
      expect(mockSendMessage).toHaveBeenCalledWith("test-session-123", "Apa itu Kredit Pintar?");
    });
  });

  it("should show error when send message fails", async () => {
    mockSendMessage.mockRejectedValue(new Error("Gagal mengirim pesan"));

    renderWithRouter(<ChatPage />);

    await waitFor(() => {
      expect(screen.getByLabelText("Input pesan chat")).toBeInTheDocument();
    });

    const textarea = screen.getByLabelText("Input pesan chat");
    fireEvent.change(textarea, { target: { value: "Test" } });

    const form = textarea.closest("form") as HTMLFormElement;
    fireEvent.submit(form);

    await waitFor(() => {
      expect(screen.getByRole("alert")).toBeInTheDocument();
    });
  });

  it("should show rate limit warning when rate limited", async () => {
    const rateLimitError = new Error("Rate limited") as Error & { rateLimitExceeded: boolean };
    rateLimitError.rateLimitExceeded = true;
    mockSendMessage.mockRejectedValue(rateLimitError);
    vi.mocked(chatClient.isRateLimitError).mockReturnValue(true);

    renderWithRouter(<ChatPage />);

    await waitFor(() => {
      expect(screen.getByLabelText("Input pesan chat")).toBeInTheDocument();
    });

    const textarea = screen.getByLabelText("Input pesan chat");
    fireEvent.change(textarea, { target: { value: "Test" } });

    const form = textarea.closest("form") as HTMLFormElement;
    fireEvent.submit(form);

    await waitFor(() => {
      expect(screen.getAllByText(/Anda telah mencapai batas pengiriman/i)).toHaveLength(2);
    });
  });

  it("should render typing indicator while sending", async () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let resolvePromise: any;
    mockSendMessage.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolvePromise = resolve;
        })
    );

    renderWithRouter(<ChatPage />);

    await waitFor(() => {
      expect(screen.getByLabelText("Input pesan chat")).toBeInTheDocument();
    });

    const textarea = screen.getByLabelText("Input pesan chat");
    fireEvent.change(textarea, { target: { value: "Test" } });

    const form = textarea.closest("form") as HTMLFormElement;
    fireEvent.submit(form);

    // Immediately after submit, typing indicator should be visible
    await waitFor(() => {
      expect(screen.getByLabelText("Asisten sedang mengetik")).toBeInTheDocument();
    });

    // Resolve the promise
    resolvePromise!({
      message_id: "msg-1",
      content: "Response",
      sources: [],
    });
  });

  it("should use stored session when localStorage has session_id", async () => {
    // Mock localStorage to return a stored session
    mockLocalStorageForSession("stored-session-456");

    renderWithRouter(<ChatPage />);

    // createSession should NOT be called when there's a stored session
    await waitFor(() => {
      expect(mockCreateSession).not.toHaveBeenCalled();
    });

    // getChatHistory should be called with the stored session ID
    await waitFor(() => {
      expect(mockGetChatHistory).toHaveBeenCalledWith("stored-session-456");
    });
  });
});