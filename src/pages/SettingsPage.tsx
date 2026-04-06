import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { ArrowLeft, Cloud, Database, Shield, Info, RefreshCw } from 'lucide-react';
import { UserProfile } from '@/src/types';

interface SettingsPageProps {
  user: UserProfile;
  onBack: () => void;
  onExportCSV: () => void;
  onExportSheets: () => void;
  onOpenRNS: () => void;
}

export default function SettingsPage({ user, onBack, onExportCSV, onExportSheets, onOpenRNS }: SettingsPageProps) {
  return (
    <div className="p-4 space-y-6 max-w-2xl mx-auto pb-24">
      <div className="flex items-center gap-4 mb-2">
        <Button variant="ghost" size="icon" onClick={onBack} className="rounded-full bg-white shadow-sm border border-gray-100">
          <ArrowLeft className="w-5 h-5 text-gray-600" />
        </Button>
        <div>
          <h2 className="text-2xl font-black text-gray-900 tracking-tight">Settings</h2>
          <p className="text-gray-500 text-xs font-bold uppercase tracking-widest">Manage your application</p>
        </div>
      </div>

      <Card className="border-none shadow-lg rounded-3xl overflow-hidden bg-white">
        <CardHeader className="p-6 pb-2">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-orange-50 rounded-xl flex items-center justify-center">
              <RefreshCw className="w-5 h-5 text-orange-600" />
            </div>
            <div>
              <CardTitle className="text-lg font-black">Off-Grid Sync</CardTitle>
              <CardDescription className="text-xs font-medium">Reticulum Network Stack (RNS)</CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent className="p-6 space-y-6">
          <div className="flex items-center justify-between p-4 bg-orange-50/30 rounded-2xl border border-orange-100">
            <div className="space-y-1">
              <Label className="text-sm font-black text-gray-900">RNS Bridge</Label>
              <p className="text-[10px] text-gray-500 font-medium max-w-[200px]">
                Sync harvest data via LoRa when cellular network is unavailable.
              </p>
            </div>
            <Button 
              variant="default" 
              className="rounded-xl bg-orange-600 text-white font-bold text-xs hover:bg-orange-700 shadow-md"
              onClick={onOpenRNS}
            >
              Open Bridge
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card className="border-none shadow-lg rounded-3xl overflow-hidden bg-white">
        <CardHeader className="p-6 pb-2">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-green-50 rounded-xl flex items-center justify-center">
              <RefreshCw className="w-5 h-5 text-green-600" />
            </div>
            <div>
              <CardTitle className="text-lg font-black">Export Data</CardTitle>
              <CardDescription className="text-xs font-medium">Download your harvest history</CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent className="p-6 space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <Button 
              variant="outline" 
              className="h-24 flex flex-col gap-2 rounded-2xl border-gray-100 hover:bg-gray-50 hover:border-gray-200 transition-all"
              onClick={onExportCSV}
            >
              <div className="w-10 h-10 bg-gray-100 rounded-full flex items-center justify-center">
                <Database className="w-5 h-5 text-gray-600" />
              </div>
              <span className="text-xs font-black">Export CSV</span>
            </Button>

            <Button 
              variant="outline" 
              className="h-24 flex flex-col gap-2 rounded-2xl border-gray-100 hover:bg-gray-50 hover:border-gray-200 transition-all"
              onClick={onExportSheets}
            >
              <div className="w-10 h-10 bg-green-100 rounded-full flex items-center justify-center">
                <Cloud className="w-5 h-5 text-green-600" />
              </div>
              <span className="text-xs font-black">Google Sheets</span>
            </Button>
          </div>
          <p className="text-[10px] text-gray-400 text-center font-medium">
            CSV exports are immediate. Google Sheets sync requires an active cloud connection.
          </p>
        </CardContent>
      </Card>

      <Card className="border-none shadow-lg rounded-3xl overflow-hidden bg-white">
        <CardHeader className="p-6 pb-2">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-purple-50 rounded-xl flex items-center justify-center">
              <Database className="w-5 h-5 text-purple-600" />
            </div>
            <div>
              <CardTitle className="text-lg font-black">Data Management</CardTitle>
              <CardDescription className="text-xs font-medium">Local storage and exports</CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent className="p-6 space-y-4">
          <div className="flex items-center justify-between">
            <div className="space-y-0.5">
              <p className="text-sm font-bold text-gray-900">Local Records</p>
              <p className="text-[10px] text-gray-500">Currently stored on this device</p>
            </div>
            <p className="text-sm font-black text-gray-900">Active</p>
          </div>
          
          <div className="pt-2">
            <Button variant="ghost" className="w-full justify-start text-red-600 font-bold text-xs hover:bg-red-50 rounded-xl px-0">
              Clear Local Cache
            </Button>
          </div>
        </CardContent>
      </Card>

      <div className="text-center pt-4">
        <p className="text-[10px] text-gray-400 font-bold uppercase tracking-widest">PalmHarvest Pro v1.0.2</p>
      </div>
    </div>
  );
}
