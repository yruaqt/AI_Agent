export interface User { id: string; username: string; displayName: string; role: 'ADMIN'|'STUDENT'; status: string }
export interface LoginResult { accessToken: string; tokenType: string; expiresIn: number; user: User }
export interface Orchard { id: string; name: string; areaMu: number; treeCount: number; treeAge: number; variety: string; irrigationMode: string; region: string; address?: string; manager: string; currentPhenology: string; status: string }
export interface PageData<T> { items: T[]; page: number; pageSize: number; total: number }
export interface ApiResult<T> { code: number; message: string; data: T; requestId: string; timestamp?: string }
