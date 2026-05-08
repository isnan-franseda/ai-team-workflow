import { Link, useLocation } from "react-router-dom";

export const Layout = () => {
  const location = useLocation();
  const isAdminRoute = location.pathname.startsWith("/admin");

  return (
    <nav className="bg-white border-b border-gray-200 shadow-sm px-4">
      <div className="max-w-4xl mx-auto h-16 flex items-center justify-between">
        <Link
          to="/"
          className="flex items-center gap-3 hover:opacity-80"
          aria-label="Beranda Chatbot Kredit Pintar"
        >
          <div className="w-10 h-10 bg-kp-green rounded-xl flex items-center justify-center">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="w-6 h-6 text-white"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              aria-hidden="true"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"
              />
            </svg>
          </div>
          <span className="text-lg font-semibold text-gray-900">Kredit Pintar</span>
        </Link>

        <div className="flex items-center gap-4">
          {isAdminRoute ? (
            <>
              <Link
                to="/"
                className="px-4 py-2 text-gray-600 hover:text-kp-green hover:bg-kp-green-light rounded-lg transition-colors"
                aria-label="Buka halaman chat pengguna"
              >
                Chat
              </Link>
              <span className="px-3 py-1 bg-kp-orange text-white text-sm font-medium rounded-full">
                Admin
              </span>
            </>
          ) : (
            <Link
              to="/admin"
              className="px-4 py-2 text-white bg-kp-orange hover:bg-kp-orange-dark rounded-lg transition-colors"
              aria-label="Buka panel admin"
            >
              Panel Admin
            </Link>
          )}
        </div>
      </div>
    </nav>
  );
};