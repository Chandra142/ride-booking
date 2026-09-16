import { Navigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export function ProtectedRoute({ children, requiredRole }) {
  const { isAuthenticated, user } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (requiredRole && user?.role !== requiredRole) {
    return <Navigate to={user?.role === 'ROLE_DRIVER' ? '/driver/dashboard' : '/app/dashboard'} replace />;
  }

  return <div className="app-layout">{children}</div>;
}

export function PublicRoute({ children }) {
  const { isAuthenticated, user } = useAuth();

  if (isAuthenticated) {
    return <Navigate to={user?.role === 'ROLE_DRIVER' ? '/driver/dashboard' : '/app/dashboard'} replace />;
  }

  return children;
}
