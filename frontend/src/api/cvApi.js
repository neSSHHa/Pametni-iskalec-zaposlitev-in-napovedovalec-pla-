import apiClient from "./apiClient";

export async function uploadCv(file, mode = "fast") {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("mode", mode);

  const response = await apiClient.post("/cv/jobs/filter", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return response.data;
}

export async function getCvAnalysis(userId) {
  const response = await apiClient.get(`/cv/analysis/${userId}`);
  return response.data;
}
