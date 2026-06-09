import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { expect, test } from "vitest";

import MotionJobsPanel from "./MotionJobsPanel.jsx";
import { ComparisonProvider } from "../../context/ComparisonContext.jsx";

// Renders the jobs panel with fake data, without calling the backend.
function renderPanel(props = {}) {
  const jobs = props.jobs || sampleJobs();

  return render(
    <ComparisonProvider>
      <MotionJobsPanel
        mode="search"
        score={72}
        jobs={jobs}
        totalCount={props.totalCount ?? jobs.length}
        filterRequest={props.filterRequest || sampleFilter()}
        salaryPrediction={props.salaryPrediction}
        salaryPredictionLoading={props.salaryPredictionLoading || false}
        query="java jobs in austria"
        {...props}
      />
    </ComparisonProvider>
  );
}

// Fake filter that looks like the AI/backend response after a prompt search.
function sampleFilter() {
  return {
    job: {
      jobname: "Java Developer",
      requiredExperience: 2,
      experienceLevelName: "Junior",
      educationLevel: "Bachelor",
    },
    location: {
      city: "Vienna",
      country: "Austria",
    },
    skills: ["Java", "SQL"],
    workTypes: ["Hybrid"],
  };
}

// Fake jobs used to test rendering, sorting, comparison and details behavior.
function sampleJobs() {
  return [
    {
      id: "job-old-high",
      title: "Senior Java Developer",
      company: "Code GmbH",
      city: "Vienna",
      country: "Austria",
      mode: "Hybrid",
      level: "Senior",
      postedDate: "2026-05-20",
      match: 95,
      confidence: 90,
      salary: "3.000 - 4.500 EUR",
      salaryMin: 3000,
      salaryMax: 4500,
      tags: ["Java", "SQL", "Spring"],
      skills: ["Java", "SQL", "Spring"],
      description: "Develop backend applications.",
      educationLevel: "Bachelor",
      sourceUrl: "https://example.com/java",
    },
    {
      id: "job-new-low",
      title: "Junior React Developer",
      company: "Frontend GmbH",
      city: "Graz",
      country: "Austria",
      mode: "Remote",
      level: "Junior",
      postedDate: "2026-05-25",
      match: 62,
      confidence: 70,
      salary: "2.400 - 3.200 EUR",
      salaryMin: 2400,
      salaryMax: 3200,
      tags: ["React", "JavaScript"],
      skills: ["React", "JavaScript"],
      description: "Build UI features.",
      educationLevel: "Bachelor",
      sourceUrl: "",
    },
    {
      id: "job-new-high",
      title: "Java Engineer",
      company: "Tech GmbH",
      city: "Wien",
      country: "Austria",
      mode: "On-site",
      level: "Mid",
      postedDate: "2026-05-25",
      match: 88,
      confidence: 82,
      salary: "2.800 - 3.800 EUR",
      salaryMin: 2800,
      salaryMax: 3800,
      tags: ["Java", "Databases"],
      skills: ["Java", "Databases"],
      description: "Work on enterprise systems.",
      educationLevel: "Master",
      sourceUrl: "https://example.com/engineer",
    },
  ];
}

// Main results card should show the most important job fields.
test("renders search results with important job information", () => {
  renderPanel();

  expect(screen.getByText("Senior Java Developer")).toBeInTheDocument();
  expect(screen.getByText("Code GmbH")).toBeInTheDocument();
  expect(screen.getByText(/Vienna, Austria/)).toBeInTheDocument();
  expect(screen.getAllByText("Java").length).toBeGreaterThan(0);
  expect(screen.getByText("95%")).toBeInTheDocument();
  expect(screen.getByText(/Posted 20\. 05\. 2026/)).toBeInTheDocument();
});

// Salary prediction is loaded separately, but the panel must render it when available.
test("renders salary range card when salary prediction is available", () => {
  renderPanel({
    salaryPrediction: {
      available: true,
      predictedMinSalary: 2600,
      predictedMaxSalary: 3400,
      currency: "EUR",
      profileCompleteness: 90,
      modelMae: 425,
      marketAssumed: false,
    },
  });

  const salaryCard = screen.getByLabelText("Predicted salary range");

  expect(salaryCard).toBeInTheDocument();
  expect(within(salaryCard).getByText("Austrian market salary estimate")).toBeInTheDocument();
  expect(within(salaryCard).getByText(/2600 EUR\s*-\s*3400 EUR/)).toBeInTheDocument();
  expect(within(salaryCard).getByText((content) => content.replace(/\s/g, "") === "90%")).toBeInTheDocument();
});

// Default ranking is by compatibility; Date posted uses compatibility as tie-breaker.
test("sorts by compatibility by default and by date posted when selected", async () => {
  const user = userEvent.setup();
  renderPanel();

  let titles = screen.getAllByRole("heading", { level: 3 }).map((heading) => heading.textContent);

  expect(titles).toEqual([
    "Senior Java Developer",
    "Java Engineer",
    "Junior React Developer",
  ]);

  await user.click(screen.getByRole("button", { name: "Date posted" }));

  titles = screen.getAllByRole("heading", { level: 3 }).map((heading) => heading.textContent);

  expect(titles).toEqual([
    "Java Engineer",
    "Junior React Developer",
    "Senior Java Developer",
  ]);
});

// Comparison is frontend-only and must never allow more than two selected jobs.
test("limits comparison selection to two jobs", async () => {
  const user = userEvent.setup();
  renderPanel();

  const compareButtons = screen.getAllByRole("button", { name: /add job to comparison/i });

  await user.click(compareButtons[0]);
  await user.click(compareButtons[1]);
  await user.click(compareButtons[2]);

  expect(screen.getAllByRole("button", { name: /remove job from comparison/i })).toHaveLength(2);
  expect(screen.getByText("You can compare up to 2 jobs.")).toBeInTheDocument();
});

// Details modal should expose source links only when present and expand long descriptions.
test("job details shows source link only when available and expands long description", async () => {
  const user = userEvent.setup();
  const longDescription = Array(30).fill("This is a detailed job description.").join(" ");
  const jobs = sampleJobs();

  jobs[0] = {
    ...jobs[0],
    description: longDescription,
    sourceUrl: "https://example.com/java",
  };

  renderPanel({ jobs });

  await user.click(screen.getByRole("heading", { level: 3, name: "Senior Java Developer" }));

  expect(screen.getByRole("dialog")).toBeInTheDocument();
  expect(screen.getByText("Source")).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "Open source listing" })).toHaveAttribute(
    "href",
    "https://example.com/java"
  );

  expect(screen.getByText("See more")).toBeInTheDocument();

  await user.click(screen.getByText("See more"));

  expect(screen.getByText("See less")).toBeInTheDocument();

  await user.click(screen.getByRole("button", { name: /close job details/i }));

  await user.click(screen.getByRole("heading", { level: 3, name: "Junior React Developer" }));

  expect(screen.queryByText("Source")).not.toBeInTheDocument();
  expect(screen.queryByRole("link", { name: "Open source listing" })).not.toBeInTheDocument();
});
