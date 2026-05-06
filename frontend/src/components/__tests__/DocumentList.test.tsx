import { render, screen } from "@testing-library/react";
import { DocumentList } from "../DocumentList";
import type { DocumentMetadata } from "../../types";

describe("DocumentList", () => {
  const mockDocuments: DocumentMetadata[] = [
    {
      id: "1",
      filename: "faq.pdf",
      doc_type: "FAQ",
      chunks_count: 42,
      uploaded_at: "2026-05-06T10:00:00Z",
      status: "ACTIVE",
    },
    {
      id: "2",
      filename: "tos.pdf",
      doc_type: "TOS",
      chunks_count: 15,
      uploaded_at: "2026-05-05T09:00:00Z",
      status: "ACTIVE",
    },
  ];

  it("should render document table with correct columns", () => {
    render(<DocumentList documents={mockDocuments} loading={false} />);

    expect(screen.getByText("Nama File")).toBeInTheDocument();
    expect(screen.getByText("Tipe Dokumen")).toBeInTheDocument();
    expect(screen.getByText("Chunk")).toBeInTheDocument();
    expect(screen.getByText("Tanggal Unggah")).toBeInTheDocument();
    expect(screen.getByText("faq.pdf")).toBeInTheDocument();
  });

  it("should show loading state", () => {
    render(<DocumentList documents={[]} loading={true} />);

    expect(screen.getByText("Memuat dokumen...")).toBeInTheDocument();
  });

  it("should show empty state when no documents", () => {
    render(<DocumentList documents={[]} loading={false} />);

    expect(screen.getByText("Tidak ada dokumen yang diunggah")).toBeInTheDocument();
  });
});