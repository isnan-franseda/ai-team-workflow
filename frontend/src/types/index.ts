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
  id: string;
  filename: string;
  doc_type: string;
  chunks_count: number;
  uploaded_at: string;
  status: string;
}

export interface DocumentList {
  documents: DocumentMetadata[];
  total: number;
}

export interface AdminSession {
  adminKey: string;
  isAuthenticated: boolean;
}