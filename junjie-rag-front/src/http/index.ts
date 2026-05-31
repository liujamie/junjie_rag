import axios from "axios";
import { BASE_URL, HEADER } from "./config";

const service = axios.create({
  baseURL: BASE_URL,
  //   timeout: 10000,
  withCredentials: false,
  // headers: HEADER,
});

// 创建请求拦截器
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    console.log("🔍 获取结果:", token);
    if (token !== null) {
      config.headers.Authorization = "Bearer " + token;
      console.log("🔍 获取结果:", config.headers.Authorization);

    }
    return config;
  },
  (error) => {
    console.log(error);

    return Promise.reject(error);
  }
);

// 创建响应拦截器
// service.interceptors.response.use(
//   (res: any) => {
//     return res.data;
//   },
//   (error) => {
//     let message = "";
//     console.log(error);
//     return Promise.reject(message);
//   }
// );

// export default service;

// 创建响应拦截器
service.interceptors.response.use(
  (res: any) => {
    return res.data;
  },
  (error) => {
    // 1. 专门处理 401 状态码
    if (error.response && error.response.status === 401) {
      // 清除本地存储的 token
      localStorage.removeItem("token");
      // 提示用户并跳转登录页 (需根据实际路由调整)
      console.warn("认证失败，已清除 token，请重新登录");
      // window.location.href = "/login"; 
    }

    // 2. 提取具体的错误信息
    let message = error.response?.data?.message || error.message || "请求失败";
    console.error(message);
    
    // 3. 拒绝 Promise 并传递错误对象
    return Promise.reject(new Error(message));
  }
);

export default service;

export const getAuthHeaders = () => {
  const token = localStorage.getItem("token");
  return token ? { Authorization: "Bearer " + token } : {};
};
