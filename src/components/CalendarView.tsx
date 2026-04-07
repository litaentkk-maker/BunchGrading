import { useState } from 'react';
import Calendar from 'react-calendar';
import { format, isSameDay, startOfMonth, endOfMonth, endOfDay } from 'date-fns';
import { HarvestRecord } from '@/src/types';
import ActivityList from './ActivityList';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Download, FileSpreadsheet, TrendingUp, Calendar as CalendarIcon } from 'lucide-react';
import { Button } from '@/components/ui/button';
import 'react-calendar/dist/Calendar.css';

interface CalendarViewProps {
  records: HarvestRecord[];
  onExportCSV: () => void;
  onExportSheets: () => void;
  onEdit?: (record: HarvestRecord) => void;
  onDelete?: (recordId: string) => void;
}

export default function CalendarView({ records, onExportCSV, onExportSheets, onEdit, onDelete }: CalendarViewProps) {
  const [selectedDate, setSelectedDate] = useState<Date>(new Date());

  const filteredRecords = records.filter(record => 
    isSameDay(new Date(record.timestamp), selectedDate)
  );

  const totalToday = filteredRecords.reduce((sum, record) => sum + record.bunchCount, 0);
  const entriesToday = filteredRecords.length;
  
  const monthStart = startOfMonth(selectedDate);
  const dayEnd = endOfDay(selectedDate);
  const totalToDate = records
    .filter(record => {
      const date = new Date(record.timestamp);
      return date >= monthStart && date <= dayEnd;
    })
    .reduce((sum, record) => sum + record.bunchCount, 0);

  const getTileContent = ({ date, view }: { date: Date; view: string }) => {
    if (view === 'month') {
      const dayRecords = records.filter(record => isSameDay(new Date(record.timestamp), date));
      const dayTotal = dayRecords.reduce((sum, record) => sum + record.bunchCount, 0);
      
      if (dayTotal > 0) {
        return (
          <div className="text-[10px] font-bold text-primary-600 mt-1 bg-primary-50 rounded px-1 flex items-center justify-center">
            {dayTotal}
          </div>
        );
      }
    }
    return null;
  };

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Card className="bg-primary-600 text-white border-none shadow-lg rounded-2xl overflow-hidden relative">
          <CardContent className="p-5">
            <div className="flex items-center gap-2 mb-2 opacity-80">
              <TrendingUp className="w-4 h-4" />
              <p className="text-[10px] font-bold uppercase tracking-wider">Total Bunches Today</p>
            </div>
            <h3 className="text-3xl font-black">{totalToday}</h3>
            <p className="text-[10px] mt-1 opacity-70">{format(selectedDate, 'MMMM d, yyyy')}</p>
          </CardContent>
          <div className="absolute -right-4 -bottom-4 opacity-10">
            <TrendingUp className="w-24 h-24" />
          </div>
        </Card>

        <Card className="bg-white border-none shadow-lg rounded-2xl overflow-hidden relative">
          <CardContent className="p-5">
            <div className="flex items-center gap-2 mb-2 text-primary-600 opacity-80">
              <TrendingUp className="w-4 h-4" />
              <p className="text-[10px] font-bold uppercase tracking-wider">Total Entries Today</p>
            </div>
            <h3 className="text-3xl font-black text-gray-900">{entriesToday}</h3>
            <p className="text-[10px] mt-1 text-gray-400">Records for {format(selectedDate, 'MMM d')}</p>
          </CardContent>
        </Card>
        
        <Card className="bg-white border-none shadow-lg rounded-2xl overflow-hidden relative">
          <CardContent className="p-5">
            <div className="flex items-center gap-2 mb-2 text-primary-600 opacity-80">
              <CalendarIcon className="w-4 h-4" />
              <p className="text-[10px] font-bold uppercase tracking-wider">Total Bunches to Date</p>
            </div>
            <h3 className="text-3xl font-black text-gray-900">{totalToDate}</h3>
            <p className="text-[10px] mt-1 text-gray-400">Month of {format(selectedDate, 'MMMM yyyy')}</p>
          </CardContent>
          <div className="absolute -right-4 -bottom-4 opacity-5 text-primary-600">
            <CalendarIcon className="w-24 h-24" />
          </div>
        </Card>
      </div>

      <Card className="border-none shadow-lg rounded-2xl overflow-hidden bg-white">
        <CardHeader className="bg-gray-50 border-b border-gray-100 px-6 py-4">
          <div className="flex items-center justify-between">
            <CardTitle className="text-lg font-bold text-gray-900">Monthly Overview</CardTitle>
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={onExportCSV} className="h-8 text-[10px] font-bold uppercase tracking-wider rounded-lg border-gray-200">
                <Download className="w-3 h-3 mr-1.5" /> CSV
              </Button>
              <Button variant="outline" size="sm" onClick={onExportSheets} className="h-8 text-[10px] font-bold uppercase tracking-wider rounded-lg border-gray-200">
                <FileSpreadsheet className="w-3 h-3 mr-1.5" /> Sheets
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent className="p-4">
          <Calendar 
            onChange={(val) => setSelectedDate(val as Date)} 
            value={selectedDate}
            tileContent={getTileContent}
            className="w-full border-none font-sans"
          />
        </CardContent>
      </Card>

      <div className="bg-gray-50 p-4 rounded-2xl border border-gray-100">
        <ActivityList 
          records={filteredRecords} 
          title={`Entries for ${format(selectedDate, 'MMM d')}`} 
          onEdit={onEdit}
          onDelete={onDelete}
        />
      </div>
    </div>
  );
}
