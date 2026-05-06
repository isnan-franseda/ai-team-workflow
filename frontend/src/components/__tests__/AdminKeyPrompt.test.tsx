import { render, screen, fireEvent } from "@testing-library/react";
import { AdminKeyPrompt } from "../AdminKeyPrompt";

describe("AdminKeyPrompt", () => {
  it("should store admin key in sessionStorage on submit", () => {
    const mockOnAuthenticated = vi.fn();
    render(<AdminKeyPrompt onAuthenticated={mockOnAuthenticated} />);

    const input = screen.getByLabelText("Admin Key Input");
    const button = screen.getByRole("button", { name: "Masuk" });

    fireEvent.change(input, { target: { value: "test-key-123" } });
    fireEvent.click(button);

    expect(sessionStorage.setItem).toHaveBeenCalledWith("admin_key", "test-key-123");
    expect(mockOnAuthenticated).toHaveBeenCalledWith("test-key-123");
  });

  it("should restore key from sessionStorage on mount", () => {
    vi.mocked(sessionStorage.getItem).mockReturnValue("existing-key");
    const mockOnAuthenticated = vi.fn();

    render(<AdminKeyPrompt onAuthenticated={mockOnAuthenticated} />);

    expect(mockOnAuthenticated).toHaveBeenCalledWith("existing-key");
  });

  it("should show error when key is empty", () => {
    const mockOnAuthenticated = vi.fn();
    render(<AdminKeyPrompt onAuthenticated={mockOnAuthenticated} />);

    const button = screen.getByRole("button", { name: "Masuk" });
    fireEvent.click(button);

    expect(screen.getByText("Kunci admin diperlukan")).toBeInTheDocument();
    expect(mockOnAuthenticated).not.toHaveBeenCalled();
  });
});