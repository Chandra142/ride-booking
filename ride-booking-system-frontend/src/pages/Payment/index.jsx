import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../../context/AuthContext';
import { getPaymentsByUser, processPayment } from '../../api/paymentApi';
import { LoadingSpinner, ErrorState, EmptyState } from '../../components/common/LoadingSpinner';
import { StatusBadge } from '../../components/common/Badge';

export default function PaymentPage() {
  const { user } = useAuth();
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ rideId: '', amount: '', paymentMethod: 'CARD', cardNumber: '', cardHolderName: '', expiryDate: '', cvv: '' });
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');
  const [submitSuccess, setSubmitSuccess] = useState('');

  const fetchPayments = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getPaymentsByUser(user.userId);
      setPayments(res.data?.data || []);
    } catch (err) {
      setError('Failed to load payments');
    } finally {
      setLoading(false);
    }
  }, [user.userId]);

  useEffect(() => { fetchPayments(); }, [fetchPayments]);

  const handlePay = async (e) => {
    e.preventDefault();
    setSubmitError('');
    setSubmitSuccess('');
    setSubmitting(true);
    try {
      const idempotencyKey = `pay-${form.rideId}-${user.userId}-${Date.now()}`;
      const payload = {
        rideId: parseInt(form.rideId),
        userId: user.userId,
        amount: parseFloat(form.amount),
        paymentMethod: form.paymentMethod,
        idempotencyKey,
        cardDetails: {
          cardNumber: form.cardNumber,
          cardHolderName: form.cardHolderName,
          expiryDate: form.expiryDate,
          cvv: form.cvv,
        },
      };
      const res = await processPayment(payload);
      setSubmitSuccess(`Payment ${res.data?.data?.paymentStatus || 'processed'}`);
      setShowForm(false);
      fetchPayments();
    } catch (err) {
      setSubmitError(err.response?.data?.message || 'Payment failed');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <LoadingSpinner text="Loading payments..." />;
  if (error) return <ErrorState message={error} onRetry={fetchPayments} />;

  return (
    <div>
      <div className="page-header">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h2>Payments</h2>
            <p>{payments.length} payment{payments.length !== 1 ? 's' : ''}</p>
          </div>
          <button className="btn btn--primary" onClick={() => setShowForm(!showForm)}>
            {showForm ? 'Cancel' : 'New Payment'}
          </button>
        </div>
      </div>

      {showForm && (
        <div className="card" style={{ marginBottom: 'var(--space-xl)' }}>
          <h3 style={{ marginBottom: 'var(--space-md)' }}>Process Payment</h3>
          {submitError && <div className="alert alert--error">{submitError}</div>}
          {submitSuccess && <div className="alert alert--success">{submitSuccess}</div>}
          <form onSubmit={handlePay}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-md)' }}>
              <div className="form-group">
                <label>Ride ID</label>
                <input type="number" value={form.rideId} onChange={(e) => setForm({ ...form, rideId: e.target.value })} required />
              </div>
              <div className="form-group">
                <label>Amount (₹)</label>
                <input type="number" step="0.01" min="1" value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} required />
              </div>
            </div>
            <div className="form-group">
              <label>Card Number</label>
              <input value={form.cardNumber} onChange={(e) => setForm({ ...form, cardNumber: e.target.value })} required placeholder="1234567890123456" />
            </div>
            <div className="form-group">
              <label>Card Holder Name</label>
              <input value={form.cardHolderName} onChange={(e) => setForm({ ...form, cardHolderName: e.target.value })} required />
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-md)' }}>
              <div className="form-group">
                <label>Expiry Date</label>
                <input value={form.expiryDate} onChange={(e) => setForm({ ...form, expiryDate: e.target.value })} required placeholder="12/28" />
              </div>
              <div className="form-group">
                <label>CVV</label>
                <input type="password" maxLength={4} value={form.cvv} onChange={(e) => setForm({ ...form, cvv: e.target.value })} required placeholder="123" />
              </div>
            </div>
            <button className="btn btn--success btn--block btn--lg" type="submit" disabled={submitting}>
              {submitting ? 'Processing...' : 'Pay Now'}
            </button>
          </form>
        </div>
      )}

      {payments.length === 0 ? (
        <EmptyState message="No payments yet" />
      ) : (
        <div className="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Ride</th>
                <th>Amount</th>
                <th>Method</th>
                <th>Status</th>
                <th>Date</th>
              </tr>
            </thead>
            <tbody>
              {payments.map((p) => (
                <tr key={p.paymentId}>
                  <td>#{p.paymentId}</td>
                  <td>#{p.rideId}</td>
                  <td>₹{p.amount}</td>
                  <td>{p.paymentMethod}</td>
                  <td><StatusBadge status={p.paymentStatus} /></td>
                  <td>{p.paymentTime ? new Date(p.paymentTime).toLocaleDateString() : '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
