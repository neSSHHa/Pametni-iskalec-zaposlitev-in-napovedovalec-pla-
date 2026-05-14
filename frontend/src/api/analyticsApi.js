import apiClient from "./apiClient";

export async function getSkillStats() {
  const response = await apiClient.get("/analytics/skills");
  return response.data;
}

export async function getLocationStats() {
  const response = await apiClient.get("/analytics/locations");
  return response.data;
}
