import { useCallback, useState } from 'react';
import { startPipelineStream } from '../api/pipelineApi';
import type { AgentStepResult, PipelineState, RouteResult } from '../types/pipeline';

const INITIAL_STATE: PipelineState = {
  status: 'idle',
  steps: [],
  route: null,
  error: null,
};

export function usePipeline() {
  const [state, setState] = useState<PipelineState>(INITIAL_STATE);

  const run = useCallback(async (userMessage: string, modelOverrides?: Record<string, string>) => {
    setState({ status: 'running', steps: [], route: null, error: null });

    try {
      await startPipelineStream(
        { userMessage, modelOverrides },
        (step: AgentStepResult) => {
          setState(prev => ({
            ...prev,
            steps: [...prev.steps, step],
          }));
        },
        (route: RouteResult) => {
          setState(prev => ({ ...prev, route }));
        },
        () => {
          setState(prev => ({ ...prev, status: 'complete' }));
        },
        (error: string) => {
          setState(prev => ({ ...prev, status: 'error', error }));
        }
      );
    } catch (e) {
      setState(prev => ({
        ...prev,
        status: 'error',
        error: e instanceof Error ? e.message : 'Unknown error',
      }));
    }
  }, []);

  const reset = useCallback(() => setState(INITIAL_STATE), []);

  return { state, run, reset };
}
