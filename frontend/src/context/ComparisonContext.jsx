import { createContext, useContext, useEffect, useMemo, useState } from "react";

const STORAGE_KEY = "jobradar-comparison-jobs";
const MAX_COMPARISON_JOBS = 2;

const ComparisonContext = createContext(null);

export function ComparisonProvider({ children }) {
  const [jobs, setJobs] = useState([]);
  const [toast, setToast] = useState("");

  useEffect(() => {
    window.localStorage.removeItem(STORAGE_KEY);
  }, []);

  useEffect(() => {
    if (!toast) return undefined;

    const timeout = window.setTimeout(() => setToast(""), 3200);
    return () => window.clearTimeout(timeout);
  }, [toast]);

  const value = useMemo(() => {
    const isSelected = (jobId) => jobs.some((job) => job.id === jobId);

    const toggleJob = (job) => {
      if (!job?.id) return;

      setJobs((currentJobs) => {
        if (currentJobs.some((currentJob) => currentJob.id === job.id)) {
          return currentJobs.filter((currentJob) => currentJob.id !== job.id);
        }

        if (currentJobs.length >= MAX_COMPARISON_JOBS) {
          setToast("You can compare up to 2 jobs.");
          return currentJobs;
        }

        return [...currentJobs, job];
      });
    };

    const removeJob = (jobId) => {
      setJobs((currentJobs) => currentJobs.filter((job) => job.id !== jobId));
    };

    const clearJobs = () => setJobs([]);

    return {
      jobs,
      maxJobs: MAX_COMPARISON_JOBS,
      count: jobs.length,
      isSelected,
      toggleJob,
      removeJob,
      clearJobs,
    };
  }, [jobs]);

  return (
    <ComparisonContext.Provider value={value}>
      {children}
      {toast ? (
        <div className="comparison-toast" role="status" aria-live="polite">
          {toast}
        </div>
      ) : null}
    </ComparisonContext.Provider>
  );
}

export function useComparison() {
  const context = useContext(ComparisonContext);

  if (!context) {
    throw new Error("useComparison must be used inside ComparisonProvider");
  }

  return context;
}
