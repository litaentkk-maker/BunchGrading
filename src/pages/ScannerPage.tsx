import QRScanner from '@/src/components/QRScanner';
import ActivityList from '@/src/components/ActivityList';
import { HarvestRecord } from '@/src/types';

interface ScannerPageProps {
  onScan: (data: { name: string; collectionPoint: string }) => void;
  recentRecords: HarvestRecord[];
}

export default function ScannerPage({ onScan, recentRecords }: ScannerPageProps) {
  return (
    <div className="p-4 space-y-8 max-w-2xl mx-auto pb-20">
      <div className="text-center space-y-2">
        <h2 className="text-2xl font-black text-gray-900 tracking-tight">Ready to Scan</h2>
        <p className="text-gray-500 text-sm font-medium">Scan a harvester's QR code to start a new entry</p>
      </div>

      <QRScanner onScan={onScan} />

      <div className="bg-white p-6 rounded-3xl shadow-sm border border-gray-100">
        <ActivityList records={recentRecords} title="Today's Harvests" />
      </div>
    </div>
  );
}
