import ky from "ky";
import type { UploadResult, DocumentMetadata } from "../types";
import { mapErrorToBahasa } from "../utils/errorMapper";

const apiUrl = import.meta.env.VITE_API_URL || "http://localhost:8080";

export const apiClient = {
  async uploadSingle(file: File, docType: string, adminKey: string): Promise<UploadResult> {
    try {
      const formData = new FormData();
      formData.append("file", file);
      formData.append("doc_type", docType);
      const response = await ky.post(`${apiUrl}/admin/ingest`, {
        headers: { "X-Admin-Key": adminKey },
        body: formData,
      });
      return response.json() as Promise<UploadResult>;
    } catch (err) {
      throw new Error(mapErrorToBahasa(err));
    }
  },

  async uploadBatch(files: File[], docTypes: string[], adminKey: string): Promise<UploadResult[]> {
    try {
      const formData = new FormData();
      files.forEach((file, idx) => {
        formData.append("files", file);
        formData.append("doc_types", docTypes[idx]);
      });
      const response = await ky.post(`${apiUrl}/admin/ingest/batch`, {
        headers: { "X-Admin-Key": adminKey },
        body: formData,
      });
      return response.json() as Promise<UploadResult[]>;
    } catch (err) {
      throw new Error(mapErrorToBahasa(err));
    }
  },

  async getDocuments(adminKey: string, docType?: string): Promise<DocumentMetadata[]> {
    try {
      const searchParams = new URLSearchParams();
      if (docType) searchParams.set("doc_type", docType);
      const response = await ky.get(`${apiUrl}/admin/documents`, {
        headers: { "X-Admin-Key": adminKey },
        searchParams,
      });
      const data = await response.json();
      return Array.isArray(data) ? data : (data.documents || []);
    } catch (err) {
      throw new Error(mapErrorToBahasa(err));
    }
  },
};