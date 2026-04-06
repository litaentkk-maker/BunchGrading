import { useState, useEffect } from 'react';
import { Capacitor } from '@capacitor/core';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Slider } from '@/components/ui/slider';
import { 
  Radio, 
  Wifi, 
  WifiOff, 
  RefreshCw, 
  Send, 
  Settings, 
  Zap, 
  CheckCircle2, 
  AlertCircle,
  ArrowLeft,
  Activity,
  Bluetooth,
  BluetoothOff
} from 'lucide-react';
import { HarvestRecord, RNSConfig, RNSStatus } from '@/src/types';
import { rnsService } from '@/src/services/rnsService';
import { toast } from 'sonner';
import { motion, AnimatePresence } from 'motion/react';

interface RNSPageProps {
  records: HarvestRecord[];
  onBack: () => void;
}

export default function RNSPage({ records, onBack }: RNSPageProps) {
  const [config, setConfig] = useState<RNSConfig>({
    frequency: 433000000,
    bandwidth: 125000,
    txPower: 17,
    spreadingFactor: 8,
    codingRate: 6,
    destinationHex: '',
    nickname: localStorage.getItem('palm_harvest_user') ? JSON.parse(localStorage.getItem('palm_harvest_user')!).displayName : 'Harvester'
  });

  const [status, setStatus] = useState<RNSStatus>(rnsService.getStatus());
  const [devices, setDevices] = useState<{ name: string, address: string }[]>([]);
  const [selectedAddress, setSelectedAddress] = useState<string>('');
  const [isConnecting, setIsConnecting] = useState(false);
  const [isStarting, setIsStarting] = useState(false);
  const [isSyncing, setIsSyncing] = useState(false);
  const [syncProgress, setSyncProgress] = useState(0);

  useEffect(() => {
    const fetchDevices = async () => {
      try {
        // Request permissions first on Android 12+
        if (Capacitor.getPlatform() === 'android') {
          // We use a simple check for now, but in a real app we'd use a permission plugin
          // For this environment, we'll assume the user can grant them if prompted
          // or we can try to trigger a scan which usually prompts for permissions.
          console.log('Checking permissions...');
        }
        
        const pairedDevices = await rnsService.getDevices();
        setDevices(pairedDevices);
        if (pairedDevices.length > 0) {
          setSelectedAddress(pairedDevices[0].address);
        }
      } catch (error) {
        console.error('Failed to fetch devices:', error);
      }
    };
    fetchDevices();

    // Listen for status updates from the service
    const interval = setInterval(() => {
      setStatus({ ...rnsService.getStatus() });
    }, 500);

    return () => clearInterval(interval);
  }, []);

  const handleConnect = async () => {
    if (!selectedAddress) {
      return toast.error('Please select an RNode device');
    }
    setIsConnecting(true);
    try {
      const newStatus = await rnsService.connectToAddress(selectedAddress);
      setStatus({ ...newStatus });
      toast.success('Connected to RNode via Bluetooth');
    } catch (error: any) {
      toast.error('Connection failed', { description: error.message });
    } finally {
      setIsConnecting(false);
    }
  };

  const handleDisconnect = async () => {
    await rnsService.disconnect();
    setStatus(rnsService.getStatus());
    toast.info('Disconnected from RNode');
  };

  const handleStartRNS = async () => {
    setIsStarting(true);
    try {
      const hash = await rnsService.startRNS(config);
      setStatus(rnsService.getStatus());
      toast.success('RNS Stack Started', { description: `Local Hash: ${hash}` });
    } catch (error: any) {
      toast.error('Failed to start RNS', { description: error.message });
    } finally {
      setIsStarting(false);
    }
  };

  const handleSyncAll = async () => {
    if (!config.destinationHex) {
      return toast.error('Please enter a destination hash');
    }

    setIsSyncing(true);
    setSyncProgress(0);
    
    try {
      for (let i = 0; i < records.length; i++) {
        await rnsService.syncRecord(records[i], config.destinationHex);
        setSyncProgress(Math.round(((i + 1) / records.length) * 100));
      }
      toast.success('All records synced via RNS/LXMF');
    } catch (error: any) {
      toast.error('Sync failed', { description: error.message });
    } finally {
      setIsSyncing(false);
    }
  };

  const handleAnnounce = async () => {
    try {
      await rnsService.announce(config.nickname);
      setStatus(rnsService.getStatus());
      toast.success('Announce sent to Reticulum network');
    } catch (error: any) {
      toast.error('Announce failed', { description: error.message });
    }
  };

  return (
    <div className="p-4 space-y-6 max-w-2xl mx-auto pb-24">
      <div className="flex items-center gap-4 mb-2">
        <Button variant="ghost" size="icon" onClick={onBack} className="rounded-full bg-white shadow-sm border border-gray-100">
          <ArrowLeft className="w-5 h-5 text-gray-600" />
        </Button>
        <div>
          <h2 className="text-2xl font-black text-gray-900 tracking-tight">RNS Bridge</h2>
          <p className="text-gray-500 text-xs font-bold uppercase tracking-widest">Off-grid LoRa Sync</p>
        </div>
      </div>

      {/* GLOBAL DIAGNOSTICS - MOVED TO TOP FOR ABSOLUTE VISIBILITY */}
      <div className="bg-black p-4 rounded-3xl border-4 border-green-500 shadow-2xl ring-4 ring-black/20">
        <div className="flex items-center gap-2 mb-2">
          <Activity className="w-5 h-5 text-green-400" />
          <span className="text-[11px] font-black text-white uppercase tracking-widest">System Live Diagnostics</span>
        </div>
        <div className="bg-gray-900/50 p-3 rounded-xl border border-gray-800">
          <p className="text-sm font-mono font-bold text-green-400 animate-pulse break-all leading-relaxed">
            {status.statusMessage || "SYSTEM READY - WAITING FOR ACTION..."}
          </p>
        </div>
      </div>

      {/* Connection Status */}
      <Card className="border-none shadow-lg rounded-3xl overflow-hidden bg-white">
        <CardContent className="p-6">
          <div className="flex flex-col gap-4 mb-6">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className={`w-12 h-12 rounded-2xl flex items-center justify-center ${status.isConnected ? 'bg-green-50' : 'bg-gray-50'}`}>
                  {status.isConnected ? <Bluetooth className="w-6 h-6 text-green-600" /> : <BluetoothOff className="w-6 h-6 text-gray-400" />}
                </div>
                <div>
                  <h3 className="font-black text-gray-900">RNode Connection</h3>
                  <p className="text-xs font-medium text-gray-500">
                    {status.isConnected ? `Connected to ${status.device}` : 'Select RNode device'}
                  </p>
                </div>
              </div>
              <Button 
                onClick={status.isConnected ? handleDisconnect : handleConnect}
                disabled={isConnecting}
                variant={status.isConnected ? "outline" : "default"}
                className={`rounded-xl font-bold ${status.isConnected ? 'border-red-100 text-red-600 hover:bg-red-50' : 'bg-primary-600'}`}
              >
                {isConnecting ? 'Connecting...' : status.isConnected ? 'Disconnect' : 'Connect BT'}
              </Button>
            </div>

            {!status.isConnected && devices.length > 0 && (
              <div className="space-y-2">
                <Label className="text-[10px] font-bold uppercase tracking-widest text-gray-400">Paired Devices</Label>
                <select 
                  className="w-full h-12 rounded-xl border border-gray-100 bg-gray-50 px-4 text-sm font-bold text-gray-700"
                  value={selectedAddress}
                  onChange={(e) => setSelectedAddress(e.target.value)}
                >
                  {devices.map(device => (
                    <option key={device.address} value={device.address}>
                      {device.name} ({device.address})
                    </option>
                  ))}
                </select>
              </div>
            )}
            
            {!status.isConnected && devices.length === 0 && (
              <p className="text-[10px] font-bold text-orange-500 bg-orange-50 p-3 rounded-xl border border-orange-100">
                No paired Bluetooth devices found. Please pair your RNode in Android settings first.
              </p>
            )}
          </div>

          <AnimatePresence>
            {status.isConnected && (
              <motion.div 
                initial={{ height: 0, opacity: 0 }}
                animate={{ height: 'auto', opacity: 1 }}
                exit={{ height: 0, opacity: 0 }}
                className="space-y-4 overflow-hidden"
              >
                <div className="h-px bg-gray-100" />
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className={`w-2 h-2 rounded-full ${status.isRnsRunning ? 'bg-green-500 animate-pulse' : 'bg-gray-300'}`} />
                    <span className="text-xs font-bold text-gray-700 uppercase tracking-wider">RNS Stack</span>
                  </div>
                  <Button 
                    size="sm" 
                    onClick={handleStartRNS} 
                    disabled={isStarting || status.isRnsRunning}
                    className="rounded-lg bg-gray-900 text-white font-bold text-[10px] h-8 px-4"
                  >
                    {isStarting ? 'Starting...' : status.isRnsRunning ? 'Running' : 'Start Stack'}
                  </Button>
                </div>
                
                {status.localHash && (
                  <div className="bg-gray-50 p-3 rounded-xl border border-gray-100 flex items-center justify-between">
                    <span className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">Local Identity</span>
                    <code className="text-xs font-mono font-bold text-primary-600">{status.localHash}</code>
                  </div>
                )}
              </motion.div>
            )}
          </AnimatePresence>
        </CardContent>
      </Card>

      {/* RNode Tuning */}
      <Card className="border-none shadow-lg rounded-3xl overflow-hidden bg-white">
        <CardHeader className="p-6 pb-2">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-orange-50 rounded-xl flex items-center justify-center">
                <Settings className="w-5 h-5 text-orange-600" />
              </div>
              <div>
                <CardTitle className="text-lg font-black">RNode Tuning</CardTitle>
                <CardDescription className="text-xs font-medium">LoRa Physical Layer Config</CardDescription>
              </div>
            </div>
            <Button 
              variant="ghost" 
              size="sm" 
              onClick={() => setConfig({
                ...config,
                frequency: 433000000,
                bandwidth: 125000,
                txPower: 17,
                spreadingFactor: 8,
                codingRate: 6
              })}
              className="text-[10px] font-black text-primary-600 hover:text-primary-700 hover:bg-primary-50"
            >
              RESET DEFAULTS
            </Button>
          </div>
        </CardHeader>
        <CardContent className="p-6 space-y-6">
          <div className="space-y-4">
            <div className="space-y-2">
              <div className="flex justify-between">
                <Label className="text-xs font-bold uppercase tracking-wider text-gray-500">Frequency (Hz)</Label>
                <span className="text-xs font-mono font-bold text-primary-600">{(config.frequency / 1000000).toFixed(1)} MHz</span>
              </div>
              <Input 
                type="number" 
                value={config.frequency}
                onChange={(e) => setConfig({ ...config, frequency: parseInt(e.target.value) })}
                className="rounded-xl border-gray-200 h-11 font-mono text-sm text-gray-900 bg-white"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label className="text-xs font-bold uppercase tracking-wider text-gray-500">Bandwidth</Label>
                <select 
                  className="w-full h-11 rounded-xl border border-gray-200 bg-white px-3 text-sm font-bold text-gray-900"
                  value={config.bandwidth}
                  onChange={(e) => setConfig({ ...config, bandwidth: parseInt(e.target.value) })}
                >
                  <option value={125000}>125 kHz</option>
                  <option value={250000}>250 kHz</option>
                  <option value={500000}>500 kHz</option>
                </select>
              </div>
              <div className="space-y-2">
                <Label className="text-xs font-bold uppercase tracking-wider text-gray-500">TX Power (dBm)</Label>
                <Input 
                  type="number" 
                  value={config.txPower}
                  onChange={(e) => setConfig({ ...config, txPower: parseInt(e.target.value) })}
                  className="rounded-xl border-gray-200 h-11 font-mono text-sm text-gray-900 bg-white"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label className="text-xs font-bold uppercase tracking-wider text-gray-500">Spreading Factor</Label>
                <select 
                  className="w-full h-11 rounded-xl border border-gray-200 bg-white px-3 text-sm font-bold text-gray-900"
                  value={config.spreadingFactor}
                  onChange={(e) => setConfig({ ...config, spreadingFactor: parseInt(e.target.value) })}
                >
                  {[7, 8, 9, 10, 11, 12].map(sf => (
                    <option key={sf} value={sf}>SF{sf}</option>
                  ))}
                </select>
              </div>
              <div className="space-y-2">
                <Label className="text-xs font-bold uppercase tracking-wider text-gray-500">Coding Rate</Label>
                <select 
                  className="w-full h-11 rounded-xl border border-gray-200 bg-white px-3 text-sm font-bold text-gray-900"
                  value={config.codingRate}
                  onChange={(e) => setConfig({ ...config, codingRate: parseInt(e.target.value) })}
                >
                  {[5, 6, 7, 8].map(cr => (
                    <option key={cr} value={cr}>CR{cr}</option>
                  ))}
                </select>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Sync via LXMF */}
      <Card className="border-none shadow-lg rounded-3xl overflow-hidden bg-white">
        <CardHeader className="p-6 pb-2">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-blue-50 rounded-xl flex items-center justify-center">
              <Zap className="w-5 h-5 text-blue-600" />
            </div>
            <div>
              <CardTitle className="text-lg font-black">LXMF Sync</CardTitle>
              <CardDescription className="text-xs font-medium">Off-grid Data Exchange</CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent className="p-6 space-y-6">
          <div className="space-y-4">
            <div className="space-y-2">
              <Label className="text-xs font-bold uppercase tracking-wider text-gray-500">Destination Hash (Hex)</Label>
              <div className="relative">
                <Send className="absolute left-3 top-3 w-5 h-5 text-gray-400" />
                <Input 
                  placeholder="e.g. 8b4f2c..." 
                  className="pl-10 h-12 rounded-xl border-gray-200 font-mono text-sm text-gray-900 bg-white"
                  value={config.destinationHex}
                  onChange={(e) => setConfig({ ...config, destinationHex: e.target.value })}
                />
              </div>
            </div>

            <div className="flex gap-2">
              <Button 
                onClick={handleAnnounce}
                disabled={!status.isRnsRunning}
                variant="outline"
                className="flex-1 h-12 rounded-xl border-gray-100 font-bold text-xs"
              >
                <Activity className="w-4 h-4 mr-2" /> Announce
              </Button>
              <Button 
                onClick={handleSyncAll}
                disabled={!status.isRnsRunning || isSyncing || records.length === 0}
                className="flex-[2] h-12 rounded-xl bg-primary-600 font-bold text-xs shadow-lg"
              >
                {isSyncing ? (
                  <span className="flex items-center gap-2">
                    <RefreshCw className="w-4 h-4 animate-spin" /> Syncing {syncProgress}%
                  </span>
                ) : (
                  <span className="flex items-center gap-2">
                    <Zap className="w-4 h-4" /> Sync {records.length} Records
                  </span>
                )}
              </Button>
            </div>

            {isSyncing && (
              <div className="w-full bg-gray-100 h-1.5 rounded-full overflow-hidden">
                <motion.div 
                  className="bg-primary-600 h-full"
                  initial={{ width: 0 }}
                  animate={{ width: `${syncProgress}%` }}
                />
              </div>
            )}
          </div>

          <div className="flex items-start gap-3 p-4 bg-blue-50/50 rounded-2xl border border-blue-100">
            <AlertCircle className="w-4 h-4 text-blue-500 mt-0.5" />
            <p className="text-[10px] text-blue-700 font-medium leading-relaxed">
              LXMF (Lightweight eXchange Message Format) allows sending harvest data over LoRa without cellular coverage. Photos will be compressed for transmission.
            </p>
          </div>
        </CardContent>
      </Card>

      <div className="text-center pt-4">
        <p className="text-[10px] text-gray-400 font-bold uppercase tracking-widest">Powered by Reticulum Network Stack</p>
      </div>
    </div>
  );
}
