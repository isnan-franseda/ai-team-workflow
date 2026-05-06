import { useState } from "react";
import type { UploadResult } from "../types";

interface UseUploadOptions {
  uploadSingle: (file: File, docType: string, adminKey: string) => Promise<UploadResult>;
  uploadBatch: (files: File[], docTypes: string[], adminKey: string) => Promise<UploadResult[]>;
}

export const useUpload = (
  apiClient: UseUploadOptions,
  adminKey: string
) => {
  const [uploading, setUploading] = useState(false);
  const [results, setResults] = useState<UploadResult[]>([]);
  const [error, setError] = useState<string | null>(null);

  const uploadFiles = async (files: File[], docTypes: string[]) => {
    setUploading(true);
    setError(null);
    setResults([]);

    try {
      let uploadResults: UploadResult[];
      if (files.length === 1) {
        uploadResults = [await apiClient.uploadSingle(files[0], docTypes[0], adminKey)];
      } else {
        uploadResults = await apiClient.uploadBatch(files, docTypes, adminKey);
      }
      setResults(uploadResults);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Terjadi kesalahan saat mengunggah");
    } finally {
      setUploading(false);
    }
  };

  return { uploading, results, error, uploadFiles };
};