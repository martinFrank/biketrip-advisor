import { useState } from 'react';
import { AGENT_ROLES } from '../../types/pipeline';
import type { AgentStepResult } from '../../types/pipeline';

interface Props {
  step: AgentStepResult;
}

export function AgentStepCard({ step }: Props) {
  const [showInput, setShowInput] = useState(false);
  const roleInfo = AGENT_ROLES.find(r => r.role === step.role);

  const formatDuration = (ms: number) => {
    if (ms < 1000) return `${ms}ms`;
    return `${(ms / 1000).toFixed(1)}s`;
  };

  return (
    <div className="rounded-lg border border-gray-200 bg-white shadow-sm">
      <div className="flex items-center justify-between border-b border-gray-100 px-4 py-3">
        <div className="flex items-center gap-3">
          <span className="rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-700">
            ✓
          </span>
          <h3 className="font-semibold text-gray-800">{roleInfo?.label || step.role}</h3>
          <span className="text-xs text-gray-400">{roleInfo?.description}</span>
        </div>
        <div className="flex items-center gap-3">
          <span className="rounded bg-gray-100 px-2 py-0.5 text-xs font-mono text-gray-600">
            {step.modelUsed}
          </span>
          <span className="text-xs text-gray-400">{formatDuration(step.durationMs)}</span>
        </div>
      </div>

      <div className="px-4 py-3">
        <button
          onClick={() => setShowInput(!showInput)}
          className="mb-2 text-xs text-blue-500 hover:text-blue-700"
        >
          {showInput ? 'Input ausblenden' : 'Input anzeigen'}
        </button>

        {showInput && (
          <pre className="mb-3 max-h-48 overflow-auto rounded bg-gray-50 p-3 text-xs text-gray-600">
            {step.input}
          </pre>
        )}

        <div className="max-h-96 overflow-auto text-sm text-gray-700 whitespace-pre-wrap">
          {step.output}
        </div>
      </div>
    </div>
  );
}
