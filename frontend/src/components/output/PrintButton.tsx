interface Props {
  disabled?: boolean;
}

export function PrintButton({ disabled }: Props) {
  return (
    <button
      onClick={() => window.print()}
      disabled={disabled}
      className="px-4 py-1.5 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg shadow-sm hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-all print-hidden"
      title="Reisebericht drucken"
    >
      Drucken
    </button>
  );
}
