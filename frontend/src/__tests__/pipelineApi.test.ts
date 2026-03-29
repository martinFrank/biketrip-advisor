import { describe, it, expect, vi, beforeEach } from 'vitest';
import { startPipelineStream, fetchModels, fetchConfig } from '../api/pipelineApi';
import type { AgentStepResult, RouteResult } from '../types/pipeline';

function createSSEResponse(events: { event: string; data: string }[]): ReadableStream<Uint8Array> {
  const encoder = new TextEncoder();
  const text = events
    .map(e => `event:${e.event}\ndata:${e.data}\n\n`)
    .join('');

  return new ReadableStream({
    start(controller) {
      controller.enqueue(encoder.encode(text));
      controller.close();
    },
  });
}

describe('startPipelineStream', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('calls onStep for each step-complete event', async () => {
    const step: AgentStepResult = {
      role: 'CHAT',
      modelUsed: 'mistral',
      input: 'test',
      output: 'result',
      durationMs: 100,
    };

    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      body: createSSEResponse([
        { event: 'step-complete', data: JSON.stringify(step) },
        { event: 'pipeline-complete', data: 'done' },
      ]),
    } as unknown as Response);

    const onStep = vi.fn();
    const onRoute = vi.fn();
    const onComplete = vi.fn();
    const onError = vi.fn();

    await startPipelineStream({ userMessage: 'test' }, onStep, onRoute, onComplete, onError);

    expect(onStep).toHaveBeenCalledTimes(1);
    expect(onStep).toHaveBeenCalledWith(step);
    expect(onComplete).toHaveBeenCalledTimes(1);
    expect(onError).not.toHaveBeenCalled();
  });

  it('calls onRoute for route-ready event', async () => {
    const route: RouteResult = {
      waypoints: [{ name: 'A', lat: 1, lon: 2, dayNumber: 1 }],
      geojson: null,
      totalDistanceKm: 50,
    };

    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      body: createSSEResponse([
        { event: 'route-ready', data: JSON.stringify(route) },
        { event: 'pipeline-complete', data: 'done' },
      ]),
    } as unknown as Response);

    const onRoute = vi.fn();
    await startPipelineStream({ userMessage: 'test' }, vi.fn(), onRoute, vi.fn(), vi.fn());

    expect(onRoute).toHaveBeenCalledTimes(1);
    expect(onRoute).toHaveBeenCalledWith(route);
  });

  it('calls onError for error event', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      body: createSSEResponse([
        { event: 'error', data: 'Pipeline failed' },
      ]),
    } as unknown as Response);

    const onError = vi.fn();
    await startPipelineStream({ userMessage: 'test' }, vi.fn(), vi.fn(), vi.fn(), onError);

    expect(onError).toHaveBeenCalledWith('Pipeline failed');
  });

  it('calls onError when response is not ok', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: false,
      status: 500,
    } as Response);

    const onError = vi.fn();
    await startPipelineStream({ userMessage: 'test' }, vi.fn(), vi.fn(), vi.fn(), onError);

    expect(onError).toHaveBeenCalledWith('Server error: 500');
  });

  it('calls onError when response body is null', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      body: null,
    } as unknown as Response);

    const onError = vi.fn();
    await startPipelineStream({ userMessage: 'test' }, vi.fn(), vi.fn(), vi.fn(), onError);

    expect(onError).toHaveBeenCalledWith('No response body');
  });

  it('handles multiple steps in sequence', async () => {
    const steps: AgentStepResult[] = [
      { role: 'CHAT', modelUsed: 'mistral', input: 'a', output: 'b', durationMs: 10 },
      { role: 'REASONING', modelUsed: 'deepseek', input: 'b', output: 'c', durationMs: 20 },
      { role: 'PLANNING', modelUsed: 'qwen', input: 'c', output: 'd', durationMs: 30 },
      { role: 'LANGUAGE', modelUsed: 'llama', input: 'd', output: 'e', durationMs: 40 },
    ];

    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      body: createSSEResponse([
        ...steps.map(s => ({ event: 'step-complete', data: JSON.stringify(s) })),
        { event: 'pipeline-complete', data: 'done' },
      ]),
    } as unknown as Response);

    const onStep = vi.fn();
    await startPipelineStream({ userMessage: 'test' }, onStep, vi.fn(), vi.fn(), vi.fn());

    expect(onStep).toHaveBeenCalledTimes(4);
    expect(onStep.mock.calls[0][0].role).toBe('CHAT');
    expect(onStep.mock.calls[3][0].role).toBe('LANGUAGE');
  });

  it('handles chunked SSE data across multiple reads', async () => {
    const step: AgentStepResult = {
      role: 'CHAT', modelUsed: 'mistral', input: 'a', output: 'b', durationMs: 10,
    };
    const encoder = new TextEncoder();

    // Split between two complete SSE events (at the double-newline boundary)
    const chunk1 = `event:step-complete\ndata:${JSON.stringify(step)}\n\n`;
    const chunk2 = `event:pipeline-complete\ndata:done\n\n`;

    let enqueueCount = 0;
    const stream = new ReadableStream<Uint8Array>({
      pull(controller) {
        if (enqueueCount === 0) {
          controller.enqueue(encoder.encode(chunk1));
          enqueueCount++;
        } else if (enqueueCount === 1) {
          controller.enqueue(encoder.encode(chunk2));
          enqueueCount++;
        } else {
          controller.close();
        }
      },
    });

    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      body: stream,
    } as unknown as Response);

    const onStep = vi.fn();
    const onComplete = vi.fn();
    await startPipelineStream({ userMessage: 'test' }, onStep, vi.fn(), onComplete, vi.fn());

    expect(onStep).toHaveBeenCalledTimes(1);
    expect(onComplete).toHaveBeenCalledTimes(1);
  });
});

describe('fetchModels', () => {
  it('returns model names from Ollama response', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({
        models: [{ name: 'mistral' }, { name: 'llama3.1:8b' }],
      }),
    } as unknown as Response);

    const models = await fetchModels();
    expect(models).toEqual(['mistral', 'llama3.1:8b']);
  });

  it('returns empty array on error', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('Network error'));

    const models = await fetchModels();
    expect(models).toEqual([]);
  });
});

describe('fetchConfig', () => {
  it('returns config from backend', async () => {
    const config = {
      defaults: { CHAT: 'mistral', REASONING: 'deepseek-r1:8b' },
      categories: { CHAT: ['mistral', 'llama3.1:8b'], REASONING: ['deepseek-r1:8b'] },
    };
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(config),
    } as unknown as Response);

    const result = await fetchConfig();
    expect(result).toEqual(config);
  });

  it('returns empty defaults and categories on error', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('fail'));

    const result = await fetchConfig();
    expect(result).toEqual({ defaults: {}, categories: {} });
  });
});
