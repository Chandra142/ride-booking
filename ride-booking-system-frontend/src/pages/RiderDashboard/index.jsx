import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { getRidesByRider } from '../../api/rideApi';
import { getNotificationsByUser } from '../../api/notificationApi';
import { LoadingSpinner, ErrorState } from '../../components/common/LoadingSpinner';
import { StatusBadge } from '../../components/common/Badge';

export default function RiderDashboard() {
  const { user } = useAuth();
  const [rides, setRides] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [ridesRes, notifRes] = await Promise.all([
        getRidesByRider(user.userId),
        getNotificationsByUser(user.userId),
      ]);
      setRides(ridesRes.data || []);
      setNotifications(notifRes.data?.data || []);
    } catch (err) {
      setError('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  }, [user.userId]);

  useEffect(() => { fetchData(); }, [fetchData]);

  if (loading) return <LoadingSpinner text="Loading dashboard..." />;
  if (error) return <ErrorState message={error} onRetry={fetchData} />;

  const activeRide = rides.find((r) => !['COMPLETED', 'CANCELLED'].includes(r.status));
  const recentRides = rides.slice(0, 5);

  return (
    <div>
      <div className="page-header">
        <h2>Welcome back, {user?.email}</h2>
        <p>Here&apos;s your ride overview</p>
      </div>

      <div className="grid-2" style={{ marginBottom: 'var(--space-xl)' }}>
        <div className="stat-card">
          <div className="stat-label">Total Rides</div>
          <div className="stat-value">{rides.length}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Notifications</div>
          <div className="stat-value">{notifications.length}</div>
        </div>
      </div>

      {activeRide && (
        <div className="card" style={{ marginBottom: 'var(--space-xl)' }}>
          <div className="card-header">
            <h3>Active Ride</h3>
            <StatusBadge status={activeRide.status} />
          </div>
          <div className="location-group">
            <div>
              <div className="location-dot location-dot--pickup" />
              <div className="location-line" />
              <div className="location-dot location-dot--drop" />
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ marginBottom: 'var(--space-sm)' }}>
                <strong>{activeRide.pickupLocation}</strong>
              </div>
              <div>
                <strong>{activeRide.dropLocation}</strong>
              </div>
            </div>
          </div>
          {activeRide.fare && <p style={{ fontWeight: 700 }}>Fare: ₹{activeRide.fare}</p>}
          <Link to={`/app/rides/${activeRide.id}`} className="btn btn--primary btn--sm" style={{ marginTop: 'var(--space-sm)' }}>
            View Details
          </Link>
        </div>
      )}

      <div className="card">
        <div className="card-header">
          <h3>Recent Rides</h3>
          <Link to="/app/rides" className="btn btn--secondary btn--sm">View All</Link>
        </div>
        {recentRides.length === 0 ? (
          <p style={{ color: 'var(--color-text-secondary)' }}>No rides yet. Book your first ride!</p>
        ) : (
          <div className="ride-list">
            {recentRides.map((ride) => (
              <Link key={ride.id} to={`/app/rides/${ride.id}`} className="ride-card" style={{ textDecoration: 'none', color: 'inherit' }}>
                <div className="ride-card-info">
                  <h4>{ride.pickupLocation} → {ride.dropLocation}</h4>
                  <p>{ride.requestedAt ? new Date(ride.requestedAt).toLocaleDateString() : ''}</p>
                </div>
                <div className="ride-card-meta">
                  <StatusBadge status={ride.status} />
                  {ride.fare && <div className="fare" style={{ marginTop: 'var(--space-xs)' }}>₹{ride.fare}</div>}
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>

      <div style={{ marginTop: 'var(--space-xl)', textAlign: 'center' }}>
        <Link to="/app/request-ride" className="btn btn--primary btn--lg">
          Book a Ride
        </Link>
      </div>
    </div>
  );
}
