// 用户相关类型
export interface User {
  id: string
  username: string
  displayName: string
  role: 'ADMIN' | 'STUDENT'
  status: 'ENABLED' | 'DISABLED'
}

export interface LoginResult {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: User
}

// 果园相关类型
export interface Orchard {
  id: string
  name: string
  areaMu: number
  treeCount: number
  treeAgeYears: number
  variety: string
  irrigationMode: string
  region: string
  address?: string
  managerName: string
  currentPhenology: string
  phenologyEffectiveDate?: string
  status: string
}

// 分页数据结构
export interface PageData<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
}

// API 统一响应结构（成功）
export interface ApiResult<T> {
  code: number
  message: string
  data: T
  requestId: string
  timestamp?: string
}

// API 错误响应结构
export interface ApiError {
  code: number
  message: string
  data: null
  requestId: string
  timestamp: string
}

// 业务错误码枚举（根据接口文档 2.5）
export enum ErrorCode {
  // 请求参数错误
  BAD_REQUEST = 40001,
  // 未登录或令牌失效
  UNAUTHORIZED = 40101,
  // 无操作权限
  FORBIDDEN = 40301,
  // 资源不存在
  NOT_FOUND = 40401,
  // 数据状态冲突或重复
  CONFLICT = 40901,
  // 上传文件过大
  PAYLOAD_TOO_LARGE = 41301,
  // 请求过于频繁
  TOO_MANY_REQUESTS = 42901,
  // 系统内部错误
  INTERNAL_ERROR = 50001,
  // 模型服务调用失败
  MODEL_SERVICE_ERROR = 50201,
  // Embedding 服务调用失败
  EMBEDDING_SERVICE_ERROR = 50202,
  // 天气服务调用失败
  WEATHER_SERVICE_ERROR = 50203,
  // 外部服务超时
  EXTERNAL_SERVICE_TIMEOUT = 50401
}

// HTTP 状态码映射
export const HttpStatus = {
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  CONFLICT: 409,
  PAYLOAD_TOO_LARGE: 413,
  TOO_MANY_REQUESTS: 429,
  INTERNAL_ERROR: 500,
  BAD_GATEWAY: 502,
  GATEWAY_TIMEOUT: 504
} as const

// 天气相关类型
export interface CurrentWeather {
  temperatureC: number
  weather: string
  windDirection: string
  windLevel: string
}

export interface WeatherForecast {
  date: string
  dayWeather: string
  nightWeather: string
  minTemperatureC: number
  maxTemperatureC: number
  windLevel: string
}

export interface WeatherData {
  orchardId: string
  provider: string
  updatedAt: string
  current: CurrentWeather
  forecast: WeatherForecast[]
  cached: boolean
}

// 农事任务相关类型
export interface Task {
  id: string
  type: string
  title: string
  content: string
  priority: 'HIGH' | 'MEDIUM' | 'LOW'
  suggestedTime: string
  status: 'DRAFT' | 'CONFIRMED' | 'TODO' | 'DOING' | 'DONE' | 'CANCELLED'
  basis: string
  safetyNotice: string
  orchardId?: string
  createdAt?: string
}

// 物候期历史记录类型
export interface PhenologyRecord {
  id: string
  orchardId: string
  phenology: string
  effectiveDate: string
  remark: string
  createdAt?: string
}

// 实训记录相关类型
export interface TrainingRecord {
  id: string
  orchardId: string
  orchardName?: string
  taskId?: string
  taskTitle?: string
  recordDate: string
  inspectedTreeCount: number
  abnormalTreeCount: number
  phenomenon: string
  measure: string
  result?: string
  studentId: string
  studentName?: string
  score?: number
  teacherComment?: string
  status?: 'PENDING' | 'APPROVED' | 'REJECTED'
  createdAt?: string
  updatedAt?: string
}

export interface TrainingRecordCreate {
  orchardId: string
  taskId?: string
  recordDate: string
  inspectedTreeCount: number
  abnormalTreeCount: number
  phenomenon: string
  measure: string
  result?: string
}

export interface TrainingRecordReview {
  score: number
  comment: string
  status: 'APPROVED' | 'REJECTED'
}
