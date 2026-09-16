import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { getDriverById, updateAvailability, updateDriverLocation } from '../../api/driverApi';
import { LoadingSpinner, ErrorState } from '../../components/common/LoadingSpinner';
import { StatusBadge } from '../../components/common/Badge';

export default function DriverDashboardPage() {
  const { user } = useAuth();
  const [driver, setDriver] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [locationEnabled, setLocationEnabled] = useState(false);
  const [locationStatus, setLocationStatus] = useState('');
  const [lastLocationUpdate, setLastLocationUpdate] = useState(null);

  const fetchDriver = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await getDriverById(user.userId);
      setDriver(res.data);
    } catch (err) {
      setError('Failed to load driver profile');
    } finally {
      setLoading(false);
    }
  }, [user.userId]);

  useEffect(() => { fetchDriver(); }, [fetchDriver]);

  const handleAvailabilityToggle = async () => {
    if (!driver) return;
    const newStatus = driver.availabilityStatus === 'ONLINE' ? 'OFFLINE' : 'ONLINE';
    try {
      const res = await updateAvailability(driver.id, newStatus);
      setDriver(res.data);
    } catch (err) {
      setError('Failed to update availability');
    }
  };

  const enableLocation = () => {
    if (!navigator.geolocation) {
      setLocationStatus('Geolocation is not supported by your browser');
      return;
    }
    setLocationEnabled(true);
    setLocationStatus('Requesting location permission...');

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        const { latitude, longitude } = position.coords;
        setLocationStatus(`Location: ${latitude.toFixed(4)}, ${longitude.toFixed(4)}`);
        setLastLocationUpdate(new Date());
        try {
          await updateDriverLocation(driver.id, { latitude, longitude });
        } catch (err) {
          setLocationStatus('Failed to send location to server');
        }
      },
      (err) => {
        setLocationEnabled(false);
        if (err.code === 1) {
          setLocationStatus('Location permission denied. Please enable location access in your browser settings.');
        } else {
          setLocationStatus('Unable to get location');
        }
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  };

  useEffect(() => {
    if (!locationEnabled || !driver) return;
    const interval = setInterval(async () => {
      navigator.geolocation.getCurrentPosition(
        async (position) => {
          const { latitude, longitude } = position.coords;
          setLocationStatus(`Location: ${latitude.toFixed(4)}, ${longitude.toFixed(4)}`);
          setLastLocationUpdate(new Date());
          try {
            await updateDriverLocation(driver.id, { latitude, longitude });
          } catch (_err) {
            // Location update failed silently
          }
        },
        () => {},
        { enableHighAccuracy: true, timeout: 10000 }
      );
    }, 30000);
    return () => clearInterval(interval);
  }, [locationEnabled, driver]);

  if (loading) return <LoadingSpinner text="Loading dashboard..." />;
  if (error) return <ErrorState message={error} onRetry={fetchDriver} />;
  if (!driver) return <ErrorState message="Driver profile not found" />;

  const isOnline = driver.availabilityStatus === 'ONLINE';
  const isBusy = driver.availabilityStatus === 'BUSY';

  return (
    <div>
      <div className="page-header">
        <h2>Driver Dashboard</h2>
        <p>{driver.firstName} {driver.lastName}</p>
      </div>

      <div className="grid-2" style={{ marginBottom: 'var(--space-xl)' }}>
        <div className="stat-card">
          <div className="stat-label">Availability</div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-sm)', marginTop: 'var(--space-sm)' }}>
            <StatusBadge status={driver.availabilityStatus} />
            {!isBusy && (
              <button className={`btn btn--sm ${isOnline ? 'btn--danger' : 'btn--success'}`} onClick={handleAvailabilityToggle}>
                {isOnline ? 'Go Offline' : 'Go Online'}
              </button>
            )}
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Vehicle</div>
          <div className="stat-value" style={{ fontSize: '1.125rem' }}>
            {driver.vehicleType} · {driver.vehicleColor}
          </div>
          <div style={{ fontSize: '0.8125rem', color: 'var(--color-text-secondary)' }}>{driver.vehicleNumber}</div>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 'var(--space-xl)' }}>
        <div className="card-header">
          <h3>Location</h3>
          {!locationEnabled ? (
            <button className="btn btn--primary btn--sm" onClick={enableLocation}>
              Enable Location
            </button>
          ) : (
            <button className="btn btn--secondary btn--sm" disabled>
              Active
            </button>
          )}
        </div>
        <p style={{ fontSize: '0.875rem', color: 'var(--color-text-secondary)' }}>
          {locationStatus || 'Location tracking is disabled'}
        </p>
        {lastLocationUpdate && (
          <p style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', marginTop: 'var(--space-xs)' }}>
            Last updated: {lastLocationUpdate.toLocaleTimeString()}
          </p>
        )}
      </div>

      <div className="card">
        <div className="card-header">
          <h3>Quick Actions</h3>
        </div>
        <div className="btn-group">
          <Link to="/driver/rides" className="btn btn--primary">View My Rides</Link>
          <Link to="/driver/profile" className="btn btn--secondary">Edit Profile</Link>
        </div>
      </div>
    </div>
  );
}
