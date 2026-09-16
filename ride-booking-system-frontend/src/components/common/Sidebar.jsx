import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export default function Sidebar() {
  const { user, isDriver, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const riderLinks = [
    { to: '/app/dashboard', label: 'Dashboard' },
    { to: '/app/request-ride', label: 'Book a Ride' },
    { to: '/app/rides', label: 'My Rides' },
    { to: '/app/payments', label: 'Payments' },
    { to: '/app/notifications', label: 'Notifications' },
    { to: '/app/profile', label: 'Profile' },
  ];

  const driverLinks = [
    { to: '/driver/dashboard', label: 'Dashboard' },
    { to: '/driver/rides', label: 'My Rides' },
    { to: '/driver/notifications', label: 'Notifications' },
    { to: '/driver/profile', label: 'Profile' },
  ];

  const links = isDriver ? driverLinks : riderLinks;

  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <h1>RideBooking</h1>
      </div>
      <nav className="sidebar-nav">
        {links.map((link) => (
          <NavLink
            key={link.to}
            to={link.to}
            className={({ isActive }) => isActive ? 'active' : ''}
          >
            {link.label}
          </NavLink>
        ))}
      </nav>
      <div className="sidebar-footer">
        <div style={{ fontSize: '0.8125rem', color: 'var(--color-text-secondary)', marginBottom: 'var(--space-sm)' }}>
          {user?.email}
          <br />
          <span style={{ fontWeight: 600, color: 'var(--color-primary)' }}>
            {isDriver ? 'Driver' : 'Rider'}
          </span>
        </div>
        <button className="btn btn--secondary btn--sm btn--block" onClick={handleLogout}>
          Sign Out
        </button>
      </div>
    </aside>
  );
}
