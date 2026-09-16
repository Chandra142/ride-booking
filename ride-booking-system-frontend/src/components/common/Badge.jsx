export function Badge({ children, variant = 'default' }) {
  return <span className={`badge badge--${variant}`}>{children}</span>;
}

const STATUS_VARIANTS = {
  REQUESTED: 'info',
  DRIVER_ASSIGNED: 'warning',
  ACCEPTED: 'warning',
  ONGOING: 'primary',
  COMPLETED: 'success',
  CANCELLED: 'danger',
  PENDING: 'warning',
  SUCCESS: 'success',
  FAILED: 'danger',
  REFUNDED: 'info',
  SENT: 'success',
};

export function StatusBadge({ status }) {
  const variant = STATUS_VARIANTS[status] || 'default';
  return <Badge variant={variant}>{status?.replace(/_/g, ' ')}</Badge>;
}
