import { format } from 'date-fns';
import { MapPin, Calendar, Clock, User, Box } from 'lucide-react';
import { HarvestRecord } from '@/src/types';
import { Card, CardContent } from '@/components/ui/card';

interface ActivityListProps {
  records: HarvestRecord[];
  title?: string;
  onEdit?: (record: HarvestRecord) => void;
}

export default function ActivityList({ records, title = "Recent Activity", onEdit }: ActivityListProps) {
  if (records.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center p-12 text-gray-400 bg-gray-50 rounded-2xl border-2 border-dashed border-gray-200">
        <Box className="w-12 h-12 mb-4 opacity-20" />
        <p className="text-sm font-medium">No harvest records found yet</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between px-1">
        <h3 className="text-lg font-bold text-gray-900">{title}</h3>
        <span className="text-xs font-medium text-primary-600 bg-primary-50 px-2 py-1 rounded-full">
          {records.length} {records.length === 1 ? 'entry' : 'entries'}
        </span>
      </div>
      
      <div className="space-y-3 max-h-[500px] overflow-y-auto pr-1 custom-scrollbar">
        {records.map((record) => (
          <Card 
            key={record.id} 
            onClick={() => onEdit?.(record)}
            className={`overflow-hidden border-2 border-transparent hover:border-primary-500 active:border-primary-600 shadow-sm hover:shadow-md transition-all bg-white rounded-2xl group ${onEdit ? 'cursor-pointer active:scale-[0.98]' : ''}`}
          >
            <CardContent className="p-0 flex items-stretch">
              <div className="w-24 sm:w-32 aspect-square bg-gray-100 flex-shrink-0 relative">
                <img 
                  src={record.photoUrl} 
                  alt="Bunch" 
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                  referrerPolicy="no-referrer"
                />
                <div className="absolute bottom-0 left-0 right-0 bg-black/40 backdrop-blur-sm text-white text-[10px] font-bold py-1 px-2 text-center">
                  {record.bunchCount} Bunches
                </div>
              </div>
              
              <div className="flex-1 p-3 flex flex-col justify-between min-w-0">
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <h4 className="text-sm font-bold text-gray-900 truncate flex items-center gap-1.5">
                      <User className="w-3.5 h-3.5 text-primary-500" /> {record.harvesterName}
                    </h4>
                    <span className="text-[10px] font-bold text-primary-700 bg-primary-50 px-1.5 py-0.5 rounded uppercase tracking-wider">
                      {record.collectionPoint}
                    </span>
                  </div>
                  
                  <div className="flex flex-wrap gap-x-3 gap-y-1 mt-2">
                    <div className="flex items-center gap-1 text-[11px] text-gray-500">
                      <Calendar className="w-3 h-3" /> {format(record.timestamp, 'MMM d, yyyy')}
                    </div>
                    <div className="flex items-center gap-1 text-[11px] text-gray-500">
                      <Clock className="w-3 h-3" /> {format(record.timestamp, 'h:mm a')}
                    </div>
                  </div>
                </div>
                
                {record.location && (
                  <div className="flex items-center gap-1 text-[10px] text-gray-400 mt-2 truncate">
                    <MapPin className="w-3 h-3 text-red-400" />
                    {record.location.latitude.toFixed(4)}, {record.location.longitude.toFixed(4)}
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
