import { useEffect, useState } from "react";
import { ComparisonProvider } from "./context/ComparisonContext.jsx";
import { AuthProvider } from "./context/AuthContext.jsx";
import AdminPanelPage from "./pages/AdminPanelPage.jsx";
import AuthCallbackPage from "./pages/AuthCallbackPage.jsx";
import CompareJobsPage from "./pages/CompareJobsPage.jsx";
import LoginPage from "./pages/LoginPage.jsx";
import MotionCvResultPage from "./pages/MotionCvResultPage.jsx";
import MotionLandingPage from "./pages/MotionLandingPage.jsx";
import MotionPromptResultPage from "./pages/MotionPromptResultPage.jsx";
import MotionExperience from "./pages/MotionExperience.jsx";
import RegisterPage from "./pages/RegisterPage.jsx";
import "./styles/motion.css";

export default function App() {
  const [path, setPath] = useState(() => window.location.pathname.replace(/\/+$/, "") || "/");

  useEffect(() => {
    const handleNavigation = () => setPath(window.location.pathname.replace(/\/+$/, "") || "/");

    window.addEventListener("popstate", handleNavigation);
    window.addEventListener("jobradar:navigate", handleNavigation);

    return () => {
      window.removeEventListener("popstate", handleNavigation);
      window.removeEventListener("jobradar:navigate", handleNavigation);
    };
  }, []);

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "auto" });
  }, [path]);

  let page;

  if (path === "/motion-cv" || path === "/cv") page = <MotionCvResultPage />;
  else if (path === "/motion-prompt" || path === "/prompt") page = <MotionPromptResultPage />;
  else if (path === "/analytics") page = <MotionExperience initialMode="analytics" resultPage />;
  else if (path === "/compare") page = <CompareJobsPage />;
  else if (path === "/login") page = <LoginPage />;
  else if (path === "/register") page = <RegisterPage />;
  else if (path === "/auth/callback") page = <AuthCallbackPage />;
  else if (path === "/admin") page = <AdminPanelPage />;
  else page = <MotionLandingPage />;

  return (
    <AuthProvider>
      <ComparisonProvider>{page}</ComparisonProvider>
    </AuthProvider>
  );
}
