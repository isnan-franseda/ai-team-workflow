import { useState } from "react";
import { UploadZone } from "./UploadZone";
import { DocumentList } from "./DocumentList";
import { mockApiClient } from "../api/mockClient";
import type { UploadResult, DocumentMetadata } from "../types";

interface UploadPageProps {
  adminKey: string;
  onLogout: () => void;
}

export const UploadPage = ({ adminKey, onLogout }: UploadPageProps) => {
  const [files, setFiles] = useState<File[]>([]);
  const [docTypes, setDocTypes] = useState<string[]>([]);
  const [documents, setDocuments] = useState<DocumentMetadata[]>([]);
  const [loadingDocs, setLoadingDocs] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [results, setResults] = useState<UploadResult[]>([]);
  const [error, setError] = useState<string | null>(null);

  const loadDocuments = async () => {
    setLoadingDocs(true);
    try {
      const docList = await mockApiClient.getDocuments(adminKey);
      setDocuments(docList.documents);
    } catch (err) {
      console.error("Failed to load documents", err);
    } finally {
      setLoadingDocs(false);
    }
  };

  const handleUpload = async () => {
    if (files.length === 0 || docTypes.length === 0) {
      alert("Pilih file dan tentukan tipe dokumen");
      return;
    }

    setUploading(true);
    setError(null);
    setResults([]);

    try {
      let uploadResults: UploadResult[];
      if (files.length === 1) {
        uploadResults = [await mockApiClient.uploadSingle(files[0], docTypes[0], adminKey)];
      } else {
        uploadResults = await mockApiClient.uploadBatch(files, docTypes, adminKey);
      }
      setResults(uploadResults);
      setFiles([]);
      setDocTypes([]);
      await loadDocuments();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Terjadi kesalahan saat mengunggah");
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-8 py-6 flex justify-between items-center">
          <h1 className="text-3xl font-bold">Panel Admin KP</h1>
          <button
            onClick={onLogout}
            className="px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600"
            aria-label="Tombol Keluar"
          >
            Keluar
          </button>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-8 py-8">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          <div>
            <h2 className="text-2xl font-bold mb-6">Unggah Dokumen Baru</h2>
            <UploadZone
              onFilesSelected={setFiles}
              onDocTypesChanged={setDocTypes}
              files={files}
              docTypes={docTypes}
              disabled={uploading}
            />

            {error && (
              <div className="mt-4 bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
                {error}
              </div>
            )}

            {results.length > 0 && (
              <div className="mt-6 space-y-4">
                <h3 className="font-semibold">Hasil Unggahan:</h3>
                {results.map((result, idx) => (
                  <div
                    key={idx}
                    className={`p-4 rounded border ${
                      result.status === "SUCCESS"
                        ? "bg-green-50 border-green-200"
                        : result.status === "SKIPPED"
                        ? "bg-yellow-50 border-yellow-200"
                        : "bg-red-50 border-red-200"
                    }`}
                  >
                    <div className="font-medium">{result.filename}</div>
                    <div className="text-sm text-gray-600">
                      Status: {result.status === "SUCCESS" ? "Berhasil" : result.status}
                    </div>
                    {result.chunks_created && (
                      <div className="text-sm text-gray-600">
                        Chunk: {result.chunks_created}, Token: {result.tokens_used}
                      </div>
                    )}
                    {result.error_message && (
                      <div className="text-sm text-red-600 mt-2">{result.error_message}</div>
                    )}
                  </div>
                ))}
              </div>
            )}

            <button
              onClick={handleUpload}
              disabled={files.length === 0 || uploading}
              className="mt-6 w-full bg-blue-600 text-white py-3 rounded hover:bg-blue-700 disabled:opacity-50 font-semibold"
              aria-label="Tombol Unggah Dokumen"
            >
              {uploading ? "Mengunggah..." : "Unggah Dokumen"}
            </button>
          </div>

          <div>
            <h2 className="text-2xl font-bold mb-6">Dokumen Teringest</h2>
            <DocumentList documents={documents} loading={loadingDocs} />
          </div>
        </div>
      </div>
    </div>
  );
};