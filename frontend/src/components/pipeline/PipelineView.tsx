import type { PipelineState } from '../../types/pipeline';
import { AgentStepCard } from './AgentStepCard';
import { StepProgress } from './StepProgress';

interface Props {
  state: PipelineState;
}

export function PipelineView({ state }: Props) {
  if (state.status === 'idle') return null;

  return (
    <div className="space-y-4">
      <StepProgress currentStep={state.steps.length} status={state.status} />

      {state.status === 'running' && state.steps.length < 4 && (
        <div className="flex items-center justify-center gap-2 py-4 text-sm text-blue-600">
          <div className="h-4 w-4 animate-spin rounded-full border-2 border-blue-600 border-t-transparent" />
          Agent {state.steps.length + 1} von 4 arbeitet...
        </div>
      )}

      {state.error && (
        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          Fehler: {state.error}
        </div>
      )}

      <div className="space-y-3">
        {state.steps.map((step, i) => (
          <AgentStepCard key={i} step={step} />
        ))}
      </div>
    </div>
  );
}
