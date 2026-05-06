import { useState, useEffect, type FormEvent } from "react";
import { mapErrorToBahasa } from "../utils/errorMapper";

interface AdminKeyPromptProps {
  onAuthenticated: (adminKey: string) => void;
}

export const AdminKeyPrompt = ({ onAuthenticated }: AdminKeyPromptProps) => {
  const [adminKey, setAdminKey] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    const stored = sessionStorage.getItem("admin_key");
    if (stored) {
      onAuthenticated(stored);
    }
  }, [onAuthenticated]);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsLoading(true);

    if (!adminKey.trim()) {
      setError("Kunci admin diperlukan");
      setIsLoading(false);
      return;
    }

    try {
      sessionStorage.setItem("admin_key", adminKey);
      onAuthenticated(adminKey);
    } catch (err) {
      setError(mapErrorToBahasa(err));
      setIsLoading(false);
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-100">
      <form
        onSubmit={handleSubmit}
        className="bg-white p-8 rounded-lg shadow-lg w-full max-w-md"
      >
        <h1 className="text-2xl font-bold mb-2">Panel Admin KP</h1>
        <p className="text-gray-600 mb-6">Masukkan kunci admin untuk melanjutkan</p>

        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
            {error}
          </div>
        )}

        <input
          type="password"
          placeholder="Masukkan kunci admin"
          value={adminKey}
          onChange={(e) => setAdminKey(e.target.value)}
          disabled={isLoading}
          className="w-full px-4 py-2 border rounded mb-4 focus:outline-none focus:border-blue-600"
          aria-label="Admin Key Input"
        />

        <button
          type="submit"
          disabled={isLoading}
          className="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700 disabled:opacity-50"
        >
          {isLoading ? "Memproses..." : "Masuk"}
        </button>
      </form>
    </div>
  );
};