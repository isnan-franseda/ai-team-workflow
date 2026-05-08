import { useState, useEffect, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";

interface AdminRouteGuardProps {
  children: React.ReactNode;
}

export const AdminRouteGuard = ({ children }: AdminRouteGuardProps) => {
  const [adminKey, setAdminKey] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const stored = sessionStorage.getItem("admin_key");
    setAdminKey(stored);
    setIsLoading(false);
  }, []);

  const handleAuthenticated = (key: string) => {
    setAdminKey(key);
    sessionStorage.setItem("admin_key", key);
  };

  const handleLogout = () => {
    sessionStorage.removeItem("admin_key");
    setAdminKey(null);
    navigate("/admin");
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-gray-500">Memuat...</div>
      </div>
    );
  }

  if (!adminKey) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-100">
        <form
          onSubmit={(e: FormEvent) => {
            e.preventDefault();
            const form = e.target as HTMLFormElement;
            const input = form.elements.namedItem("adminKey") as HTMLInputElement;
            if (input?.value.trim()) {
              handleAuthenticated(input.value.trim());
            }
          }}
          className="bg-white p-8 rounded-lg shadow-lg w-full max-w-md"
        >
          <h1 className="text-2xl font-bold mb-2">Panel Admin KP</h1>
          <p className="text-gray-600 mb-6">Masukkan kunci admin untuk melanjutkan</p>

          <input
            type="password"
            name="adminKey"
            placeholder="Masukkan kunci admin"
            className="w-full px-4 py-2 border rounded mb-4 focus:outline-none focus:border-blue-600"
            aria-label="Input kunci admin"
          />

          <button
            type="submit"
            className="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700"
            aria-label="Tombol masuk admin"
          >
            Masuk
          </button>
        </form>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-8 py-6 flex justify-between items-center">
          <h1 className="text-3xl font-bold">Panel Admin KP</h1>
          <div className="flex items-center gap-4">
            <a
              href="/"
              className="px-4 py-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
              aria-label="Kembali ke chat"
            >
              Kembali ke Chat
            </a>
            <button
              onClick={handleLogout}
              className="px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600"
              aria-label="Tombol keluar admin"
            >
              Keluar
            </button>
          </div>
        </div>
      </div>

      {children}
    </div>
  );
};