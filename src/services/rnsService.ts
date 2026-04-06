import { registerPlugin } from '@capacitor/core';
import { Capacitor } from '@capacitor/core';
import { HarvestRecord, RNSConfig, RNSStatus } from '../types';

interface RNSPlugin {
  startRNS(options: { nickname: string }): Promise<{ localHash: string }>;
  connectRNode(options: { address: string }): Promise<void>;
  disconnectRNode(): Promise<void>;
  getPairedDevices(): Promise<{ devices: { name: string, address: string }[] }>;
  injectRNode(options: { frequency: number, bandwidth: number, txpower: number, spreadingfactor: number, codingrate: number }): Promise<{ status: string }>;
  sendText(options: { destination: string, text: string }): Promise<{ messageHash: string }>;
  announce(): Promise<void>;
  addListener(eventName: string, listenerFunc: (data: any) => void): Promise<any>;
}

const RNSPlugin = registerPlugin<RNSPlugin>('RNSPlugin');

class RNSService {
  private isCapacitor = Capacitor.isNativePlatform();
  private status: RNSStatus = {
    isConnected: false,
    isRnsRunning: false,
  };

  private messages: any[] = [];
  private messageListeners: ((msg: any) => void)[] = [];

  constructor() {
    if (this.isCapacitor) {
      RNSPlugin.addListener('onAnnounceReceived', (data) => {
        console.log('Announce received:', data);
        const msg = {
          id: Date.now(),
          type: 'info',
          text: `Discovered node: ${data.name || 'Unknown'} (${data.hash.substring(0, 8)}...)`,
          time: new Date().toLocaleTimeString()
        };
        this.addMessage(msg);
      });
      RNSPlugin.addListener('onNewMessage', (data) => {
        console.log('New message:', data);
        const msg = {
          id: Date.now(),
          type: 'in',
          text: data.content,
          sender: data.sender,
          time: new Date(data.time).toLocaleTimeString(),
          hash: data.hash
        };
        this.addMessage(msg);
      });
      RNSPlugin.addListener('onStatusUpdate', (data: { message: string }) => {
        console.log('Status update:', data.message);
        this.status = {
          ...this.status,
          statusMessage: data.message
        };
      });
    }
  }

  private addMessage(msg: any) {
    this.messages = [msg, ...this.messages].slice(0, 50);
    this.messageListeners.forEach(l => l(msg));
  }

  onMessage(callback: (msg: any) => void) {
    this.messageListeners.push(callback);
    return () => {
      this.messageListeners = this.messageListeners.filter(l => l !== callback);
    };
  }

  getMessages() {
    return this.messages;
  }

  async getDevices(): Promise<{ name: string, address: string }[]> {
    if (!this.isCapacitor) return [];
    const { devices } = await RNSPlugin.getPairedDevices();
    return devices;
  }

  async connect(): Promise<RNSStatus> {
    try {
      if (this.isCapacitor) {
        // In a real app, we would scan, but for now we'll assume the user has a paired RNode
        // or we can use a hardcoded address for testing if needed, but better to let them pick.
        // For this implementation, we'll look for "RNode" in paired devices if we had a list.
        // Since we don't have a list here, we'll prompt or use a placeholder.
        // Actually, the user's RNSPage doesn't have a device selector yet.
        
        // Let's assume we have a way to get the address. 
        // For now, I'll use a placeholder or try to find one.
        const address = "00:00:00:00:00:00"; // Placeholder
        
        // The user's RNSPage calls connect() without arguments.
        // I should probably add a device picker to RNSPage later.
        // For now, let's just try to connect to the first paired RNode if possible.
        
        // Wait, I'll just use the address from the config if available or prompt.
        // Actually, let's just resolve for now and tell the user to provide an address.
        throw new Error('Please select an RNode device address in the settings.');
      }

      throw new Error('Bluetooth Classic is only supported on Android native app.');
    } catch (error: any) {
      console.error('RNS Connection Error:', error);
      throw error;
    }
  }

  async connectToAddress(address: string): Promise<RNSStatus> {
    if (!this.isCapacitor) throw new Error('Native only');
    
    await RNSPlugin.connectRNode({ address });
    this.status = {
      ...this.status,
      isConnected: true,
      device: address,
    };
    return this.status;
  }

  async disconnect() {
    if (this.isCapacitor) {
      await RNSPlugin.disconnectRNode();
    }
    this.status = {
      isConnected: false,
      isRnsRunning: false,
    };
  }

  async startRNS(config: RNSConfig): Promise<string> {
    if (!this.isCapacitor) throw new Error('Native only');
    
    const { localHash } = await RNSPlugin.startRNS({ nickname: config.nickname });
    
    // Also inject the RNode config
    await RNSPlugin.injectRNode({
      frequency: config.frequency,
      bandwidth: config.bandwidth,
      txpower: config.txPower,
      spreadingfactor: config.spreadingFactor,
      codingrate: config.codingRate
    });

    this.status = {
      ...this.status,
      isRnsRunning: true,
      localHash: localHash,
    };
    
    return localHash;
  }

  async syncRecord(record: HarvestRecord, destinationHex: string): Promise<string> {
    if (!this.status.isRnsRunning) {
      throw new Error('RNS is not running. Please start RNS first.');
    }

    const text = `HARVEST_RECORD|${record.id}|${record.harvesterName}|${record.collectionPoint}|${record.bunchCount}|${record.timestamp}`;
    const { messageHash } = await RNSPlugin.sendText({ destination: destinationHex, text });
    
    return messageHash;
  }

  async announce(nickname: string) {
    if (!this.status.isRnsRunning) return;
    await RNSPlugin.announce();
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
