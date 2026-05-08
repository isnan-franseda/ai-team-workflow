import type { DocumentMetadata } from "../types";

interface DocumentListProps {
  documents: DocumentMetadata[];
  loading: boolean;
}

export const DocumentList = ({ documents, loading }: DocumentListProps) => {
  if (loading) {
    return (
      <div className="flex items-center justify-center p-8">
        <span className="text-gray-600">Memuat dokumen...</span>
      </div>
    );
  }

  if (documents.length === 0) {
    return (
      <div className="flex items-center justify-center p-8 text-gray-500">
        Tidak ada dokumen yang diunggah
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow overflow-hidden">
      <table className="w-full">
        <thead className="bg-gray-100 border-b">
          <tr>
            <th className="px-6 py-3 text-left text-sm font-semibold">Nama File</th>
            <th className="px-6 py-3 text-left text-sm font-semibold">Tipe Dokumen</th>
            <th className="px-6 py-3 text-left text-sm font-semibold">Chunk</th>
            <th className="px-6 py-3 text-left text-sm font-semibold">Tanggal Unggah</th>
          </tr>
        </thead>
        <tbody>
          {documents.map((doc) => (
            <tr key={doc.document_id} className="border-b hover:bg-gray-50">
              <td className="px-6 py-4 text-sm">{doc.filename}</td>
              <td className="px-6 py-4 text-sm">
                <span className="px-3 py-1 bg-kp-green-light text-kp-green-dark rounded-full text-xs">
                  {doc.doc_type}
                </span>
              </td>
              <td className="px-6 py-4 text-sm">{doc.chunk_count}</td>
              <td className="px-6 py-4 text-sm">
                {new Date(doc.created_at).toLocaleDateString("id-ID")}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};