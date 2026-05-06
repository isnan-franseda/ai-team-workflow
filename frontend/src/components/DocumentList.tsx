import type { DocumentMetadata } from "../types";

interface DocumentListProps {
  documents: DocumentMetadata[];
  loading: boolean;
}

export const DocumentList = ({ documents, loading }: DocumentListProps) => {
  return (
    <div>
      <h2 className="text-xl font-bold mb-4">Dokumen Teringest</h2>
      {loading ? (
        <p>Memuat...</p>
      ) : (
        <table className="w-full border">
          <thead>
            <tr className="bg-gray-200">
              <th className="p-2">Nama File</th>
              <th className="p-2">Tipe</th>
              <th className="p-2">Tanggal Unggah</th>
            </tr>
          </thead>
          <tbody>
            {documents.map((doc) => (
              <tr key={doc.id} className="border-b">
                <td className="p-2">{doc.filename}</td>
                <td className="p-2">{doc.doc_type}</td>
                <td className="p-2">{doc.uploaded_at}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
};