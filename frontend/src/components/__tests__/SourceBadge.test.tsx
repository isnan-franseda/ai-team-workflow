import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { SourceBadge } from "../SourceBadge";
import type { SourceCitation } from "../../types/chat";

describe("SourceBadge", () => {
  const mockSource: SourceCitation = {
    source: "faq-pinjaman.pdf",
    type: "FAQ",
  };

  it("should render FAQ badge correctly", () => {
    render(<SourceBadge source={mockSource} />);

    const badge = screen.getByLabelText(/Sumber dari FAQ: faq-pinjaman.pdf/i);
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveTextContent("FAQ");
    expect(badge).toHaveTextContent("faq-pinjaman.pdf");
  });

  it("should render TOS badge correctly", () => {
    const tosSource: SourceCitation = {
      source: "syarat-ketentuan.pdf",
      type: "TOS",
    };
    render(<SourceBadge source={tosSource} />);

    const badge = screen.getByLabelText(/Sumber dari Syarat & Ketentuan: syarat-ketentuan.pdf/i);
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveTextContent("Syarat & Ketentuan");
  });

  it("should render BRAND badge correctly", () => {
    const brandSource: SourceCitation = {
      source: "brand-guidelines.pdf",
      type: "BRAND",
    };
    render(<SourceBadge source={brandSource} />);

    const badge = screen.getByLabelText(/Sumber dari Panduan Merek: brand-guidelines.pdf/i);
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveTextContent("Panduan Merek");
  });

  it("should render HOWTO badge correctly", () => {
    const howtoSource: SourceCitation = {
      source: "panduan-penggunaan.pdf",
      type: "HOWTO",
    };
    render(<SourceBadge source={howtoSource} />);

    const badge = screen.getByLabelText(/Sumber dari Cara Penggunaan: panduan-penggunaan.pdf/i);
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveTextContent("Cara Penggunaan");
  });
});