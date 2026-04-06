import { useState, useRef, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Camera as CameraIcon, Image, RefreshCw, Check } from 'lucide-react';
import { toast } from 'sonner';
import { Camera, CameraResultType, CameraSource } from '@capacitor/camera';
import { Capacitor } from '@capacitor/core';
import { Geolocation } from '@capacitor/geolocation';

type PhotoCaptureProps = {
  onCapture: (photoUrl: string, location?: { latitude: number; longitude: number }) => void;
};

export default function PhotoCapture({ onCapture }: PhotoCaptureProps) {
  const [photoUrl, setPhotoUrl] = useState<string | null>(null);
  const [isCapturing, setIsCapturing] = useState(false);
  const videoRef = useRef<HTMLVideoElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  
  // More robust native check
  const isNative = Capacitor.isNativePlatform() || Capacitor.getPlatform() !== 'web';

  const checkPermissions = async () => {
    if (isNative) {
      try {
        const status = await Camera.checkPermissions();
        if (status.camera !== 'granted') {
          const request = await Camera.requestPermissions();
          return request.camera === 'granted';
        }
        return true;
      } catch (err) {
        console.error('Permission check failed:', err);
        return false;
      }
    }
    return true;
  };

  // Reset component state when key changes (component remounts)
  useEffect(() => {
    setPhotoUrl(null);
    setIsCapturing(false);
    stopMediaStream();
    return () => {
      stopMediaStream();
    };
  }, []);

  const stopMediaStream = () => {
    if (streamRef.current) {
      const tracks = streamRef.current.getTracks();
      tracks.forEach(track => track.stop());
      streamRef.current = null;
    }
  };

  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (e) => {
        const dataUrl = e.target?.result as string;
        handleCaptureComplete(dataUrl);
      };
      reader.readAsDataURL(file);
    }
  };

  const triggerFileUpload = () => {
    fileInputRef.current?.click();
  };

  const startCapture = async () => {
    console.log('Starting photo capture. Platform:', Capacitor.getPlatform(), 'isNative:', isNative);
    // 1. Try Capacitor Camera (Native UI) - Most reliable on Android/iOS
    try {
      const hasPermission = await checkPermissions();
      console.log('Camera permission status:', hasPermission);
      if (!hasPermission) {
        toast.error("Permission Denied", { description: "Camera permission is required to take photos." });
        return;
      }

      console.log('Attempting Capacitor Camera.getPhoto...');
      const image = await Camera.getPhoto({
        quality: 90,
        allowEditing: false,
        resultType: CameraResultType.DataUrl,
        source: CameraSource.Camera,
        saveToGallery: true,
        width: 1280,
        height: 720
      });

      if (image.dataUrl) {
        console.log('Capacitor Camera success');
        handleCaptureComplete(image.dataUrl);
        return; // Success!
      }
    } catch (nativeError: any) {
      console.warn('Capacitor Camera failed, trying browser fallback:', nativeError);
      // If user cancelled, don't fallback to browser camera
      if (nativeError.message === 'User cancelled photos app' || nativeError.message === 'User cancelled') {
        return;
      }
    }

    // 2. Browser Fallback (only if Capacitor Camera failed or is unavailable)
    try {
      console.log('Attempting browser camera fallback...');
      setIsCapturing(true);
      setPhotoUrl(null);
      
      if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
        let stream: MediaStream | null = null;
        
        // Strategy 1: Try with ideal environment facing mode
        try {
          console.log('Strategy 1: Environment facing mode');
          stream = await navigator.mediaDevices.getUserMedia({
            video: { 
              facingMode: { ideal: 'environment' },
              width: { ideal: 1280 },
              height: { ideal: 720 }
            }
          });
        } catch (err) {
          console.warn('Ideal environment camera failed:', err);
        }

        // Strategy 2: Try with any video device (simplest constraints)
        if (!stream) {
          try {
            stream = await navigator.mediaDevices.getUserMedia({
              video: true
            });
          } catch (err) {
            console.warn('Basic video access failed:', err);
          }
        }

        // Strategy 3: Enumerate devices and try the first video input
        if (!stream) {
          try {
            const devices = await navigator.mediaDevices.enumerateDevices();
            const videoDevices = devices.filter(device => device.kind === 'videoinput');
            
            if (videoDevices.length > 0) {
              stream = await navigator.mediaDevices.getUserMedia({
                video: { deviceId: { exact: videoDevices[0].deviceId } }
              });
            }
          } catch (err) {
            console.warn('Device enumeration fallback failed:', err);
          }
        }

        if (!stream) {
          throw new Error('No camera device could be found or accessed.');
        }
        
        streamRef.current = stream;
        
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          videoRef.current.play();
        }
      } else {
        throw new Error('Camera access is not supported in this browser.');
      }
    } catch (error: any) {
      console.error('Error accessing camera:', error);
      let message = "Unable to access your camera.";
      
      if (error.name === 'NotAllowedError' || error.name === 'PermissionDeniedError') {
        message = "Camera permission denied. Please allow access in your browser settings.";
      } else if (error.name === 'NotFoundError' || error.name === 'DevicesNotFoundError') {
        message = "No camera found on this device.";
      } else if (error.name === 'NotReadableError' || error.name === 'TrackStartError') {
        message = "Camera is already in use by another application.";
      } else if (error.message) {
        message = error.message;
      }

      toast.error("Camera access error", {
        description: `${message} Please try uploading a photo instead.`,
      });
      setIsCapturing(false);
    }
  };

  const capturePhoto = () => {
    if (videoRef.current) {
      const canvas = document.createElement('canvas');
      canvas.width = videoRef.current.videoWidth;
      canvas.height = videoRef.current.videoHeight;
      
      const ctx = canvas.getContext('2d');
      if (ctx) {
        ctx.drawImage(videoRef.current, 0, 0, canvas.width, canvas.height);
        const dataUrl = canvas.toDataURL('image/jpeg', 0.8);
        handleCaptureComplete(dataUrl);
      }
    }
  };

  const handleCaptureComplete = async (url: string) => {
    // Get geolocation
    if (isNative) {
      try {
        const position = await Geolocation.getCurrentPosition();
        const location = {
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
        };
        setPhotoUrl(url);
        onCapture(url, location);
        setIsCapturing(false);
        stopMediaStream();
        toast.success("Photo captured with location");
      } catch (error) {
        setPhotoUrl(url);
        onCapture(url);
        setIsCapturing(false);
        stopMediaStream();
        toast.warning("Photo captured without location");
      }
      return;
    }

    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          const location = {
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
          };
          setPhotoUrl(url);
          onCapture(url, location);
          setIsCapturing(false);
          stopMediaStream();
          toast.success("Photo captured with location", {
            description: `Lat: ${location.latitude.toFixed(4)}, Lng: ${location.longitude.toFixed(4)}`,
          });
        },
        (error) => {
          console.error('Error getting location:', error);
          setPhotoUrl(url);
          onCapture(url);
          setIsCapturing(false);
          stopMediaStream();
          toast.warning("Photo captured without location", {
            description: "Unable to get your current location.",
          });
        }
      );
    } else {
      setPhotoUrl(url);
      onCapture(url);
      setIsCapturing(false);
      stopMediaStream();
      toast.success("Photo captured", {
        description: "Bunch photo has been added to your record",
      });
    }
  };

  const retakePhoto = () => {
    setPhotoUrl(null);
    startCapture();
  };

  return (
    <Card className="w-full max-w-md mx-auto overflow-hidden border-none shadow-lg">
      <CardHeader className="bg-primary-50 px-6 py-4">
        <CardTitle className="text-lg font-bold text-primary-900 flex items-center gap-2">
          <CameraIcon className="w-5 h-5" /> Bunch Photo Evidence
        </CardTitle>
      </CardHeader>
      <CardContent className="p-6">
        <div className="flex flex-col items-center space-y-6">
          <div className="w-full aspect-[4/3] bg-gray-100 rounded-2xl overflow-hidden flex items-center justify-center border-2 border-dashed border-gray-200 relative">
            {isCapturing ? (
              <div className="relative w-full h-full bg-black">
                <video 
                  ref={videoRef}
                  className="absolute inset-0 w-full h-full object-cover"
                  autoPlay
                  playsInline
                  muted
                  poster="data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7"
                />
                <div className="absolute inset-0 border-2 border-white/30 pointer-events-none"></div>
              </div>
            ) : photoUrl ? (
              <div className="relative w-full h-full">
                <img 
                  src={photoUrl} 
                  alt="Captured bunch" 
                  className="w-full h-full object-cover"
                />
                <div className="absolute top-4 right-4 bg-green-500 text-white p-1.5 rounded-full shadow-lg">
                  <Check className="w-4 h-4" />
                </div>
              </div>
            ) : (
              <div className="flex flex-col items-center justify-center p-8">
                <div className="w-16 h-16 bg-gray-200 rounded-full flex items-center justify-center mb-4">
                  <Image className="w-8 h-8 text-gray-400" />
                </div>
                <p className="text-gray-500 text-center text-sm font-medium">Capture a photo of the harvested bunches for evidence</p>
              </div>
            )}
          </div>
          
          <div className="w-full flex flex-col gap-3">
            <input 
              type="file" 
              ref={fileInputRef} 
              className="hidden" 
              accept="image/*" 
              onChange={handleFileUpload}
            />
            
            {isCapturing ? (
              <Button 
                onClick={capturePhoto}
                className="w-full h-12 rounded-xl bg-primary-600 hover:bg-primary-700 shadow-md"
              >
                <CameraIcon className="mr-2 h-4 w-4" />
                Take Photo
              </Button>
            ) : photoUrl ? (
              <div className="flex gap-3 w-full">
                <Button 
                  onClick={retakePhoto}
                  variant="outline"
                  className="flex-1 h-12 rounded-xl border-primary-200 text-primary-700 hover:bg-primary-50"
                >
                  <RefreshCw className="mr-2 h-4 w-4" />
                  Retake Photo
                </Button>
                <Button 
                  onClick={triggerFileUpload}
                  variant="outline"
                  className="flex-1 h-12 rounded-xl border-primary-200 text-primary-700 hover:bg-primary-50"
                >
                  <Image className="mr-2 h-4 w-4" />
                  Gallery
                </Button>
              </div>
            ) : (
              <div className="flex gap-3 w-full">
                <Button 
                  onClick={startCapture}
                  className="flex-[2] h-12 rounded-xl bg-primary-600 hover:bg-primary-700 shadow-md"
                >
                  <CameraIcon className="mr-2 h-4 w-4" />
                  Capture Photo
                </Button>
                <Button 
                  onClick={triggerFileUpload}
                  variant="outline"
                  className="flex-1 h-12 rounded-xl border-primary-200 text-primary-700 hover:bg-primary-50"
                >
                  <Image className="mr-2 h-4 w-4" />
                  Gallery
                </Button>
              </div>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
