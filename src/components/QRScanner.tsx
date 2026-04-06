import { useState, useEffect } from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Camera as CameraIcon, QrCode, X } from 'lucide-react';
import { toast } from 'sonner';
import { BarcodeScanner, BarcodeFormat } from '@capacitor-mlkit/barcode-scanning';
import { Capacitor } from '@capacitor/core';

type QRScannerProps = {
  onScan: (data: { name: string; collectionPoint: string }) => void;
  onCancel?: () => void;
};

export default function QRScanner({ onScan, onCancel }: QRScannerProps) {
  const [isScanning, setIsScanning] = useState(false);
  const isNative = Capacitor.isNativePlatform() || Capacitor.getPlatform() !== 'web';

  const checkPermissions = async () => {
    if (isNative) {
      try {
        const status = await BarcodeScanner.checkPermissions();
        if (status.camera !== 'granted') {
          const request = await BarcodeScanner.requestPermissions();
          return request.camera === 'granted';
        }
        return true;
      } catch (err) {
        console.error('QR Permission check failed:', err);
        return false;
      }
    }
    return true;
  };

  // Reset scanning state when component key changes
  useEffect(() => {
    setIsScanning(false);
  }, []);

  const startScanning = async () => {
    // Try Capacitor Barcode Scanner (Native)
    try {
      const hasPermission = await checkPermissions();
      if (!hasPermission) {
        toast.error("Permission Denied", { description: "Camera permission is required to scan QR codes." });
        return;
      }

      // On native, this opens a full-screen scanner
      const { barcodes } = await BarcodeScanner.scan({
        formats: [BarcodeFormat.QrCode],
      });

      if (barcodes.length > 0) {
        handleScanResult(barcodes[0].displayValue);
      }
    } catch (nativeError: any) {
      console.error('Scanner error:', nativeError);
      if (nativeError.message !== 'User cancelled scan' && nativeError.message !== 'User cancelled') {
        toast.error("Scanner Error", { description: "Could not start the native scanner." });
      }
    }
  };

  const stopScanning = () => {
    setIsScanning(false);
  };

  const handleScanResult = (result: string | null) => {
    if (!result) return;
    
    try {
      // Parse QR code data (format: "Name, CP1")
      const parts = result.split(',');
      if (parts.length < 2) {
        throw new Error('Invalid QR code format');
      }
      
      const name = parts[0].trim();
      const collectionPoint = parts[1].trim();
      
      onScan({ name, collectionPoint });
      
      toast.success("QR Code Scanned Successfully", {
        description: `Harvester: ${name}, Collection Point: ${collectionPoint}`,
      });
      
      stopScanning();
    } catch (error) {
      toast.error("Invalid QR Code", {
        description: "The QR code format is not valid. Expected format: Name, CP1",
      });
    }
  };

  return (
    <Card className="w-full max-w-md mx-auto overflow-hidden border-none shadow-lg">
      <CardContent className="p-6">
        <div className="flex flex-col items-center">
          <div className="relative w-full aspect-square bg-gray-50 rounded-2xl overflow-hidden border-2 border-dashed border-gray-200 flex items-center justify-center mb-6">
            <div className="flex flex-col items-center justify-center h-full p-8">
              <div className="w-20 h-20 bg-primary-50 rounded-full flex items-center justify-center mb-4">
                <QrCode className="w-10 h-10 text-primary-600" />
              </div>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">Scan Harvester QR</h3>
              <p className="text-gray-500 text-center text-sm">
                Position the QR code within the frame to automatically capture harvester data
              </p>
            </div>
          </div>

          <div className="flex gap-3 w-full">
            {onCancel && (
              <Button 
                variant="outline"
                onClick={onCancel}
                className="flex-1 h-12 rounded-xl"
              >
                <X className="mr-2 h-4 w-4" />
                Cancel
              </Button>
            )}
            <Button 
              onClick={startScanning}
              className="flex-1 h-12 rounded-xl bg-primary-600 hover:bg-primary-700"
              size="lg"
            >
              <CameraIcon className="mr-2 h-4 w-4" />
              Start Scanning
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
