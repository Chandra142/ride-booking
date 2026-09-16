import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../../context/AuthContext';
import { getNotificationsByUser } from '../../api/notificationApi';
import { LoadingSpinner, ErrorState, EmptyState } from '../../components/common/LoadingSpinner';
import { StatusBadge } from '../../components/common/Badge';

export default function NotificationPage() {
  const { user } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchNotifications = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await getNotificationsByUser(user.userId);
      setNotifications(res.data?.data || []);
    } catch (err) {
      setError('Failed to load notifications');
    } finally {
      setLoading(false);
    }
  }, [user.userId]);

  useEffect(() => { fetchNotifications(); }, [fetchNotifications]);

  if (loading) return <LoadingSpinner text="Loading notifications..." />;
  if (error) return <ErrorState message={error} onRetry={fetchNotifications} />;
  if (notifications.length === 0) return <EmptyState message="No notifications" />;

  return (
    <div>
      <div className="page-header">
        <h2>Notifications</h2>
        <p>{notifications.length} notification{notifications.length !== 1 ? 's' : ''}</p>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-md)' }}>
        {notifications.map((n) => (
          <div key={n.notificationId} className="card" style={{ opacity: n.status === 'SENT' ? 1 : 0.7 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 'var(--space-md)' }}>
              <div>
                <h4 style={{ fontSize: '0.9375rem', marginBottom: 'var(--space-xs)' }}>{n.title}</h4>
                <p style={{ fontSize: '0.875rem', color: 'var(--color-text-secondary)' }}>{n.message}</p>
              </div>
              <StatusBadge status={n.status} />
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', marginTop: 'var(--space-sm)' }}>
              {n.createdAt ? new Date(n.createdAt).toLocaleString() : ''}
              {n.rideId ? ` · Ride #${n.rideId}` : ''}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
