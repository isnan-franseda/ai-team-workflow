import type { UploadResult, DocumentMetadata } from "../types";

export const mockApiClient = {
  async uploadSingle(file: File, docType: string, adminKey: string): Promise<UploadResult> {
    // adminKey would be used for authentication in real implementation
    void adminKey;
    // Simulate network delay
    await new Promise(resolve => setTimeout(resolve, 1000 + Math.random() * 1000));

    return {
      document_id: `doc-${Math.random().toString(36).substr(2, 9)}`,
      filename: file.name,
      doc_type: docType as "FAQ" | "TOS" | "BRAND" | "HOWTO",
      status: "SUCCESS",
      chunks_created: Math.floor(Math.random() * 20) + 5,
      tokens_used: Math.floor(Math.random() * 500) + 100,
      duration_ms: Math.floor(Math.random() * 3000) + 500,
      error_message: null,
    };
  },

  async uploadBatch(files: File[], docTypes: string[], adminKey: string): Promise<UploadResult[]> {
    // adminKey would be used for authentication in real implementation
    void adminKey;
    // Simulate network delay
    await new Promise(resolve => setTimeout(resolve, 2000 + Math.random() * 2000));

    return files.map((file, idx) => ({
      document_id: `doc-${Math.random().toString(36).substr(2, 9)}`,
      filename: file.name,
      doc_type: docTypes[idx] as "FAQ" | "TOS" | "BRAND" | "HOWTO",
      status: Math.random() > 0.1 ? "SUCCESS" : "SKIPPED",
      chunks_created: Math.floor(Math.random() * 20) + 5,
      tokens_used: Math.floor(Math.random() * 500) + 100,
      duration_ms: Math.floor(Math.random() * 3000) + 500,
      error_message: null,
    }));
  },

  async getDocuments(adminKey: string, docType?: string): Promise<DocumentMetadata[]> {
    // adminKey and docType would be used in real implementation
    void adminKey;
    void docType;
    // Simulate network delay
    await new Promise(resolve => setTimeout(resolve, 500));

    return [
      {
        document_id: "doc-1",
        filename: "faq-2026-05.pdf",
        doc_type: "FAQ",
        created_at: new Date().toISOString(),
        chunk_count: 42,
      },
    ];
  },
};
