import { Settings, User, LogOut, Wifi, WifiOff } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { UserProfile } from '@/src/types';

interface HeaderProps {
  user: UserProfile | null;
  onLogout: () => void;
  onSettings: () => void;
  isOnline: boolean;
  lastSync?: string;
}

export default function Header({ user, onLogout, onSettings, isOnline, lastSync }: HeaderProps) {
  return (
    <header className="bg-white border-b border-gray-200 sticky top-0 z-50 px-4 py-3 flex items-center justify-between shadow-sm">
      <div className="flex items-center gap-2">
        <div className="bg-primary-600 p-1.5 rounded-lg overflow-hidden">
          <img src="/logo.png" alt="Logo" className="w-6 h-6 object-cover" referrerPolicy="no-referrer" />
        </div>
        <div>
          <h1 className="text-lg font-bold text-gray-900 leading-none tracking-tight">PalmHarvest Pro</h1>
          <div className="flex items-center gap-1.5 mt-1">
            {isOnline ? (
              <span className="flex items-center gap-1 text-[10px] text-green-600 font-medium">
                <Wifi className="w-3 h-3" /> Online
              </span>
            ) : (
              <span className="flex items-center gap-1 text-[10px] text-red-600 font-medium">
                <WifiOff className="w-3 h-3" /> Offline
              </span>
            )}
            {lastSync && (
              <span className="text-[10px] text-gray-400">
                Sync: {lastSync}
              </span>
            )}
          </div>
        </div>
      </div>

      <div className="flex items-center gap-2">
        {user && (
          <div className="flex items-center gap-2 mr-2">
            <div className="text-right hidden sm:block">
              <p className="text-xs font-medium text-gray-900">{user.displayName || user.email}</p>
              <p className="text-[10px] text-gray-500">Harvester</p>
            </div>
            <div className="w-8 h-8 rounded-full bg-gray-100 flex items-center justify-center border border-gray-200 overflow-hidden">
              {user.photoURL ? (
                <img src={user.photoURL} alt="Avatar" className="w-full h-full object-cover" referrerPolicy="no-referrer" />
              ) : (
                <User className="w-4 h-4 text-gray-400" />
              )}
            </div>
          </div>
        )}
        <Button variant="ghost" size="icon" onClick={onSettings} className="text-gray-500">
          <Settings className="w-5 h-5" />
        </Button>
        <Button variant="ghost" size="icon" onClick={onLogout} className="text-gray-500">
          <LogOut className="w-5 h-5" />
        </Button>
      </div>
    </header>
  );
}
