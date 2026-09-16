import { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { getRideById, cancelRide } from '../../api/rideApi';
import { LoadingSpinner, ErrorState } from '../../components/common/LoadingSpinner';
import { StatusBadge } from '../../components/common/Badge';

export default function RideDetailPage() {
  const { id } = useParams();
  const [ride, setRide] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  const fetchRide = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await getRideById(id);
      setRide(res.data);
    } catch (err) {
      setError('Failed to load ride details');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { fetchRide(); }, [fetchRide]);

  const handleCancel = async () => {
    if (!window.confirm('Are you sure you want to cancel this ride?')) return;
    setActionLoading(true);
    try {
      const res = await cancelRide(id);
      setRide(res.data);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to cancel ride');
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) return <LoadingSpinner text="Loading ride details..." />;
  if (error) return <ErrorState message={error} onRetry={fetchRide} />;
  if (!ride) return <ErrorState message="Ride not found" />;

  const canCancel = ['REQUESTED', 'DRIVER_ASSIGNED', 'ACCEPTED'].includes(ride.status);

  return (
    <div style={{ maxWidth: 600, margin: '0 auto' }}>
      <div className="page-header">
        <h2>Ride #{ride.id}</h2>
        <StatusBadge status={ride.status} />
      </div>

      <div className="card" style={{ marginBottom: 'var(--space-lg)' }}>
        <div className="location-group">
          <div>
            <div className="location-dot location-dot--pickup" />
            <div className="location-line" />
            <div className="location-dot location-dot--drop" />
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ marginBottom: 'var(--space-sm)' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)' }}>Pickup</div>
              <strong>{ride.pickupLocation}</strong>
            </div>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)' }}>Drop</div>
              <strong>{ride.dropLocation}</strong>
            </div>
          </div>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 'var(--space-lg)' }}>
        <dl className="detail-grid">
          <dt>Status</dt>
          <dd><StatusBadge status={ride.status} /></dd>
          <dt>Rider ID</dt>
          <dd>{ride.riderId}</dd>
          <dt>Driver ID</dt>
          <dd>{ride.driverId || 'Not assigned'}</dd>
          <dt>Fare</dt>
          <dd>{ride.fare ? `₹${ride.fare}` : 'Pending'}</dd>
          <dt>Requested At</dt>
          <dd>{ride.requestedAt ? new Date(ride.requestedAt).toLocaleString() : '-'}</dd>
          <dt>Completed At</dt>
          <dd>{ride.completedAt ? new Date(ride.completedAt).toLocaleString() : '-'}</dd>
        </dl>
      </div>

      {canCancel && (
        <button className="btn btn--danger btn--block btn--lg" onClick={handleCancel} disabled={actionLoading}>
          {actionLoading ? 'Cancelling...' : 'Cancel Ride'}
        </button>
      )}
    </div>
  );
}
