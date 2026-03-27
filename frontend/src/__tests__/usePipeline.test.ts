import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { usePipeline } from '../hooks/usePipeline';
import * as api from '../api/pipelineApi';
import type { AgentStepResult, RouteResult } from '../types/pipeline';

vi.mock('../api/pipelineApi');

describe('usePipeline', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('starts with idle state', () => {
    const { result } = renderHook(() => usePipeline());

    expect(result.current.state.status).toBe('idle');
    expect(result.current.state.steps).toEqual([]);
    expect(result.current.state.route).toBeNull();
    expect(result.current.state.error).toBeNull();
  });

  it('transitions to running when run is called', async () => {
    vi.mocked(api.startPipelineStream).mockImplementation(async () => {});

    const { result } = renderHook(() => usePipeline());

    await act(async () => {
      await result.current.run('test message');
    });

    expect(result.current.state.status).not.toBe('idle');
  });

  it('accumulates steps from onStep callback', async () => {
    const step: AgentStepResult = {
      role: 'CHAT', modelUsed: 'mistral', input: 'x', output: 'y', durationMs: 50,
    };

    vi.mocked(api.startPipelineStream).mockImplementation(
      async (_req, onStep, _onRoute, onComplete) => {
        onStep(step);
        onComplete();
      }
    );

    const { result } = renderHook(() => usePipeline());

    await act(async () => {
      await result.current.run('test');
    });

    expect(result.current.state.steps).toHaveLength(1);
    expect(result.current.state.steps[0].role).toBe('CHAT');
    expect(result.current.state.status).toBe('complete');
  });

  it('sets route from onRoute callback', async () => {
    const route: RouteResult = {
      waypoints: [{ name: 'A', lat: 1, lon: 2, dayNumber: 1 }],
      geojson: null,
      totalDistanceKm: 75,
    };

    vi.mocked(api.startPipelineStream).mockImplementation(
      async (_req, _onStep, onRoute, onComplete) => {
        onRoute(route);
        onComplete();
      }
    );

    const { result } = renderHook(() => usePipeline());

    await act(async () => {
      await result.current.run('test');
    });

    expect(result.current.state.route).toEqual(route);
  });

  it('sets error state from onError callback', async () => {
    vi.mocked(api.startPipelineStream).mockImplementation(
      async (_req, _onStep, _onRoute, _onComplete, onError) => {
        onError('Pipeline kaputt');
      }
    );

    const { result } = renderHook(() => usePipeline());

    await act(async () => {
      await result.current.run('test');
    });

    expect(result.current.state.status).toBe('error');
    expect(result.current.state.error).toBe('Pipeline kaputt');
  });

  it('sets error state when startPipelineStream throws', async () => {
    vi.mocked(api.startPipelineStream).mockRejectedValue(new Error('Network error'));

    const { result } = renderHook(() => usePipeline());

    await act(async () => {
      await result.current.run('test');
    });

    expect(result.current.state.status).toBe('error');
    expect(result.current.state.error).toBe('Network error');
  });

  it('resets state to initial', async () => {
    vi.mocked(api.startPipelineStream).mockImplementation(
      async (_req, onStep, _onRoute, onComplete) => {
        onStep({ role: 'CHAT', modelUsed: 'm', input: 'i', output: 'o', durationMs: 1 });
        onComplete();
      }
    );

    const { result } = renderHook(() => usePipeline());

    await act(async () => {
      await result.current.run('test');
    });

    expect(result.current.state.status).toBe('complete');

    act(() => {
      result.current.reset();
    });

    expect(result.current.state.status).toBe('idle');
    expect(result.current.state.steps).toEqual([]);
  });

  it('passes model overrides to API', async () => {
    vi.mocked(api.startPipelineStream).mockImplementation(async () => {});

    const { result } = renderHook(() => usePipeline());
    const overrides = { CHAT: 'llama3.1:8b' };

    await act(async () => {
      await result.current.run('test', overrides);
    });

    expect(api.startPipelineStream).toHaveBeenCalledWith(
      { userMessage: 'test', modelOverrides: overrides },
      expect.any(Function),
      expect.any(Function),
      expect.any(Function),
      expect.any(Function),
    );
  });
});
