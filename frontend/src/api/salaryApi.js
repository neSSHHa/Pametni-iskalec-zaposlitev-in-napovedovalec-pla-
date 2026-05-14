import apiClient from "./apiClient";

export async function predictSalary(payload) {
  const response = await apiClient.post("/salary/predict", payload);
  return response.data;
}
