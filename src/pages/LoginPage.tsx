import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { LogIn, UserPlus, Key, Mail, Lock, User } from 'lucide-react';
import { toast } from 'sonner';

interface LoginPageProps {
  onLogin: (email: string, pass: string) => Promise<void>;
  onRegister: (email: string, pass: string, name: string) => Promise<void>;
  onResetPassword: (email: string) => Promise<void>;
}

export default function LoginPage({ onLogin, onRegister, onResetPassword }: LoginPageProps) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [showReset, setShowReset] = useState(false);
  const [isRegistering, setIsRegistering] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) return toast.error("Please fill in all fields");
    if (isRegistering && !name) return toast.error("Please enter your full name");

    setIsLoading(true);
    try {
      if (isRegistering) {
        await onRegister(email, password, name);
      } else {
        await onLogin(email, password);
      }
    } catch (error: any) {
      toast.error(isRegistering ? "Registration failed" : "Login failed", { description: error.message });
    } finally {
      setIsLoading(false);
    }
  };

  const handleReset = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email) return toast.error("Please enter your email");
    setIsLoading(true);
    try {
      await onResetPassword(email);
      toast.success("Reset email sent", { description: "Check your inbox for instructions" });
      setShowReset(false);
    } catch (error: any) {
      toast.error("Reset failed", { description: error.message });
    } finally {
      setIsLoading(false);
    }
  };

  if (showReset) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <Card className="w-full max-w-md border-none shadow-2xl rounded-3xl overflow-hidden bg-white">
          <CardHeader className="bg-primary-600 text-white p-8">
            <CardTitle className="text-2xl font-black flex items-center gap-2">
              <Key className="w-6 h-6" /> Reset Password
            </CardTitle>
            <CardDescription className="text-primary-100 mt-2">
              Enter your email to receive a password reset link
            </CardDescription>
          </CardHeader>
          <CardContent className="p-8">
            <form onSubmit={handleReset} className="space-y-6">
              <div className="space-y-2">
                <Label htmlFor="reset-email" className="text-xs font-bold uppercase tracking-wider text-gray-500">Email Address</Label>
                <div className="relative">
                  <Mail className="absolute left-3 top-3 w-5 h-5 text-gray-400" />
                  <Input 
                    id="reset-email" 
                    type="email" 
                    placeholder="name@company.com" 
                    className="pl-10 h-12 rounded-xl border-gray-200 focus:ring-primary-500 text-gray-900 bg-white"
                    value={email ?? ''}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                  />
                </div>
              </div>
              <Button type="submit" className="w-full h-12 rounded-xl bg-primary-600 hover:bg-primary-700 shadow-lg font-bold" disabled={isLoading}>
                {isLoading ? "Sending..." : "Send Reset Link"}
              </Button>
              <Button variant="ghost" className="w-full h-12 rounded-xl text-gray-500 font-medium" onClick={() => setShowReset(false)}>
                Back to Login
              </Button>
            </form>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 flex justify-center p-4 overflow-y-auto py-8">
      <Card className="w-full max-w-md border-none shadow-2xl rounded-3xl overflow-hidden bg-white h-fit">
        <div className="bg-primary-600 p-8 flex flex-col items-center text-white relative">
          <div className="w-16 h-16 bg-white/20 backdrop-blur-md rounded-2xl flex items-center justify-center mb-3 shadow-xl overflow-hidden">
            <img src="/logo.png" alt="Logo" className="w-12 h-12 object-cover" referrerPolicy="no-referrer" />
          </div>
          <h1 className="text-2xl font-black tracking-tight">PalmHarvest Pro</h1>
          <p className="text-primary-100 text-xs mt-1 font-medium uppercase tracking-widest">Harvester Management</p>
        </div>

        <div className="p-8">
          <div className="mb-8 text-center">
            <h2 className="text-xl font-bold text-gray-900">{isRegistering ? "Create Account" : "Welcome Back"}</h2>
            <p className="text-gray-500 text-sm mt-1">
              {isRegistering ? "Join our harvesting network today" : "Sign in to manage your harvests"}
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            {isRegistering && (
              <div className="space-y-2">
                <Label htmlFor="reg-name" className="text-xs font-bold uppercase tracking-wider text-gray-500">Full Name</Label>
                <div className="relative">
                  <User className="absolute left-3 top-3 w-5 h-5 text-gray-400" />
                  <Input 
                    id="reg-name" 
                    placeholder="John Doe" 
                    className="pl-10 h-12 rounded-xl border-gray-200 focus:ring-primary-500 text-gray-900 bg-white"
                    value={name ?? ''}
                    onChange={(e) => setName(e.target.value)}
                    required={isRegistering}
                  />
                </div>
              </div>
            )}
            
            <div className="space-y-2">
              <Label htmlFor="email" className="text-xs font-bold uppercase tracking-wider text-gray-500">Email Address</Label>
              <div className="relative">
                <Mail className="absolute left-3 top-3 w-5 h-5 text-gray-400" />
                <Input 
                  id="email" 
                  type="email" 
                  placeholder="name@company.com" 
                  className="pl-10 h-12 rounded-xl border-gray-200 focus:ring-primary-500 text-gray-900 bg-white"
                  value={email ?? ''}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Label htmlFor="password" className="text-xs font-bold uppercase tracking-wider text-gray-500">Password</Label>
                {!isRegistering && (
                  <button type="button" onClick={() => setShowReset(true)} className="text-xs font-bold text-primary-600 hover:underline">Forgot?</button>
                )}
              </div>
              <div className="relative">
                <Lock className="absolute left-3 top-3 w-5 h-5 text-gray-400" />
                <Input 
                  id="password" 
                  type="password" 
                  placeholder="••••••••" 
                  className="pl-10 h-12 rounded-xl border-gray-200 focus:ring-primary-500 text-gray-900 bg-white"
                  value={password ?? ''}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              </div>
            </div>

            <Button type="submit" className="w-full h-12 rounded-xl bg-primary-600 hover:bg-primary-700 shadow-lg font-bold text-sm uppercase tracking-widest" disabled={isLoading}>
              {isLoading ? (isRegistering ? "Creating..." : "Signing in...") : (isRegistering ? "Create Account" : "Sign In")}
            </Button>

            <div className="pt-4 text-center">
              <button 
                type="button" 
                onClick={() => setIsRegistering(!isRegistering)}
                className="text-sm font-medium text-gray-500 hover:text-primary-600 transition-colors"
              >
                {isRegistering ? "Already have an account? Sign In" : "Don't have an account? Register"}
              </button>
            </div>
          </form>
        </div>
      </Card>
    </div>
  );
}
