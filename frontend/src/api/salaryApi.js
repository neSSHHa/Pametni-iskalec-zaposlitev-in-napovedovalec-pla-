import apiClient from "./apiClient";

export async function predictSalary(payload, interactionId) {
  const response = await apiClient.post("/salary/predict", payload, {
    headers: interactionId ? { "X-Interaction-ID": interactionId } : {},
  });
  return response.data;
}
