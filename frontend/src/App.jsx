import MotionCvResultPage from "./pages/MotionCvResultPage.jsx";
import MotionLandingPage from "./pages/MotionLandingPage.jsx";
import MotionPromptResultPage from "./pages/MotionPromptResultPage.jsx";
import "./styles/motion.css";

export default function App() {
  const path = window.location.pathname.replace(/\/+$/, "") || "/";

  if (path === "/motion-cv" || path === "/cv") return <MotionCvResultPage />;
  if (path === "/motion-prompt" || path === "/prompt") return <MotionPromptResultPage />;

  return <MotionLandingPage />;
}
