import CalendarView from '@/src/components/CalendarView';
import { HarvestRecord } from '@/src/types';

interface CalendarPageProps {
  records: HarvestRecord[];
  onExportCSV: () => void;
  onExportSheets: () => void;
  onEdit: (record: HarvestRecord) => void;
}

export default function CalendarPage({ records, onExportCSV, onExportSheets, onEdit }: CalendarPageProps) {
  return (
    <div className="p-4 space-y-8 max-w-2xl mx-auto pb-24">
      <div className="text-center space-y-2">
        <h2 className="text-2xl font-black text-gray-900 tracking-tight">Harvest Calendar</h2>
        <p className="text-gray-500 text-sm font-medium">Track your performance and export data</p>
      </div>

      <CalendarView 
        records={records} 
        onExportCSV={onExportCSV} 
        onExportSheets={onExportSheets} 
        onEdit={onEdit}
      />
    </div>
  );
}
