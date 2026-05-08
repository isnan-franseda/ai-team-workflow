import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { Layout } from "./components/Layout";
import { ChatPage } from "./components/ChatPage";
import { AdminRouteGuard } from "./components/AdminRouteGuard";
import { AdminUploadPage } from "./components/AdminUploadPage";

function App() {
  return (
    <BrowserRouter>
      <Layout />
      <Routes>
        <Route path="/" element={<ChatPage />} />
        <Route
          path="/admin"
          element={
            <AdminRouteGuard>
              <AdminUploadPage />
            </AdminRouteGuard>
          }
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;