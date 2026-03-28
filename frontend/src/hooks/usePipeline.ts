import { useCallback, useState } from 'react';
import { startPipelineStream } from '../api/pipelineApi';
import type { AgentStepResult, PipelineState, RouteResult } from '../types/pipeline';

const STORAGE_KEY = 'biketrip-pipeline-state';

const INITIAL_STATE: PipelineState = {
  status: 'idle',
  steps: [],
  route: null,
  error: null,
};

function loadState(): PipelineState {
  try {
    const saved = sessionStorage.getItem(STORAGE_KEY);
    if (!saved) return INITIAL_STATE;
    const parsed: PipelineState = JSON.parse(saved);
    // Don't restore a "running" state — the stream is gone after refresh
    if (parsed.status === 'running') {
      return { ...parsed, status: 'error', error: 'Pipeline wurde durch Seitenaktualisierung unterbrochen.' };
    }
    return parsed;
  } catch {
    return INITIAL_STATE;
  }
}

function saveState(state: PipelineState) {
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  } catch {
    // sessionStorage full or unavailable — ignore
  }
}

export function usePipeline() {
  const [state, setState] = useState<PipelineState>(loadState);

  const updateState = (updater: PipelineState | ((prev: PipelineState) => PipelineState)) => {
    setState(prev => {
      const next = typeof updater === 'function' ? updater(prev) : updater;
      saveState(next);
      return next;
    });
  };

  const run = useCallback(async (userMessage: string, modelOverrides?: Record<string, string>) => {
    console.log('[Pipeline] Starting pipeline run');
    updateState({ status: 'running', steps: [], route: null, error: null });

    try {
      await startPipelineStream(
        { userMessage, modelOverrides },
        (step: AgentStepResult) => {
          console.log(`[Pipeline] Step received: ${step.role}`);
          updateState(prev => ({
            ...prev,
            steps: [...prev.steps, step],
          }));
        },
        (route: RouteResult) => {
          console.log('[Pipeline] Route received');
          updateState(prev => ({ ...prev, route }));
        },
        () => {
          console.log('[Pipeline] Pipeline complete');
          updateState(prev => ({ ...prev, status: 'complete' }));
        },
        (error: string) => {
          console.error('[Pipeline] Pipeline error:', error);
          updateState(prev => ({ ...prev, status: 'error', error }));
        }
      );
    } catch (e) {
      const message = e instanceof Error ? e.message : 'Unknown error';
      console.error('[Pipeline] Unexpected error:', message);
      updateState(prev => ({
        ...prev,
        status: 'error',
        error: message,
      }));
    }
  }, []);

  const reset = useCallback(() => {
    updateState(INITIAL_STATE);
  }, []);

  return { state, run, reset };
}
