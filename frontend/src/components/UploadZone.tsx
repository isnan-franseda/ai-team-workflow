import { useState, type DragEvent, type ChangeEvent } from "react";

interface UploadZoneProps {
  onFilesSelected: (files: File[]) => void;
  onDocTypesChanged: (types: string[]) => void;
  files: File[];
  docTypes: string[];
  disabled?: boolean;
}

const ACCEPTED_TYPES = [
  "application/pdf",
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  "text/plain",
];
const ACCEPTED_EXTENSIONS = [".pdf", ".docx", ".txt"];

export const UploadZone = ({
  onFilesSelected,
  onDocTypesChanged,
  files,
  docTypes,
  disabled,
}: UploadZoneProps) => {
  const [dragActive, setDragActive] = useState(false);

  const handleDrag = (e: DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  };

  const handleDrop = (e: DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    const droppedFiles = Array.from(e.dataTransfer.files);
    const validFiles = droppedFiles.filter((file) => {
      const isValidType =
        ACCEPTED_TYPES.includes(file.type) ||
        ACCEPTED_EXTENSIONS.some((ext) => file.name.toLowerCase().endsWith(ext));
      return isValidType;
    });

    if (validFiles.length !== droppedFiles.length) {
      alert("Beberapa file tidak didukung. Gunakan PDF, DOCX, atau TXT.");
    }

    onFilesSelected([...files, ...validFiles]);
    onDocTypesChanged([...docTypes, ...validFiles.map(() => "FAQ")]);
  };

  const handleFileInput = (e: ChangeEvent<HTMLInputElement>) => {
    const selectedFiles = Array.from(e.target.files || []);
    onFilesSelected([...files, ...selectedFiles]);
    onDocTypesChanged([...docTypes, ...selectedFiles.map(() => "FAQ")]);
  };

  return (
    <div
      onDragEnter={handleDrag}
      onDragLeave={handleDrag}
      onDragOver={handleDrag}
      onDrop={handleDrop}
      className={`border-2 border-dashed rounded-lg p-8 text-center cursor-pointer transition ${
        dragActive ? "border-blue-600 bg-blue-50" : "border-gray-300"
      } ${disabled ? "opacity-50 cursor-not-allowed" : ""}`}
    >
      <input
        type="file"
        multiple
        onChange={handleFileInput}
        disabled={disabled}
        accept=".pdf,.docx,.txt"
        className="hidden"
        id="file-input"
        aria-label="Upload File Drop Zone"
      />
      <label htmlFor="file-input" className="cursor-pointer">
        <div className="text-lg font-semibold">Drag and drop files here</div>
        <div className="text-gray-500">atau klik untuk memilih file</div>
        <div className="text-sm text-gray-400 mt-2">PDF, DOCX, atau TXT</div>
      </label>

      {files.length > 0 && (
        <div className="mt-6 text-left">
          <h3 className="font-semibold mb-3">File Terpilih:</h3>
          {files.map((file, idx) => (
            <div
              key={idx}
              className="flex justify-between items-center mb-3 p-3 bg-gray-50 rounded"
            >
              <div>
                <div className="font-medium">{file.name}</div>
                <div className="text-sm text-gray-500">
                  {(file.size / 1024).toFixed(2)} KB
                </div>
              </div>
              <select
                value={docTypes[idx] || "FAQ"}
                onChange={(e) => {
                  const newTypes = [...docTypes];
                  newTypes[idx] = e.target.value;
                  onDocTypesChanged(newTypes);
                }}
                disabled={disabled}
                className="px-3 py-1 border rounded"
                aria-label={`Doc Type for ${file.name}`}
              >
                <option value="FAQ">FAQ</option>
                <option value="TOS">TOS</option>
                <option value="BRAND">BRAND</option>
                <option value="HOWTO">HOWTO</option>
              </select>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};