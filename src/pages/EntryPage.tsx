import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Slider } from '@/components/ui/slider';
import { Label } from '@/components/ui/label';
import { User, MapPin, Box, Save, ArrowLeft, CheckCircle2 } from 'lucide-react';
import { toast } from 'sonner';

interface EntryPageProps {
  photoData: { photoUrl: string; location?: { latitude: number; longitude: number } };
  initialBunchCount?: number;
  collectionPoint?: string;
  onSave: (bunchCount: number) => Promise<void>;
  onCancel: () => void;
}

export default function EntryPage({ photoData, initialBunchCount, collectionPoint, onSave, onCancel }: EntryPageProps) {
  const [bunchCount, setBunchCount] = useState(initialBunchCount ?? 1);
  const [isSaving, setIsSaving] = useState(false);

  const handleSave = async () => {
    if (bunchCount <= 0) {
      return toast.error("Invalid count", { description: "Bunch count must be at least 1" });
    }

    setIsSaving(true);
    try {
      await onSave(bunchCount);
      toast.success("Entry saved successfully", { 
        description: `Recorded ${bunchCount} bunches`,
        icon: <CheckCircle2 className="w-5 h-5 text-green-500" />
      });
    } catch (error: any) {
      toast.error("Failed to save entry", { description: error.message });
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="p-4 space-y-6 max-w-2xl mx-auto pb-24">
      <div className="flex items-center gap-4 mb-2">
        <Button variant="ghost" size="icon" onClick={onCancel} className="rounded-full bg-white shadow-sm border border-gray-100">
          <ArrowLeft className="w-5 h-5 text-gray-600" />
        </Button>
        <div>
          <h2 className="text-2xl font-black text-gray-900 tracking-tight">Bunch Data</h2>
          <p className="text-gray-500 text-xs font-bold uppercase tracking-widest">Input harvest details</p>
        </div>
      </div>

      <Card className="border-none shadow-xl rounded-3xl overflow-hidden bg-white">
        <CardHeader className="bg-primary-600 text-white p-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 bg-white/20 backdrop-blur-md rounded-xl flex items-center justify-center shadow-inner">
                <Box className="w-6 h-6" />
              </div>
              <div>
                <CardTitle className="text-xl font-black">Bunches Photo</CardTitle>
                <div className="flex items-center gap-3 mt-1">
                  <div className="flex items-center gap-1 text-primary-100 text-[10px] font-bold uppercase tracking-wider">
                    <MapPin className="w-3 h-3" /> {photoData.location ? `Lat: ${photoData.location.latitude.toFixed(4)}, Lng: ${photoData.location.longitude.toFixed(4)}` : 'Location not available'}
                  </div>
                  {collectionPoint && (
                    <div className="flex items-center gap-1 text-primary-100 text-[10px] font-bold uppercase tracking-wider bg-white/10 px-1.5 py-0.5 rounded">
                      <Box className="w-3 h-3" /> {collectionPoint}
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          <div className="aspect-[4/3] w-full bg-gray-100 relative">
            <img 
              src={photoData.photoUrl} 
              alt="Captured harvest" 
              className="w-full h-full object-cover"
            />
            <div className="absolute bottom-4 right-4 bg-black/50 backdrop-blur-md text-white px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-widest">
              Evidence Captured
            </div>
          </div>
        </CardContent>
      </Card>

      <Card className="border-none shadow-lg rounded-3xl overflow-hidden bg-white">
        <CardContent className="p-6 space-y-8">
          <div className="space-y-6">
            <div className="flex items-center justify-between">
              <Label className="text-sm font-black text-gray-900 flex items-center gap-2">
                <Box className="w-4 h-4 text-primary-600" /> Number of Bunches
              </Label>
              <div className="bg-primary-50 px-3 py-1 rounded-full">
                <span className="text-lg font-black text-primary-700">{bunchCount}</span>
              </div>
            </div>
            
            <div className="px-2 touch-pan-y">
              <Slider 
                value={[bunchCount]} 
                onValueChange={(val) => {
                  if (Array.isArray(val) && val.length > 0) {
                    setBunchCount(val[0]);
                  } else if (typeof val === 'number') {
                    setBunchCount(val);
                  }
                }} 
                max={200} 
                min={1} 
                step={1}
                className="py-4 cursor-pointer"
              />
            </div>

            <div className="flex items-center gap-3">
              <div className="flex-1">
                <Label htmlFor="manual-count" className="text-[10px] font-bold uppercase tracking-wider text-gray-400 mb-1.5 block">Manual Input</Label>
                <Input 
                  id="manual-count"
                  type="number" 
                  value={bunchCount} 
                  onChange={(e) => {
                    const val = parseInt(e.target.value);
                    if (!isNaN(val)) {
                      setBunchCount(Math.max(1, Math.min(200, val)));
                    } else if (e.target.value === '') {
                      // Allow empty input while typing, but handle it on blur or just default to 1
                      setBunchCount(1);
                    }
                  }}
                  className="h-12 rounded-xl border-gray-200 font-bold text-lg focus:ring-primary-500"
                />
              </div>
              <div className="flex gap-2 mt-5">
                {[5, 10, 20].map(val => (
                  <Button 
                    key={val}
                    variant="outline" 
                    size="sm" 
                    onClick={() => setBunchCount(prev => Math.min(200, prev + val))}
                    className="h-12 w-12 rounded-xl border-gray-200 font-bold text-primary-600 hover:bg-primary-50"
                  >
                    +{val}
                  </Button>
                ))}
              </div>
            </div>
          </div>

          <Button 
            onClick={handleSave} 
            className="w-full h-14 rounded-2xl bg-primary-600 hover:bg-primary-700 shadow-xl font-black text-lg uppercase tracking-widest gap-3"
            disabled={isSaving || !photoData.photoUrl}
          >
            {isSaving ? (
              <>Saving Record...</>
            ) : (
              <>
                <Save className="w-6 h-6" />
                Save Harvest
              </>
            )}
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
