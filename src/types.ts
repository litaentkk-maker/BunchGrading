export interface HarvestRecord {
  id: string;
  harvesterUid: string;
  harvesterName: string;
  collectionPoint: string;
  bunchCount: number;
  photoUrl: string;
  timestamp: number;
  location?: {
    latitude: number;
    longitude: number;
  } | null;
}

export interface UserProfile {
  uid: string;
  email: string;
  displayName?: string;
  role: 'harvester' | 'admin';
  photoURL?: string;
  createdAt?: string;
}

export interface RNSConfig {
  frequency: number;
  bandwidth: number;
  txPower: number;
  spreadingFactor: number;
  codingRate: number;
  destinationHex?: string;
  nickname: string;
}

export interface RNSStatus {
  isConnected: boolean;
  isRnsRunning: boolean;
  localHash?: string;
  lastAnnounce?: number;
  device?: string;
  statusMessage?: string;
}
