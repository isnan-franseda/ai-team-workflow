import { useState } from "react";
import { AdminKeyPrompt } from "./components/AdminKeyPrompt";
import { UploadPage } from "./components/UploadPage";

function App() {
  const [adminKey, setAdminKey] = useState<string | null>(null);

  const handleAuthenticated = (key: string) => {
    setAdminKey(key);
  };

  const handleLogout = () => {
    sessionStorage.removeItem("admin_key");
    setAdminKey(null);
  };

  if (!adminKey) {
    return <AdminKeyPrompt onAuthenticated={handleAuthenticated} />;
  }

  return <UploadPage adminKey={adminKey} onLogout={handleLogout} />;
}

export default App;