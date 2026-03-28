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
    console.log('[Pipeline] Starting pipeline run');
    setState({ status: 'running', steps: [], route: null, error: null });

    try {
      await startPipelineStream(
        { userMessage, modelOverrides },
        (step: AgentStepResult) => {
          console.log(`[Pipeline] Step received: ${step.role}`);
          setState(prev => ({
            ...prev,
            steps: [...prev.steps, step],
          }));
        },
        (route: RouteResult) => {
          console.log('[Pipeline] Route received');
          setState(prev => ({ ...prev, route }));
        },
        () => {
          console.log('[Pipeline] Pipeline complete');
          setState(prev => ({ ...prev, status: 'complete' }));
        },
        (error: string) => {
          console.error('[Pipeline] Pipeline error:', error);
          setState(prev => ({ ...prev, status: 'error', error }));
        }
      );
    } catch (e) {
      const message = e instanceof Error ? e.message : 'Unknown error';
      console.error('[Pipeline] Unexpected error:', message);
      setState(prev => ({
        ...prev,
        status: 'error',
        error: message,
      }));
    }
  }, []);

  const reset = useCallback(() => setState(INITIAL_STATE), []);

  return { state, run, reset };
}
