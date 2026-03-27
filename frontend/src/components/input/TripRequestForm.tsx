import { FormEvent, useState } from 'react';

interface Props {
  onSubmit: (message: string) => void;
  disabled: boolean;
}

export function TripRequestForm({ onSubmit, disabled }: Props) {
  const [message, setMessage] = useState(
    'Plane eine 5-tägige Radtour von Bad Säckingen nach Basel mit Sightseeing und Budget unter 500€'
  );

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (message.trim() && !disabled) {
      onSubmit(message.trim());
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <textarea
        value={message}
        onChange={e => setMessage(e.target.value)}
        placeholder="Beschreibe deine Radtour-Wünsche..."
        rows={4}
        disabled={disabled}
        className="w-full rounded-lg border border-gray-300 bg-white px-4 py-3 text-gray-900 placeholder-gray-400 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200 disabled:opacity-50"
      />
      <button
        type="submit"
        disabled={disabled || !message.trim()}
        className="rounded-lg bg-blue-600 px-6 py-2.5 font-medium text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-300 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {disabled ? 'Pipeline läuft...' : 'Tour planen'}
      </button>
    </form>
  );
}
