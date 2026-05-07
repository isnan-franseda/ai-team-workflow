export interface UploadResult {
  document_id: string;
  filename: string;
  doc_type: "FAQ" | "TOS" | "BRAND" | "HOWTO";
  status: "SUCCESS" | "SKIPPED" | "FAILED";
  chunks_created: number | null;
  tokens_used: number | null;
  duration_ms: number;
  error_message: string | null;
}

export interface DocumentMetadata {
  document_id: string;
  filename: string;
  doc_type: string;
  created_at: string;
  chunk_count: number;
}

export interface AdminSession {
  adminKey: string;
  isAuthenticated: boolean;
}