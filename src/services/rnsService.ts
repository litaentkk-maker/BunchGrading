import { BluetoothSerial } from 'capacitor-bluetooth-serial';
import { Capacitor } from '@capacitor/core';
import { HarvestRecord, RNSConfig, RNSStatus } from '../types';

class RNSService {
  private isCapacitor = Capacitor.isNativePlatform();
  private connectedAddress: string | null = null;
  private status: RNSStatus = {
    isConnected: false,
    isRnsRunning: false,
  };

  async connect(): Promise<RNSStatus> {
    try {
      if (this.isCapacitor) {
        // Bluetooth Classic - Scan for devices
        const result = await BluetoothSerial.scan();
        const devices = result.devices;
        
        // Find RNode in paired devices
        const rnode = devices.find((d: any) => d.name?.toLowerCase().includes('rnode') || d.name?.toLowerCase().includes('reticulum'));
        
        if (!rnode) {
          throw new Error('RNode not found in paired devices. Please pair it in Android settings first.');
        }

        await BluetoothSerial.connect({ address: rnode.address });
        this.connectedAddress = rnode.address;
        
        this.status = {
          ...this.status,
          isConnected: true,
          device: rnode.name || 'RNode',
        };

        return this.status;
      }

      throw new Error('Bluetooth Classic is only supported on Android native app.');
    } catch (error: any) {
      console.error('RNS Connection Error:', error);
      throw error;
    }
  }

  async disconnect() {
    if (this.isCapacitor && this.connectedAddress) {
      await BluetoothSerial.disconnect({ address: this.connectedAddress });
      this.connectedAddress = null;
    }
    this.status = {
      isConnected: false,
      isRnsRunning: false,
    };
  }

  async startRNS(config: RNSConfig): Promise<string> {
    // In a real implementation, this would send a command to the RNode
    // to initialize the RNS stack with the given config.
    console.log('Starting RNS with config:', config);
    
    // Simulate RNS startup delay
    await new Promise(resolve => setTimeout(resolve, 1500));
    
    const mockHash = Math.random().toString(16).substring(2, 10);
    this.status = {
      ...this.status,
      isRnsRunning: true,
      localHash: mockHash,
    };
    
    return mockHash;
  }

  async syncRecord(record: HarvestRecord, destinationHex: string): Promise<string> {
    if (!this.status.isRnsRunning) {
      throw new Error('RNS is not running. Please start RNS first.');
    }

    console.log(`Syncing record ${record.id} to ${destinationHex} via LXMF...`);
    
    // Prepare LXMF payload (simulated)
    const payload = {
      type: 'HARVEST_RECORD',
      data: record,
      timestamp: Date.now(),
    };

    // Simulate transmission delay (LoRa is slow!)
    const transmissionTime = 2000 + (record.photoUrl ? 3000 : 0);
    await new Promise(resolve => setTimeout(resolve, transmissionTime));

    const messageHash = Math.random().toString(16).substring(2, 10);
    console.log(`Record ${record.id} synced successfully. Message Hash: ${messageHash}`);
    
    return messageHash;
  }

  async announce(nickname: string) {
    if (!this.status.isRnsRunning) return;
    
    console.log(`Announcing ${nickname} to Reticulum network...`);
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    this.status = {
      ...this.status,
      lastAnnounce: Date.now(),
    };
  }

  getStatus(): RNSStatus {
    return this.status;
  }
}

export const rnsService = new RNSService();
