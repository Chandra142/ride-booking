import './common.css';

export function LoadingSpinner({ size = 'md', text }) {
  return (
    <div className={`loading-spinner loading-spinner--${size}`}>
      <div className="spinner" />
      {text && <p className="loading-text">{text}</p>}
    </div>
  );
}

export function ErrorState({ message, onRetry }) {
  return (
    <div className="state-box state-box--error">
      <div className="state-icon">!</div>
      <p>{message || 'Something went wrong'}</p>
      {onRetry && (
        <button className="btn btn--primary" onClick={onRetry}>
          Try Again
        </button>
      )}
    </div>
  );
}

export function EmptyState({ message, action, onAction }) {
  return (
    <div className="state-box state-box--empty">
      <p>{message || 'No data found'}</p>
      {action && (
        <button className="btn btn--primary" onClick={onAction}>
          {action}
        </button>
      )}
    </div>
  );
}
