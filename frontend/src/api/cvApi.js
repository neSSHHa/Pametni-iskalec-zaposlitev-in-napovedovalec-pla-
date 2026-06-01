import apiClient from "./apiClient";

export async function uploadCv(file, mode = "fast", interactionId) {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("mode", mode);

  const response = await apiClient.post("/cv/jobs/filter", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
      ...(interactionId ? { "X-Interaction-ID": interactionId } : {}),
    },
  });
  return response.data;
}

export async function getCvAnalysis(userId) {
  const response = await apiClient.get(`/cv/analysis/${userId}`);
  return response.data;
}
