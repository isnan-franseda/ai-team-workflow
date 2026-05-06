export const mapErrorToBahasa = (error: unknown): string => {
  if (typeof error === "string") {
    return mapHttpErrorToBahasa(error);
  }

  const err = error as { response?: { status?: number }; message?: string };

  if (err?.response?.status === 400) {
    return "Format file tidak didukung. Gunakan PDF, DOCX, atau TXT.";
  }
  if (err?.response?.status === 401) {
    return "Kunci admin tidak valid. Silakan masuk kembali.";
  }
  if (err?.response?.status === 409) {
    return "File ini sudah pernah diunggah. Tidak ada perubahan yang dilakukan.";
  }
  if (err?.response?.status === 429) {
    return "Terlalu banyak permintaan. Silakan tunggu sebentar.";
  }
  if (err?.response?.status === 500) {
    return "Terjadi kesalahan saat mengunggah. Silakan coba lagi atau hubungi tim engineering.";
  }

  if (err?.message) {
    return mapHttpErrorToBahasa(err.message);
  }

  return "Terjadi kesalahan yang tidak diketahui.";
};

const mapHttpErrorToBahasa = (message: string): string => {
  const mappings: Record<string, string> = {
    "invalid file": "Format file tidak didukung",
    "unauthorized": "Kunci admin tidak valid",
    "timeout": "Koneksi waktu habis. Silakan coba lagi.",
    "network error": "Kesalahan jaringan. Periksa koneksi Anda.",
  };

  for (const [en, id] of Object.entries(mappings)) {
    if (message.toLowerCase().includes(en)) {
      return id;
    }
  }

  return "Terjadi kesalahan. Silakan coba lagi.";
};