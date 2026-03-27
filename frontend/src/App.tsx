import { useState } from 'react';
import { TripRequestForm } from './components/input/TripRequestForm';
import { ModelSelector } from './components/input/ModelSelector';
import { PipelineView } from './components/pipeline/PipelineView';
import { FinalReport } from './components/output/FinalReport';
import { usePipeline } from './hooks/usePipeline';

function App() {
  const { state, run, reset } = usePipeline();
  const [modelOverrides, setModelOverrides] = useState<Record<string, string>>({});

  const handleSubmit = (message: string) => {
    run(message, Object.keys(modelOverrides).length > 0 ? modelOverrides : undefined);
  };

  const lastStep = state.steps[state.steps.length - 1];
  const finalReport = state.status === 'complete' && lastStep ? lastStep.output : '';

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="border-b border-gray-200 bg-white shadow-sm">
        <div className="mx-auto max-w-5xl px-4 py-4">
          <h1 className="text-2xl font-bold text-gray-900">
            Biketrip Advisor
          </h1>
          <p className="text-sm text-gray-500">
            Multi-Agent LLM Showcase — 4 spezialisierte KI-Agenten planen deine Radtour
          </p>
        </div>
      </header>

      <main className="mx-auto max-w-5xl space-y-6 px-4 py-6">
        <section className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
          <h2 className="mb-3 text-sm font-semibold text-gray-700">Modell-Konfiguration</h2>
          <ModelSelector
            overrides={modelOverrides}
            onChange={setModelOverrides}
            disabled={state.status === 'running'}
          />
        </section>

        <section className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
          <h2 className="mb-3 text-sm font-semibold text-gray-700">Deine Anfrage</h2>
          <TripRequestForm
            onSubmit={handleSubmit}
            disabled={state.status === 'running'}
          />
        </section>

        {state.status !== 'idle' && (
          <section>
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-sm font-semibold text-gray-700">Pipeline-Fortschritt</h2>
              {state.status === 'complete' && (
                <button
                  onClick={reset}
                  className="text-xs text-blue-500 hover:text-blue-700"
                >
                  Neue Anfrage
                </button>
              )}
            </div>
            <PipelineView state={state} />
          </section>
        )}

        {finalReport && (
          <section>
            <FinalReport content={finalReport} />
          </section>
        )}

        {state.status === 'complete' && state.steps.length > 0 && (
          <section className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
            <h2 className="mb-2 text-sm font-semibold text-gray-700">Performance-Vergleich</h2>
            <div className="grid grid-cols-4 gap-3">
              {state.steps.map(step => (
                <div key={step.role} className="rounded bg-gray-50 p-3 text-center">
                  <div className="text-xs text-gray-500">{step.role}</div>
                  <div className="text-sm font-mono font-bold text-gray-800">
                    {(step.durationMs / 1000).toFixed(1)}s
                  </div>
                  <div className="text-xs text-gray-400">{step.modelUsed}</div>
                </div>
              ))}
            </div>
          </section>
        )}
      </main>
    </div>
  );
}

export default App;
