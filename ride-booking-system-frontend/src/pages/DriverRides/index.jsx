import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getRideById } from '../../api/rideApi';
import { LoadingSpinner, ErrorState, EmptyState } from '../../components/common/LoadingSpinner';
import { StatusBadge } from '../../components/common/Badge';

export default function DriverRidesPage() {
  const [rides, setRides] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchRides = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await getRideById('me');
      setRides(Array.isArray(res.data) ? res.data : [res.data]);
    } catch {
      setRides([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchRides(); }, []);

  if (loading) return <LoadingSpinner text="Loading rides..." />;
  if (error) return <ErrorState message={error} onRetry={fetchRides} />;
  if (rides.length === 0) return <EmptyState message="No rides found" />;

  return (
    <div>
      <div className="page-header">
        <h2>My Rides</h2>
        <p>{rides.length} ride{rides.length !== 1 ? 's' : ''}</p>
      </div>
      <div className="ride-list">
        {rides.map((ride) => (
          <Link key={ride.id} to={`/driver/rides/${ride.id}`} className="ride-card" style={{ textDecoration: 'none', color: 'inherit' }}>
            <div className="ride-card-info">
              <h4>{ride.pickupLocation} → {ride.dropLocation}</h4>
              <p>Rider #{ride.riderId} · {ride.requestedAt ? new Date(ride.requestedAt).toLocaleString() : ''}</p>
            </div>
            <div className="ride-card-meta">
              <StatusBadge status={ride.status} />
              {ride.fare && <div className="fare" style={{ marginTop: 'var(--space-xs)' }}>₹{ride.fare}</div>}
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}
