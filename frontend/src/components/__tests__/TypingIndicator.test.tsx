import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { TypingIndicator } from "../TypingIndicator";

describe("TypingIndicator", () => {
  it("should not render when visible is false", () => {
    render(<TypingIndicator visible={false} />);
    expect(screen.queryByLabelText("Asisten sedang mengetik")).not.toBeInTheDocument();
  });

  it("should render three bouncing dots when visible is true", () => {
    render(<TypingIndicator visible={true} />);

    const indicator = screen.getByLabelText("Asisten sedang mengetik");
    expect(indicator).toBeInTheDocument();

    const dots = indicator.querySelectorAll("span");
    expect(dots.length).toBe(3);
  });

  it("should have aria-live attribute for accessibility", () => {
    render(<TypingIndicator visible={true} />);

    const indicator = screen.getByLabelText("Asisten sedang mengetik");
    expect(indicator).toHaveAttribute("aria-live", "polite");
  });
});