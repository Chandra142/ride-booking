import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute, PublicRoute } from './components/common/ProtectedRoute';
import Sidebar from './components/common/Sidebar';
import LoginPage from './pages/Login';
import RegisterPage from './pages/Register';
import RiderDashboard from './pages/RiderDashboard';
import RequestRidePage from './pages/RequestRide';
import RideHistoryPage from './pages/RideHistory';
import RideDetailPage from './pages/RideDetail';
import PaymentPage from './pages/Payment';
import NotificationPage from './pages/Notifications';
import ProfilePage from './pages/Profile';
import DriverDashboardPage from './pages/DriverDashboard';
import DriverRidesPage from './pages/DriverRides';
import DriverRideDetailPage from './pages/DriverRideDetail';
import './App.css';

function RiderLayout({ children }) {
  return (
    <ProtectedRoute>
      <Sidebar />
      <main className="main-content">{children}</main>
    </ProtectedRoute>
  );
}

function DriverLayout({ children }) {
  return (
    <ProtectedRoute>
      <Sidebar />
      <main className="main-content">{children}</main>
    </ProtectedRoute>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Navigate to="/login" replace />} />
          <Route path="/login" element={<PublicRoute><LoginPage /></PublicRoute>} />
          <Route path="/register" element={<PublicRoute><RegisterPage /></PublicRoute>} />

          <Route path="/app" element={<RiderLayout><RiderDashboard /></RiderLayout>} />
          <Route path="/app/dashboard" element={<RiderLayout><RiderDashboard /></RiderLayout>} />
          <Route path="/app/request-ride" element={<RiderLayout><RequestRidePage /></RiderLayout>} />
          <Route path="/app/rides" element={<RiderLayout><RideHistoryPage /></RiderLayout>} />
          <Route path="/app/rides/:id" element={<RiderLayout><RideDetailPage /></RiderLayout>} />
          <Route path="/app/payments" element={<RiderLayout><PaymentPage /></RiderLayout>} />
          <Route path="/app/notifications" element={<RiderLayout><NotificationPage /></RiderLayout>} />
          <Route path="/app/profile" element={<RiderLayout><ProfilePage /></RiderLayout>} />

          <Route path="/driver" element={<DriverLayout><DriverDashboardPage /></DriverLayout>} />
          <Route path="/driver/dashboard" element={<DriverLayout><DriverDashboardPage /></DriverLayout>} />
          <Route path="/driver/rides" element={<DriverLayout><DriverRidesPage /></DriverLayout>} />
          <Route path="/driver/rides/:id" element={<DriverLayout><DriverRideDetailPage /></DriverLayout>} />
          <Route path="/driver/notifications" element={<DriverLayout><NotificationPage /></DriverLayout>} />
          <Route path="/driver/profile" element={<DriverLayout><ProfilePage /></DriverLayout>} />

          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
