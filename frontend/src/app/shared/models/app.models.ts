export interface AuthResponse {
  token: string | null;
  role: string;
  message: string;
}

export interface JwtUser {
  userId: number | null;
  username: string;
  role: string;
}

export interface WalletBalanceResponse {
  balance: number;
}

export interface WalletAccount {
  id?: number;
  userId: number;
  balance: number;
}

export interface TransactionItem {
  id: number;
  userId: number;
  senderId: number;
  receiverId: number;
  amount: number;
  type: string;
  status: string;
  createdAt: string;
}

export interface RewardsAccount {
  userId: number;
  points: number;
  tier: string;
}

export interface RewardCatalogItem {
  id: number;
  name: string;
  pointsCost: number;
  stock: number;
  rewardType: string;
  merchantId?: number;
}

export interface UserProfile {
  id?: number;
  authUserId?: number;
  fullName: string;
  email: string;
  phone: string;
  kycStatus?: string;
  kycDocumentId?: string;
}

export interface NotificationStats {
  sent: number;
  failed: number;
  total: number;
}

export interface NotificationMessage {
  id: number;
  userId: number;
  eventType: string;
  channel: string;
  target: string;
  message: string;
  status: string;
  failureReason?: string;
  createdAt: string;
  title?: string;
  subject?: string;
  body?: string;
  type?: string;
}

