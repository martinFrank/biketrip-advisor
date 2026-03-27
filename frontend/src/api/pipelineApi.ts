import type { AgentStepResult, PipelineRequest, RouteResult } from '../types/pipeline';

const API_BASE = import.meta.env.VITE_API_BASE || '/biketrip-advisor/api';

export async function startPipelineStream(
  request: PipelineRequest,
  onStep: (step: AgentStepResult) => void,
  onRoute: (route: RouteResult) => void,
  onComplete: () => void,
  onError: (error: string) => void
): Promise<void> {
  const response = await fetch(`${API_BASE}/pipeline/run-streaming`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    onError(`Server error: ${response.status}`);
    return;
  }

  const reader = response.body?.getReader();
  if (!reader) {
    onError('No response body');
    return;
  }

  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split('\n');
    buffer = lines.pop() || '';

    let eventName = '';
    for (const line of lines) {
      if (line.startsWith('event:')) {
        eventName = line.slice(6).trim();
      } else if (line.startsWith('data:')) {
        const data = line.slice(5).trim();
        if (eventName === 'step-complete') {
          try {
            const step: AgentStepResult = JSON.parse(data);
            onStep(step);
          } catch (e) {
            console.error('Failed to parse step:', e);
          }
        } else if (eventName === 'route-ready') {
          try {
            const route: RouteResult = JSON.parse(data);
            onRoute(route);
          } catch (e) {
            console.error('Failed to parse route:', e);
          }
        } else if (eventName === 'pipeline-complete') {
          onComplete();
        } else if (eventName === 'error') {
          onError(data);
        }
        eventName = '';
      }
    }
  }
}

export async function fetchModels(): Promise<string[]> {
  try {
    const response = await fetch(`${API_BASE}/models`);
    const data = await response.json();
    return (data.models || []).map((m: { name: string }) => m.name);
  } catch {
    return [];
  }
}

export async function fetchConfig(): Promise<Record<string, string>> {
  try {
    const response = await fetch(`${API_BASE}/config`);
    return await response.json();
  } catch {
    return {};
  }
}
