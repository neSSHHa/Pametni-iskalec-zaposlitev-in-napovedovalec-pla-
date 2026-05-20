import apiClient from "./apiClient";

export async function getJobs(filters = {}) {
  const response = await apiClient.get("/jobs", { params: filters });
  return response.data;
}

export async function getJobById(id) {
  const response = await apiClient.get(`/jobs/${id}`);
  return response.data;
}

export async function searchJobsByText(query) {
  const response = await apiClient.post("/jobs/text-search", { query });
  return response.data;
}

export async function searchJobsByPrompt(text) {
  const response = await apiClient.post("/ai/jobs/filter", { text });
  return response.data;
}
