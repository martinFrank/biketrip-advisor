import { AGENT_ROLES } from '../../types/pipeline';
import type { PipelineStatus } from '../../types/pipeline';

interface Props {
  currentStep: number;
  status: PipelineStatus;
}

export function StepProgress({ currentStep, status }: Props) {
  return (
    <div className="flex items-center justify-between">
      {AGENT_ROLES.map(({ role, label }, index) => {
        let stepStatus: 'pending' | 'running' | 'complete' | 'error';
        if (status === 'error' && index === currentStep) {
          stepStatus = 'error';
        } else if (index < currentStep) {
          stepStatus = 'complete';
        } else if (index === currentStep && status === 'running') {
          stepStatus = 'running';
        } else {
          stepStatus = 'pending';
        }

        return (
          <div key={role} className="flex flex-1 items-center">
            <div className="flex flex-col items-center">
              <div
                className={`flex h-10 w-10 items-center justify-center rounded-full border-2 text-sm font-bold transition-all ${
                  stepStatus === 'complete'
                    ? 'border-green-500 bg-green-500 text-white'
                    : stepStatus === 'running'
                    ? 'border-blue-500 bg-blue-50 text-blue-600 animate-pulse'
                    : stepStatus === 'error'
                    ? 'border-red-500 bg-red-50 text-red-600'
                    : 'border-gray-300 bg-white text-gray-400'
                }`}
              >
                {stepStatus === 'complete' ? '✓' : index + 1}
              </div>
              <span
                className={`mt-1 text-xs font-medium ${
                  stepStatus === 'complete'
                    ? 'text-green-600'
                    : stepStatus === 'running'
                    ? 'text-blue-600'
                    : stepStatus === 'error'
                    ? 'text-red-600'
                    : 'text-gray-400'
                }`}
              >
                {label}
              </span>
            </div>
            {index < AGENT_ROLES.length - 1 && (
              <div
                className={`mx-2 h-0.5 flex-1 transition-all ${
                  index < currentStep ? 'bg-green-500' : 'bg-gray-200'
                }`}
              />
            )}
          </div>
        );
      })}
    </div>
  );
}
