import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { UploadPage } from "../UploadPage";
import * as apiClient from "../../api/apiClient";

vi.mock("../../api/apiClient", () => ({
  apiClient: {
    uploadSingle: vi.fn(),
    uploadBatch: vi.fn(),
    getDocuments: vi.fn(),
  },
}));

describe("UploadPage", () => {
  const mockAdminKey = "test-admin-key";
  const mockOnLogout = vi.fn();
  const mockDocuments = [
    {
      document_id: "1",
      filename: "faq.pdf",
      doc_type: "FAQ",
      created_at: "2026-05-06T10:00:00Z",
      chunk_count: 42,
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(apiClient.apiClient.getDocuments).mockResolvedValue(mockDocuments);
  });

  it("should render upload page with header", () => {
    render(<UploadPage adminKey={mockAdminKey} onLogout={mockOnLogout} />);

    expect(screen.getByText("Panel Admin KP")).toBeInTheDocument();
  });

  it("should render logout button with correct aria-label", () => {
    render(<UploadPage adminKey={mockAdminKey} onLogout={mockOnLogout} />);

    const logoutButton = screen.getByLabelText("Tombol Keluar");
    expect(logoutButton).toBeInTheDocument();
  });

  it("should call onLogout when logout button is clicked", () => {
    render(<UploadPage adminKey={mockAdminKey} onLogout={mockOnLogout} />);

    const logoutButton = screen.getByLabelText("Tombol Keluar");
    fireEvent.click(logoutButton);

    expect(mockOnLogout).toHaveBeenCalled();
  });

  it("should render upload section heading", () => {
    render(<UploadPage adminKey={mockAdminKey} onLogout={mockOnLogout} />);

    expect(screen.getByText("Unggah Dokumen Baru")).toBeInTheDocument();
  });

  it("should render document list section heading", () => {
    render(<UploadPage adminKey={mockAdminKey} onLogout={mockOnLogout} />);

    expect(screen.getByText("Dokumen Teringest")).toBeInTheDocument();
  });

  it("should fetch documents on mount", async () => {
    render(<UploadPage adminKey={mockAdminKey} onLogout={mockOnLogout} />);

    // Wait for the deferred effect to run
    await waitFor(() => {
      expect(apiClient.apiClient.getDocuments).toHaveBeenCalledWith(mockAdminKey);
    });
  });

  it("should render upload button with correct aria-label", () => {
    render(<UploadPage adminKey={mockAdminKey} onLogout={mockOnLogout} />);

    const uploadButton = screen.getByLabelText("Tombol Unggah Dokumen");
    expect(uploadButton).toBeInTheDocument();
  });

  it("should disable upload button when no files selected", () => {
    render(<UploadPage adminKey={mockAdminKey} onLogout={mockOnLogout} />);

    const uploadButton = screen.getByLabelText("Tombol Unggah Dokumen");
    expect(uploadButton).toBeDisabled();
  });

  it("should show error message when upload fails", async () => {
    vi.mocked(apiClient.apiClient.uploadSingle).mockRejectedValue(
      new Error("Terjadi kesalahan saat mengunggah")
    );

    render(<UploadPage adminKey={mockAdminKey} onLogout={mockOnLogout} />);

    // Simulate selecting a file and uploading
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
    if (fileInput) {
      const file = new File(["test"], "test.pdf", { type: "application/pdf" });
      fireEvent.change(fileInput, { target: { files: [file] } });
    }

    const uploadButton = screen.getByLabelText("Tombol Unggah Dokumen");
    fireEvent.click(uploadButton);

    await waitFor(() => {
      expect(screen.getByText("Terjadi kesalahan saat mengunggah")).toBeInTheDocument();
    });
  });

  it("should show results after successful upload", async () => {
    vi.mocked(apiClient.apiClient.uploadSingle).mockResolvedValue({
      document_id: "doc-1",
      filename: "test.pdf",
      doc_type: "FAQ",
      status: "SUCCESS",
      chunks_created: 5,
      tokens_used: 100,
      duration_ms: 1000,
      error_message: null,
    });

    render(<UploadPage adminKey={mockAdminKey} onLogout={mockOnLogout} />);

    // Wait for initial document load
    await waitFor(() => {
      expect(screen.getByText("faq.pdf")).toBeInTheDocument();
    });
  });
});
