import { useEffect, useState } from "react";
import { ComparisonProvider } from "./context/ComparisonContext.jsx";
import CompareJobsPage from "./pages/CompareJobsPage.jsx";
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

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "auto" });
  }, [path]);

  let page;

  if (path === "/motion-cv" || path === "/cv") page = <MotionCvResultPage />;
  else if (path === "/motion-prompt" || path === "/prompt") page = <MotionPromptResultPage />;
  else if (path === "/analytics") page = <MotionExperience initialMode="analytics" resultPage />;
  else if (path === "/compare") page = <CompareJobsPage />;
  else page = <MotionLandingPage />;

  return <ComparisonProvider>{page}</ComparisonProvider>;
}
