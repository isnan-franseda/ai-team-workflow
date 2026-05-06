import { useState, useEffect } from "react";

interface AdminKeyPromptProps {
  onAuthenticated: (adminKey: string) => void;
}

export const AdminKeyPrompt: React.FC<AdminKeyPromptProps> = ({ onAuthenticated }) => {
  const [adminKey, setAdminKey] = useState("");

  useEffect(() => {
    const stored = sessionStorage.getItem("admin_key");
    if (stored) {
      onAuthenticated(stored);
    }
  }, [onAuthenticated]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    sessionStorage.setItem("admin_key", adminKey);
    onAuthenticated(adminKey);
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-100">
      <form
        onSubmit={handleSubmit}
        className="bg-white p-8 rounded-lg shadow-lg w-full max-w-md"
      >
        <h1 className="text-2xl font-bold mb-6">Admin Panel</h1>
        <input
          type="password"
          placeholder="Masukkan kunci admin"
          value={adminKey}
          onChange={(e) => setAdminKey(e.target.value)}
          className="w-full px-4 py-2 border rounded mb-4"
          aria-label="Admin Key Input"
        />
        <button
          type="submit"
          className="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700"
        >
          Masuk
        </button>
      </form>
    </div>
  );
};