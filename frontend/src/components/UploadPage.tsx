import { useState } from "react";
import type { UploadResult } from "../types";

interface UploadPageProps {
  adminKey: string;
  onLogout: () => void;
}

export const UploadPage = ({ onLogout }: UploadPageProps) => {
  const [_files, _setFiles] = useState<File[]>([]);
  const [_docTypes, _setDocTypes] = useState<string[]>([]);
  const [_uploading, _setUploading] = useState(false);
  const [_results, _setResults] = useState<UploadResult[]>([]);

  return (
    <div className="p-8">
      <h1 className="text-3xl font-bold mb-6">Unggah Dokumen</h1>

      <div className="grid grid-cols-2 gap-8">
        <div>Upload Zone</div>
        <div>Document List</div>
      </div>

      <button
        onClick={onLogout}
        className="mt-8 px-4 py-2 bg-red-500 text-white rounded hover:bg-opacity-90"
      >
        Keluar
      </button>
    </div>
  );
};