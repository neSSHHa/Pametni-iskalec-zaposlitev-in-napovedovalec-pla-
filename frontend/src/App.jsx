import { useEffect, useState } from "react";
import MotionCvResultPage from "./pages/MotionCvResultPage.jsx";
import MotionLandingPage from "./pages/MotionLandingPage.jsx";
import MotionPromptResultPage from "./pages/MotionPromptResultPage.jsx";
import MotionExperience from "./pages/MotionExperience.jsx";
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

  if (path === "/motion-cv" || path === "/cv") return <MotionCvResultPage />;
  if (path === "/motion-prompt" || path === "/prompt") return <MotionPromptResultPage />;
  if (path === "/analytics") return <MotionExperience initialMode="analytics" resultPage />;

  return <MotionLandingPage />;
}
