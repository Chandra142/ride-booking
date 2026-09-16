import { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { getRideById, acceptRide, startRide, completeRide } from '../../api/rideApi';
import { LoadingSpinner, ErrorState } from '../../components/common/LoadingSpinner';
import { StatusBadge } from '../../components/common/Badge';

export default function DriverRideDetailPage() {
  const { id } = useParams();
  const [ride, setRide] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionLoading, setActionLoading] = useState(false);
  const [actionError, setActionError] = useState('');

  const fetchRide = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await getRideById(id);
      setRide(res.data);
    } catch {
      setError('Failed to load ride details');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { fetchRide(); }, [fetchRide]);

  const handleAction = async (actionFn) => {
    setActionLoading(true);
    setActionError('');
    try {
      const res = await actionFn(id);
      setRide(res.data);
    } catch (err) {
      setActionError(err.response?.data?.error || 'Action failed');
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) return <LoadingSpinner text="Loading ride..." />;
  if (error) return <ErrorState message={error} onRetry={fetchRide} />;
  if (!ride) return <ErrorState message="Ride not found" />;

  const actions = {
    DRIVER_ASSIGNED: { label: 'Accept Ride', fn: acceptRide },
    ACCEPTED: { label: 'Start Ride', fn: startRide },
    ONGOING: { label: 'Complete Ride', fn: completeRide },
  };

  const currentAction = actions[ride.status];

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
          <dt>Fare</dt>
          <dd>{ride.fare ? `₹${ride.fare}` : 'Pending'}</dd>
          <dt>Requested</dt>
          <dd>{ride.requestedAt ? new Date(ride.requestedAt).toLocaleString() : '-'}</dd>
          <dt>Completed</dt>
          <dd>{ride.completedAt ? new Date(ride.completedAt).toLocaleString() : '-'}</dd>
        </dl>
      </div>

      {actionError && <div className="alert alert--error">{actionError}</div>}

      {currentAction && (
        <button
          className="btn btn--primary btn--block btn--lg"
          onClick={() => handleAction(currentAction.fn)}
          disabled={actionLoading}
        >
          {actionLoading ? 'Processing...' : currentAction.label}
        </button>
      )}
    </div>
  );
}
