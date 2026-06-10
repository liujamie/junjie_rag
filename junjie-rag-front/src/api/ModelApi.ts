import service from "@/http";

export interface LlmModelConfig {
  id?: number;
  name: string;
  provider: string;
  baseUrl: string;
  modelName: string;
  apiKey?: string;
  temperature: number;
  maxTokens: number;
  topP: number;
  isDefault?: boolean;
  sortOrder?: number;
}

export const listModelsApi = (): Promise<any> =>
  service.get('/model/list');

export const switchModelApi = (id: number): Promise<any> =>
  service.post(`/model/switch?id=${id}`);

export const addModelApi = (data: LlmModelConfig): Promise<any> =>
  service.post('/model/add', data);

export const updateModelApi = (data: LlmModelConfig): Promise<any> =>
  service.put('/model/update', data);

export const deleteModelApi = (id: number): Promise<any> =>
  service.delete(`/model/${id}`);

export const getCurrentModelApi = (): Promise<any> =>
  service.get('/model/current');
