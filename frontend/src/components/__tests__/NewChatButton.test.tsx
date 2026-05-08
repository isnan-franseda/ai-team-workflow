import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { NewChatButton } from "../NewChatButton";

describe("NewChatButton", () => {
  it("should render button with correct label", () => {
    const onClick = vi.fn();
    render(<NewChatButton onClick={onClick} />);

    const button = screen.getByLabelText("Mulai obrolan baru");
    expect(button).toBeInTheDocument();
    expect(button).toHaveTextContent("Obrolan Baru");
  });

  it("should call onClick when clicked", () => {
    const onClick = vi.fn();
    render(<NewChatButton onClick={onClick} />);

    const button = screen.getByLabelText("Mulai obrolan baru");
    fireEvent.click(button);

    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it("should be disabled when disabled prop is true", () => {
    const onClick = vi.fn();
    render(<NewChatButton onClick={onClick} disabled />);

    const button = screen.getByLabelText("Mulai obrolan baru");
    expect(button).toBeDisabled();
  });

  it("should not call onClick when disabled", () => {
    const onClick = vi.fn();
    render(<NewChatButton onClick={onClick} disabled />);

    const button = screen.getByLabelText("Mulai obrolan baru");
    fireEvent.click(button);

    expect(onClick).not.toHaveBeenCalled();
  });

  it("should have correct styling classes", () => {
    const onClick = vi.fn();
    render(<NewChatButton onClick={onClick} />);

    const button = screen.getByLabelText("Mulai obrolan baru");
    expect(button).toHaveClass("flex", "items-center", "gap-2", "px-4", "py-2", "bg-blue-600", "text-white", "rounded-lg");
  });
});