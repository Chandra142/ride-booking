import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../../context/AuthContext';
import { getUserById, updateUser } from '../../api/userApi';
import { getDriverById, updateDriver } from '../../api/driverApi';
import { LoadingSpinner, ErrorState } from '../../components/common/LoadingSpinner';

export default function ProfilePage() {
  const { user, isDriver } = useAuth();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState({ fullName: '', phone: '', profileImage: '' });
  const [saving, setSaving] = useState(false);
  const [saveMsg, setSaveMsg] = useState('');

  const fetchProfile = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      if (isDriver) {
        const res = await getDriverById(user.userId);
        setProfile(res.data);
        setForm({ fullName: `${res.data.firstName || ''} ${res.data.lastName || ''}`.trim(), phone: res.data.phone || '', profileImage: '' });
      } else {
        const res = await getUserById(user.userId);
        setProfile(res.data);
        setForm({ fullName: res.data.fullName || '', phone: res.data.phone || '', profileImage: res.data.profileImage || '' });
      }
    } catch (err) {
      setError('Failed to load profile');
    } finally {
      setLoading(false);
    }
  }, [isDriver, user.userId]);

  useEffect(() => { fetchProfile(); }, [fetchProfile]);

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    setSaveMsg('');
    try {
      if (isDriver) {
        await updateDriver(user.userId, { firstName: form.fullName.split(' ')[0], lastName: form.fullName.split(' ').slice(1).join(' '), phone: form.phone });
      } else {
        await updateUser(user.userId, { fullName: form.fullName, phone: form.phone, profileImage: form.profileImage });
      }
      setSaveMsg('Profile updated');
      setEditing(false);
      fetchProfile();
    } catch (err) {
      setSaveMsg('Failed to update profile');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <LoadingSpinner text="Loading profile..." />;
  if (error) return <ErrorState message={error} onRetry={fetchProfile} />;

  return (
    <div style={{ maxWidth: 600, margin: '0 auto' }}>
      <div className="page-header">
        <h2>My Profile</h2>
      </div>

      {saveMsg && <div className={`alert alert--${saveMsg.includes('Failed') ? 'error' : 'success'}`}>{saveMsg}</div>}

      <div className="card">
        {editing ? (
          <form onSubmit={handleSave}>
            <div className="form-group">
              <label>Full Name</label>
              <input value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required />
            </div>
            <div className="form-group">
              <label>Phone</label>
              <input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
            </div>
            {!isDriver && (
              <div className="form-group">
                <label>Profile Image URL</label>
                <input value={form.profileImage} onChange={(e) => setForm({ ...form, profileImage: e.target.value })} />
              </div>
            )}
            <div className="btn-group">
              <button className="btn btn--primary" type="submit" disabled={saving}>{saving ? 'Saving...' : 'Save'}</button>
              <button className="btn btn--secondary" type="button" onClick={() => setEditing(false)}>Cancel</button>
            </div>
          </form>
        ) : (
          <>
            <dl className="detail-grid">
              <dt>Email</dt>
              <dd>{profile?.email || user?.email}</dd>
              <dt>Name</dt>
              <dd>{isDriver ? `${profile?.firstName || ''} ${profile?.lastName || ''}`.trim() : profile?.fullName}</dd>
              <dt>Role</dt>
              <dd>{isDriver ? 'Driver' : 'Rider'}</dd>
              <dt>Phone</dt>
              <dd>{profile?.phone || '-'}</dd>
              {isDriver && (
                <>
                  <dt>Vehicle</dt>
                  <dd>{profile?.vehicleType} ({profile?.vehicleColor})</dd>
                  <dt>License</dt>
                  <dd>{profile?.licenseNumber}</dd>
                  <dt>Availability</dt>
                  <dd>{profile?.availabilityStatus}</dd>
                </>
              )}
            </dl>
            <button className="btn btn--primary" style={{ marginTop: 'var(--space-lg)' }} onClick={() => setEditing(true)}>
              Edit Profile
            </button>
          </>
        )}
      </div>
    </div>
  );
}
