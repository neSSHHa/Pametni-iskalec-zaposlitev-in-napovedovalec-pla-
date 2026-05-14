import apiClient from "./apiClient";

export async function uploadCv(file) {
  const formData = new FormData();
  formData.append("file", file);

  const response = await apiClient.post("/cv/upload", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return response.data;
}

export async function getCvAnalysis(userId) {
  const response = await apiClient.get(`/cv/analysis/${userId}`);
  return response.data;
}
