import React from "react";
import type { SourceCitation } from "../types/chat";

interface SourceBadgeProps {
  source: SourceCitation;
}

const docTypeLabels: Record<string, string> = {
  FAQ: "FAQ",
  TOS: "Syarat & Ketentuan",
  BRAND: "Panduan Merek",
  HOWTO: "Cara Penggunaan",
  DOCUMENT: "Dokumen",
};

export const SourceBadge: React.FC<SourceBadgeProps> = ({ source }) => {
  const typeLabel = docTypeLabels[source.type] || source.type || "Dokumen";

  return (
    <span
      className="inline-flex items-center px-2 py-1 text-xs font-medium rounded bg-blue-50 text-blue-700 border border-blue-200"
      title={`Sumber: ${source.source}`}
      aria-label={`Sumber dari ${typeLabel}: ${source.source}`}
    >
      <span className="font-semibold mr-1">{typeLabel}</span>
      <span className="text-blue-500">|</span>
      <span className="ml-1 truncate max-w-32">{source.source}</span>
    </span>
  );
};