import axios from 'axios'
export interface ApiResponse<T>{code:number;message:string;data:T;requestId:string;timestamp:string}
const api=axios.create({baseURL:import.meta.env.VITE_API_BASE||'/api/v1',timeout:10000})
export const unwrap=<T>(response:{data:ApiResponse<T>})=>response.data.data
export default api

