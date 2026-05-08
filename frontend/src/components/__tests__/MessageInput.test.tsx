import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { MessageInput } from "../MessageInput";

describe("MessageInput", () => {
  const mockSendMessage = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should render textarea with placeholder", () => {
    render(<MessageInput onSendMessage={mockSendMessage} />);

    const textarea = screen.getByLabelText("Input pesan chat");
    expect(textarea).toBeInTheDocument();
    expect(textarea).toHaveAttribute("placeholder", "Ketik pesan Anda di sini...");
  });

  it("should render send button with aria-label", () => {
    render(<MessageInput onSendMessage={mockSendMessage} />);

    const sendButton = screen.getByLabelText("Kirim pesan");
    expect(sendButton).toBeInTheDocument();
  });

  it("should call onSendMessage when form is submitted", () => {
    render(<MessageInput onSendMessage={mockSendMessage} />);

    const textarea = screen.getByLabelText("Input pesan chat");
    fireEvent.change(textarea, { target: { value: "Test message" } });

    const form = textarea.closest("form");
    fireEvent.submit(form!);

    expect(mockSendMessage).toHaveBeenCalledWith("Test message");
  });

  it("should not submit empty message", () => {
    render(<MessageInput onSendMessage={mockSendMessage} />);

    const form = screen.getByLabelText("Input pesan chat").closest("form");
    fireEvent.submit(form!);

    expect(mockSendMessage).not.toHaveBeenCalled();
  });

  it("should disable textarea when disabled prop is true", () => {
    render(<MessageInput onSendMessage={mockSendMessage} disabled />);

    const textarea = screen.getByLabelText("Input pesan chat");
    expect(textarea).toBeDisabled();
  });

  it("should disable send button when disabled", () => {
    render(<MessageInput onSendMessage={mockSendMessage} disabled />);

    const sendButton = screen.getByLabelText("Kirim pesan");
    expect(sendButton).toBeDisabled();
  });

  it("should show rate limit warning when rateLimited is true", () => {
    render(<MessageInput onSendMessage={mockSendMessage} rateLimited />);

    const warning = screen.getByText(/Anda telah mencapai batas pengiriman/i);
    expect(warning).toBeInTheDocument();
    expect(warning).toHaveAttribute("role", "alert");
  });

  it("should disable input when rate limited", () => {
    render(<MessageInput onSendMessage={mockSendMessage} rateLimited />);

    const textarea = screen.getByLabelText("Input pesan chat");
    expect(textarea).toBeDisabled();
  });

  it("should clear input after submission", () => {
    render(<MessageInput onSendMessage={mockSendMessage} />);

    const textarea = screen.getByLabelText("Input pesan chat");
    fireEvent.change(textarea, { target: { value: "Test message" } });

    const form = textarea.closest("form");
    fireEvent.submit(form!);

    expect(textarea).toHaveValue("");
  });

  it("should submit on Enter key press", () => {
    render(<MessageInput onSendMessage={mockSendMessage} />);

    const textarea = screen.getByLabelText("Input pesan chat");
    fireEvent.change(textarea, { target: { value: "Test message" } });
    fireEvent.keyDown(textarea, { key: "Enter", keyCode: 13 });

    expect(mockSendMessage).toHaveBeenCalledWith("Test message");
  });

  it("should not submit on Shift+Enter", () => {
    render(<MessageInput onSendMessage={mockSendMessage} />);

    const textarea = screen.getByLabelText("Input pesan chat");
    fireEvent.change(textarea, { target: { value: "Test\nmessage" } });
    fireEvent.keyDown(textarea, { key: "Enter", shiftKey: true, keyCode: 13 });

    // Shift+Enter should not prevent default, so form submission should not happen
    // The textarea should have the newline in it
    expect(textarea).toHaveValue("Test\nmessage");
  });
});