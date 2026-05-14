import { BriefcaseBusiness } from "lucide-react";

export default function App() {
  return (
    <main className="welcome-page">
      <section className="welcome-panel">
        <div className="brand-mark">
          <BriefcaseBusiness size={34} />
        </div>
        <p className="eyebrow">Smart Jobs</p>
        <h1>Welcome</h1>
        <p className="lead">
          Pametni iskalec zaposlitev in napovedovalec plac je spreman za razvoj.
        </p>
      </section>
    </main>
  );
}
