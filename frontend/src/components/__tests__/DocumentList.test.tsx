import { render, screen } from "@testing-library/react";
import { DocumentList } from "../DocumentList";
import type { DocumentMetadata } from "../../types";

describe("DocumentList", () => {
  const mockDocuments: DocumentMetadata[] = [
    {
      document_id: "1",
      filename: "faq.pdf",
      doc_type: "FAQ",
      created_at: "2026-05-06T10:00:00Z",
      chunk_count: 42,
    },
    {
      document_id: "2",
      filename: "tos.pdf",
      doc_type: "TOS",
      created_at: "2026-05-05T09:00:00Z",
      chunk_count: 15,
    },
  ];

  it("should render document table with correct columns", () => {
    render(<DocumentList documents={mockDocuments} loading={false} />);

    expect(screen.getByText("Nama File")).toBeInTheDocument();
    expect(screen.getByText("Tipe Dokumen")).toBeInTheDocument();
    expect(screen.getByText("Chunk")).toBeInTheDocument();
    expect(screen.getByText("Tanggal Unggah")).toBeInTheDocument();
    expect(screen.getByText("faq.pdf")).toBeInTheDocument();
    expect(screen.getByText("tos.pdf")).toBeInTheDocument();
  });

  it("should render document rows with correct data", () => {
    render(<DocumentList documents={mockDocuments} loading={false} />);

    expect(screen.getByText("faq.pdf")).toBeInTheDocument();
    expect(screen.getByText("tos.pdf")).toBeInTheDocument();
    expect(screen.getAllByText("FAQ")).toHaveLength(1);
    expect(screen.getAllByText("TOS")).toHaveLength(1);
    expect(screen.getByText("42")).toBeInTheDocument();
    expect(screen.getByText("15")).toBeInTheDocument();
  });

  it("should show loading state", () => {
    render(<DocumentList documents={[]} loading={true} />);

    expect(screen.getByText("Memuat dokumen...")).toBeInTheDocument();
  });

  it("should show empty state when no documents", () => {
    render(<DocumentList documents={[]} loading={false} />);

    expect(screen.getByText("Tidak ada dokumen yang diunggah")).toBeInTheDocument();
  });

  it("should render doc_type badges correctly", () => {
    render(<DocumentList documents={mockDocuments} loading={false} />);

    const faqBadges = screen.getAllByText("FAQ");
    const tosBadges = screen.getAllByText("TOS");
    expect(faqBadges).toHaveLength(1);
    expect(tosBadges).toHaveLength(1);
  });
});
