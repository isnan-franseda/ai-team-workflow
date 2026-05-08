import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { MessageList } from "../MessageList";
import type { ChatMessage } from "../../types/chat";

describe("MessageList", () => {
  const mockMessages: ChatMessage[] = [
    {
      id: "msg-1",
      role: "user",
      content: "Apa itu Kredit Pintar?",
      timestamp: new Date("2026-05-07T10:00:00"),
    },
    {
      id: "msg-2",
      role: "assistant",
      content: "Kredit Pintar adalah layanan pinjaman online...",
      sources: [
        { source: "faq.pdf", type: "FAQ" },
      ],
      timestamp: new Date("2026-05-07T10:01:00"),
    },
  ];

  it("should render welcome message when no messages", () => {
    render(<MessageList messages={[]} typingIndicatorVisible={false} />);

    expect(screen.getByText("Selamat Datang di Chatbot KP")).toBeInTheDocument();
    expect(screen.getByText(/Tanyakan tentang produk pinjaman/i)).toBeInTheDocument();
  });

  it("should render messages correctly", () => {
    render(<MessageList messages={mockMessages} typingIndicatorVisible={false} />);

    expect(screen.getByText("Apa itu Kredit Pintar?")).toBeInTheDocument();
    expect(screen.getByText("Kredit Pintar adalah layanan pinjaman online...")).toBeInTheDocument();
  });

  it("should render user message on the right side", () => {
    render(<MessageList messages={mockMessages} typingIndicatorVisible={false} />);

    // User message is in a flex justify-end container
    const userMessage = screen.getByText("Apa itu Kredit Pintar?").closest(".max-w-xl");
    // The immediate parent is the flex container with justify-end
    expect(userMessage?.parentElement).toHaveClass("justify-end");
  });

  it("should render assistant message on the left side", () => {
    render(<MessageList messages={mockMessages} typingIndicatorVisible={false} />);

    // Assistant message is in a flex justify-start container
    const assistantMessage = screen.getByText("Kredit Pintar adalah layanan pinjaman online...").closest(".max-w-xl");
    // The immediate parent is the flex container with justify-start
    expect(assistantMessage?.parentElement).toHaveClass("justify-start");
  });

  it("should render source badges for assistant messages", () => {
    render(<MessageList messages={mockMessages} typingIndicatorVisible={false} />);

    const badge = screen.getByLabelText(/Sumber dari FAQ: faq.pdf/i);
    expect(badge).toBeInTheDocument();
  });

  it("should have role='log' for accessibility", () => {
    render(<MessageList messages={mockMessages} typingIndicatorVisible={false} />);

    const messageList = screen.getByRole("log");
    expect(messageList).toBeInTheDocument();
  });

  it("should have aria-live for accessibility", () => {
    render(<MessageList messages={mockMessages} typingIndicatorVisible={false} />);

    const messageList = screen.getByRole("log");
    expect(messageList).toHaveAttribute("aria-live", "polite");
  });

  it("should render timestamp for messages", () => {
    render(<MessageList messages={mockMessages} typingIndicatorVisible={false} />);

    // Check that timestamps are rendered for both messages
    // Use regex to match time format (colons or dots may vary by environment)
    const timestampRegex = /10:00|10\.00/;
    const timestampRegex2 = /10:01|10\.01/;
    expect(screen.getByText(timestampRegex)).toBeInTheDocument();
    expect(screen.getByText(timestampRegex2)).toBeInTheDocument();
  });

  it("should not render typing indicator when visible is false", () => {
    render(<MessageList messages={mockMessages} typingIndicatorVisible={false} />);

    expect(screen.queryByLabelText("Asisten sedang mengetik")).not.toBeInTheDocument();
  });

  it("should render typing indicator when visible is true", () => {
    render(<MessageList messages={mockMessages} typingIndicatorVisible={true} />);

    expect(screen.getByLabelText("Asisten sedang mengetik")).toBeInTheDocument();
  });
});