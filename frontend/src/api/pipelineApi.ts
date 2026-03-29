import type { AgentStepResult, PipelineRequest, RouteResult } from '../types/pipeline';

const API_BASE = import.meta.env.VITE_API_BASE || '/biketrip-advisor/api';

export async function startPipelineStream(
  request: PipelineRequest,
  onStep: (step: AgentStepResult) => void,
  onRoute: (route: RouteResult) => void,
  onComplete: () => void,
  onError: (error: string) => void
): Promise<void> {
  console.log('[API] Starting pipeline stream, message length:', request.userMessage.length,
    'overrides:', request.modelOverrides ?? 'none');

  const response = await fetch(`${API_BASE}/pipeline/run-streaming`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    if (response.status === 429) {
      try {
        const body = await response.json();
        console.warn('[API] Pipeline busy:', body.error);
        onError(body.error);
      } catch {
        onError('Ich bin noch am Denken... Bitte warte, bis die aktuelle Anfrage abgeschlossen ist.');
      }
    } else {
      console.error('[API] Pipeline request failed with status:', response.status);
      onError(`Server error: ${response.status}`);
    }
    return;
  }

  const reader = response.body?.getReader();
  if (!reader) {
    console.error('[API] No response body in pipeline stream');
    onError('No response body');
    return;
  }

  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });

    // SSE events are separated by double newlines
    const events = buffer.split('\n\n');
    // Last element is either empty or an incomplete event
    buffer = events.pop() || '';

    for (const eventBlock of events) {
      if (!eventBlock.trim()) continue;

      let eventName = '';
      let data = '';

      for (const line of eventBlock.split('\n')) {
        if (line.startsWith('event:')) {
          eventName = line.slice(6).trim();
        } else if (line.startsWith('data:')) {
          data = line.slice(5).trim();
        }
        // SSE comments (lines starting with ':') are silently ignored
      }

      if (!eventName || !data) continue;

      if (eventName === 'step-complete') {
        try {
          const step: AgentStepResult = JSON.parse(data);
          console.log(`[API] Step complete: ${step.role} (${step.modelUsed}) in ${step.durationMs}ms`);
          onStep(step);
        } catch (e) {
          console.error('[API] Failed to parse step event:', e);
        }
      } else if (eventName === 'route-ready') {
        try {
          const route: RouteResult = JSON.parse(data);
          console.log(`[API] Route ready: ${route.waypoints.length} waypoints, ${route.totalDistanceKm.toFixed(1)} km`);
          onRoute(route);
        } catch (e) {
          console.error('[API] Failed to parse route event:', e);
        }
      } else if (eventName === 'pipeline-complete') {
        console.log('[API] Pipeline complete');
        onComplete();
      } else if (eventName === 'error') {
        console.error('[API] Pipeline error:', data);
        onError(data);
      }
    }
  }
}

export async function fetchModels(): Promise<string[]> {
  try {
    const response = await fetch(`${API_BASE}/models`);
    if (!response.ok) {
      console.error('[API] Failed to fetch models, status:', response.status);
      return [];
    }
    const data = await response.json();
    const models = (data.models || []).map((m: { name: string }) => m.name);
    console.log(`[API] Fetched ${models.length} available models`);
    return models;
  } catch (e) {
    console.error('[API] Failed to fetch models:', e);
    return [];
  }
}

export interface AppConfig {
  defaults: Record<string, string>;
  categories: Record<string, string[]>;
}

export async function fetchConfig(): Promise<AppConfig> {
  try {
    const response = await fetch(`${API_BASE}/config`);
    if (!response.ok) {
      console.error('[API] Failed to fetch config, status:', response.status);
      return { defaults: {}, categories: {} };
    }
    const config: AppConfig = await response.json();
    console.log('[API] Fetched config: defaults:', config.defaults,
      'categories:', Object.fromEntries(Object.entries(config.categories).map(([k, v]) => [k, v.length])));
    return config;
  } catch (e) {
    console.error('[API] Failed to fetch config:', e);
    return { defaults: {}, categories: {} };
  }
}
