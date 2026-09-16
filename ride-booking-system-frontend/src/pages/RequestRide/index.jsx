import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { requestRide } from '../../api/rideApi';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';

export default function RequestRidePage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    pickupLocation: '',
    dropLocation: '',
    pickupLatitude: '',
    pickupLongitude: '',
    dropLatitude: '',
    dropLongitude: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const payload = {
        riderId: user.userId,
        pickupLocation: form.pickupLocation,
        dropLocation: form.dropLocation,
        pickupLatitude: parseFloat(form.pickupLatitude),
        pickupLongitude: parseFloat(form.pickupLongitude),
        dropLatitude: parseFloat(form.dropLatitude),
        dropLongitude: parseFloat(form.dropLongitude),
      };
      const res = await requestRide(payload);
      navigate(`/app/rides/${res.data.id}`);
    } catch (err) {
      setError(err.response?.data?.error || err.response?.data?.message || 'Failed to request ride');
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <LoadingSpinner text="Finding you a driver..." />;

  return (
    <div style={{ maxWidth: 600, margin: '0 auto' }}>
      <div className="page-header">
        <h2>Book a Ride</h2>
        <p>Enter your pickup and drop-off details</p>
      </div>

      <div className="card">
        {error && <div className="alert alert--error">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Pickup Location</label>
            <input value={form.pickupLocation} onChange={update('pickupLocation')} required placeholder="e.g. MG Road, Bangalore" />
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-md)' }}>
            <div className="form-group">
              <label>Pickup Latitude</label>
              <input type="number" step="any" value={form.pickupLatitude} onChange={update('pickupLatitude')} required placeholder="12.9758" />
            </div>
            <div className="form-group">
              <label>Pickup Longitude</label>
              <input type="number" step="any" value={form.pickupLongitude} onChange={update('pickupLongitude')} required placeholder="77.6045" />
            </div>
          </div>

          <div className="form-group">
            <label>Drop Location</label>
            <input value={form.dropLocation} onChange={update('dropLocation')} required placeholder="e.g. Whitefield, Bangalore" />
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-md)' }}>
            <div className="form-group">
              <label>Drop Latitude</label>
              <input type="number" step="any" value={form.dropLatitude} onChange={update('dropLatitude')} required placeholder="12.9698" />
            </div>
            <div className="form-group">
              <label>Drop Longitude</label>
              <input type="number" step="any" value={form.dropLongitude} onChange={update('dropLongitude')} required placeholder="77.7500" />
            </div>
          </div>

          <button className="btn btn--primary btn--block btn--lg" type="submit">
            Request Ride
          </button>
        </form>
      </div>
    </div>
  );
}
