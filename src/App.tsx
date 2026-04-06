import { useState, useEffect, Component, ErrorInfo, ReactNode } from 'react';
import { Toaster, toast } from 'sonner';
import { HarvestRecord, UserProfile } from '@/src/types';
import { getRecordsLocal, saveRecordLocal, saveRecordsLocal } from '@/lib/storage';
import Header from '@/src/components/Header';
import LoginPage from '@/src/pages/LoginPage';
import CapturePage from '@/src/pages/CapturePage';
import EntryPage from '@/src/pages/EntryPage';
import CalendarPage from '@/src/pages/CalendarPage';
import SettingsPage from '@/src/pages/SettingsPage';
import RNSPage from '@/src/pages/RNSPage';
import { Calendar, Camera, LogOut, AlertTriangle, Settings, Zap } from 'lucide-react';
import { Button } from '@/components/ui/button';
import Papa from 'papaparse';

// Error Boundary Component
class ErrorBoundary extends Component<{ children: ReactNode }, { hasError: boolean, error: Error | null }> {
  constructor(props: { children: ReactNode }) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error) {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error("Uncaught error:", error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen bg-gray-50 flex items-center justify-center p-6">
          <div className="bg-white p-8 rounded-3xl shadow-xl max-w-md w-full text-center space-y-4">
            <div className="w-16 h-16 bg-red-50 rounded-full flex items-center justify-center mx-auto">
              <AlertTriangle className="w-8 h-8 text-red-500" />
            </div>
            <h2 className="text-xl font-black text-gray-900">Application Error</h2>
            <p className="text-gray-500 text-sm">{this.state.error?.message || "Something went wrong."}</p>
            <Button onClick={() => window.location.reload()} className="w-full h-12 rounded-xl bg-primary-600">
              Reload Application
            </Button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

type Page = 'capture' | 'entry' | 'calendar' | 'settings' | 'rns';

export default function App() {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [currentPage, setCurrentPage] = useState<Page>('capture');
  const [records, setRecords] = useState<HarvestRecord[]>([]);
  const [capturedPhotoData, setCapturedPhotoData] = useState<{ photoUrl: string; location?: { latitude: number; longitude: number } } | null>(null);
  const [editingRecord, setEditingRecord] = useState<HarvestRecord | null>(null);
  const [nextCollectionPoint, setNextCollectionPoint] = useState<string>('');
  const [isOnline, setIsOnline] = useState(navigator.onLine);
  const [lastSync, setLastSync] = useState<string>(new Date().toLocaleTimeString());
  const [isAuthReady, setIsAuthReady] = useState(false);

  useEffect(() => {
    // Network status listeners
    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    // Mock Auth Check
    const storedUser = localStorage.getItem('palm_harvest_user');
    if (storedUser) {
      setUser(JSON.parse(storedUser));
    }
    
    // Load initial records
    const initialRecords = getRecordsLocal();
    
    // Migration: Fix records with "Main Station"
    let hasChanges = false;
    const userIds = Array.from(new Set(initialRecords.map(r => r.harvesterUid)));
    const migratedRecords = [...initialRecords];
    
    userIds.forEach(uid => {
      const userRecords = migratedRecords
        .filter(r => r.harvesterUid === uid)
        .sort((a, b) => a.timestamp - b.timestamp);
      
      let currentSeq = 0;
      // First, find the highest sequence already assigned to this user (that isn't "Main Station")
      userRecords.forEach(r => {
        if (r.collectionPoint !== 'Main Station') {
          const name = r.harvesterName || 'Unknown';
          const prefix = name.substring(0, 3).toLowerCase();
          if (r.collectionPoint.startsWith(prefix)) {
             const num = parseInt(r.collectionPoint.replace(prefix, ''));
             if (!isNaN(num)) currentSeq = Math.max(currentSeq, num);
          }
        }
      });
      
      // Now assign to "Main Station" records
      userRecords.forEach(r => {
        if (r.collectionPoint === 'Main Station') {
          const name = r.harvesterName || 'Unknown';
          const prefix = name.substring(0, 3).toLowerCase();
          currentSeq++;
          r.collectionPoint = `${prefix}${currentSeq.toString().padStart(2, '0')}`;
          hasChanges = true;
        }
      });
    });
    
    if (hasChanges) {
      saveRecordsLocal(migratedRecords);
    }
    setRecords(migratedRecords);
    setIsAuthReady(true);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  const handleLogin = async (email: string, pass: string) => {
    // Mock login delay
    await new Promise(resolve => setTimeout(resolve, 800));
    const mockUser: UserProfile = {
      uid: 'mock-uid-' + Date.now(),
      email,
      displayName: email.split('@')[0],
      role: 'harvester',
      createdAt: new Date().toISOString()
    };
    setUser(mockUser);
    localStorage.setItem('palm_harvest_user', JSON.stringify(mockUser));
  };

  const handleRegister = async (email: string, pass: string, name: string) => {
    // Mock register delay
    await new Promise(resolve => setTimeout(resolve, 800));
    const mockUser: UserProfile = {
      uid: 'mock-uid-' + Date.now(),
      email,
      displayName: name,
      role: 'harvester',
      createdAt: new Date().toISOString()
    };
    setUser(mockUser);
    localStorage.setItem('palm_harvest_user', JSON.stringify(mockUser));
  };

  const handleResetPassword = async (email: string) => {
    // Mock reset
    await new Promise(resolve => setTimeout(resolve, 500));
    console.log('Resetting password for', email);
  };

  const handleLogout = async () => {
    setUser(null);
    localStorage.removeItem('palm_harvest_user');
  };

  const handleCapture = (photoUrl: string, location?: { latitude: number; longitude: number }) => {
    if (user) {
      const name = user.displayName || user.email.split('@')[0];
      const prefix = name.substring(0, 3).toLowerCase();
      const userRecords = records.filter(r => r.harvesterUid === user.uid);
      let maxNum = 0;
      userRecords.forEach(r => {
        if (r.collectionPoint.startsWith(prefix)) {
          const numPart = r.collectionPoint.replace(prefix, '');
          const num = parseInt(numPart);
          if (!isNaN(num)) maxNum = Math.max(maxNum, num);
        }
      });
      setNextCollectionPoint(`${prefix}${(maxNum + 1).toString().padStart(2, '0')}`);
    }
    setCapturedPhotoData({ photoUrl, location });
    setEditingRecord(null);
    setCurrentPage('entry');
  };

  const handleEdit = (record: HarvestRecord) => {
    setEditingRecord(record);
    setNextCollectionPoint(record.collectionPoint);
    setCapturedPhotoData({ photoUrl: record.photoUrl, location: record.location || undefined });
    setCurrentPage('entry');
  };

  const handleSaveHarvest = async (bunchCount: number) => {
    if (!capturedPhotoData || !user) return;

    const collectionPoint = nextCollectionPoint || 'Main Station';

    const recordToSave: HarvestRecord = editingRecord ? {
      ...editingRecord,
      bunchCount,
      collectionPoint,
    } : {
      id: 'rec-' + Date.now(),
      harvesterUid: user.uid,
      harvesterName: user.displayName || 'Unknown Harvester',
      collectionPoint,
      bunchCount,
      photoUrl: capturedPhotoData.photoUrl,
      timestamp: Date.now(),
      location: capturedPhotoData.location || null,
    };

    saveRecordLocal(recordToSave);
    
    if (editingRecord) {
      setRecords(prev => prev.map(r => r.id === recordToSave.id ? recordToSave : r));
    } else {
      setRecords(prev => [recordToSave, ...prev]);
    }

    setCapturedPhotoData(null);
    setEditingRecord(null);
    setNextCollectionPoint('');
    setCurrentPage('capture');
    setLastSync(new Date().toLocaleTimeString());
  };

  const handleExportCSV = () => {
    const csvData = records.map(r => ({
      Date: new Date(r.timestamp).toLocaleDateString(),
      Time: new Date(r.timestamp).toLocaleTimeString(),
      Harvester: r.harvesterName,
      CollectionPoint: r.collectionPoint,
      Bunches: r.bunchCount,
      Latitude: r.location?.latitude || '',
      Longitude: r.location?.longitude || '',
    }));

    const csv = Papa.unparse(csvData);
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', `harvest_report_${new Date().toISOString().split('T')[0]}.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handleExportSheets = () => {
    toast.info("Google Sheets Sync", { 
      description: "In a production environment, this would sync directly to your configured Google Sheet. For now, please use the CSV export in the Settings menu." 
    });
  };

  if (!isAuthReady) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (!user) {
    return (
      <ErrorBoundary>
        <LoginPage 
          onLogin={handleLogin} 
          onRegister={handleRegister} 
          onResetPassword={handleResetPassword} 
        />
        <Toaster position="top-center" richColors />
      </ErrorBoundary>
    );
  }

  return (
    <ErrorBoundary>
      <div className="min-h-screen bg-gray-50 flex flex-col relative">
        <Header 
          user={user} 
          onLogout={handleLogout} 
          onSettings={() => setCurrentPage('settings')} 
          isOnline={isOnline}
          lastSync={lastSync}
        />

        <main className="flex-1 w-full overflow-x-hidden pb-32">
          <div className="container mx-auto">
            {currentPage === 'capture' && (
              <CapturePage 
                onCapture={handleCapture} 
                onEdit={handleEdit}
                onOpenRNS={() => setCurrentPage('rns')}
                recentRecords={records.slice(0, 5)} 
              />
            )}

            {currentPage === 'entry' && capturedPhotoData && (
              <EntryPage 
                photoData={capturedPhotoData} 
                initialBunchCount={editingRecord?.bunchCount}
                collectionPoint={nextCollectionPoint}
                onSave={handleSaveHarvest} 
                onCancel={() => {
                  setCapturedPhotoData(null);
                  setEditingRecord(null);
                  setNextCollectionPoint('');
                  setCurrentPage('capture');
                }} 
              />
            )}

            {currentPage === 'calendar' && (
              <CalendarPage 
                records={records} 
                onExportCSV={handleExportCSV} 
                onExportSheets={handleExportSheets} 
                onEdit={handleEdit}
              />
            )}

            {currentPage === 'settings' && (
              <SettingsPage 
                user={user} 
                onBack={() => setCurrentPage('capture')} 
                onExportCSV={handleExportCSV}
                onExportSheets={handleExportSheets}
                onOpenRNS={() => setCurrentPage('rns')}
              />
            )}

            {currentPage === 'rns' && (
              <RNSPage 
                records={records} 
                onBack={() => setCurrentPage('settings')} 
              />
            )}
          </div>
        </main>

        {/* Bottom Navigation */}
        <nav className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 px-6 py-3 flex items-center justify-around z-50 shadow-[0_-4px_15px_rgba(0,0,0,0.08)]">
          <Button 
            variant="ghost" 
            onClick={() => setCurrentPage('capture')}
            className={`flex flex-col items-center gap-1 h-auto py-1 px-4 rounded-xl transition-all ${currentPage === 'capture' ? 'text-primary-600 bg-primary-50' : 'text-gray-400'}`}
          >
            <Camera className="w-6 h-6" />
            <span className="text-[10px] font-bold uppercase tracking-wider">Capture</span>
          </Button>
          
          <Button 
            variant="ghost" 
            onClick={() => setCurrentPage('calendar')}
            className={`flex flex-col items-center gap-1 h-auto py-1 px-4 rounded-xl transition-all ${currentPage === 'calendar' ? 'text-primary-600 bg-primary-50' : 'text-gray-400'}`}
          >
            <Calendar className="w-6 h-6" />
            <span className="text-[10px] font-bold uppercase tracking-wider">Calendar</span>
          </Button>

          <Button 
            variant="ghost" 
            onClick={() => setCurrentPage('settings')}
            className={`flex flex-col items-center gap-1 h-auto py-1 px-4 rounded-xl transition-all ${currentPage === 'settings' ? 'text-primary-600 bg-primary-50' : 'text-gray-400'}`}
          >
            <Settings className="w-6 h-6" />
            <span className="text-[10px] font-bold uppercase tracking-wider">Settings</span>
          </Button>
          
          <Button 
            variant="ghost" 
            onClick={handleLogout}
            className="flex flex-col items-center gap-1 h-auto py-1 px-4 rounded-xl text-gray-400"
          >
            <LogOut className="w-6 h-6" />
            <span className="text-[10px] font-bold uppercase tracking-wider">Logout</span>
          </Button>
        </nav>

        <Toaster position="top-center" richColors />
      </div>
    </ErrorBoundary>
  );
}
