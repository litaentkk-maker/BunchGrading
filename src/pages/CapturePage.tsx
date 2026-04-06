import PhotoCapture from '@/src/components/PhotoCapture';
import ActivityList from '@/src/components/ActivityList';
import { HarvestRecord } from '@/src/types';

interface CapturePageProps {
  onCapture: (photoUrl: string, location?: { latitude: number; longitude: number }) => void;
  onEdit: (record: HarvestRecord) => void;
  onOpenRNS: () => void;
  recentRecords: HarvestRecord[];
}

export default function CapturePage({ onCapture, onEdit, onOpenRNS, recentRecords }: CapturePageProps) {
  return (
    <div className="p-4 space-y-8 max-w-2xl mx-auto pb-20">
      <div className="flex items-center justify-between">
        <div className="space-y-1">
          <h2 className="text-2xl font-black text-gray-900 tracking-tight">Capture Harvest</h2>
          <p className="text-gray-500 text-xs font-bold uppercase tracking-widest">Start a new entry</p>
        </div>
        <button 
          onClick={onOpenRNS}
          className="w-12 h-12 bg-white rounded-2xl shadow-sm border border-gray-100 flex items-center justify-center text-primary-600 hover:bg-primary-50 transition-colors"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-zap"><path d="M4 14.5 14 3l-2.5 9H20L10 21l2.5-9H4Z"/></svg>
        </button>
      </div>

      <PhotoCapture onCapture={onCapture} />

      <div className="bg-white p-6 rounded-3xl shadow-sm border border-gray-100">
        <ActivityList records={recentRecords} title="Today's Harvests" onEdit={onEdit} />
      </div>
    </div>
  );
}
