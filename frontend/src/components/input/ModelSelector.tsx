import { useEffect, useState } from 'react';
import { fetchConfig, fetchModels } from '../../api/pipelineApi';
import { AGENT_ROLES } from '../../types/pipeline';
import type { AgentRole } from '../../types/pipeline';

interface Props {
  overrides: Record<string, string>;
  onChange: (overrides: Record<string, string>) => void;
  disabled: boolean;
}

export function ModelSelector({ overrides, onChange, disabled }: Props) {
  const [models, setModels] = useState<string[]>([]);
  const [defaults, setDefaults] = useState<Record<string, string>>({});

  useEffect(() => {
    fetchModels().then(setModels);
    fetchConfig().then(setDefaults);
  }, []);

  const handleChange = (role: AgentRole, model: string) => {
    const next = { ...overrides };
    if (model === '' || model === defaults[role]) {
      delete next[role];
    } else {
      next[role] = model;
    }
    onChange(next);
  };

  return (
    <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
      {AGENT_ROLES.map(({ role, label }) => {
        const selectId = `model-${role.toLowerCase()}`;
        return (
          <div key={role}>
            <label htmlFor={selectId} className="mb-1 block text-xs font-medium text-gray-500">
              {label}
            </label>
            <select
              id={selectId}
              value={overrides[role] || defaults[role] || ''}
              onChange={e => handleChange(role, e.target.value)}
              disabled={disabled || models.length === 0}
              className="w-full rounded border border-gray-300 bg-white px-2 py-1.5 text-sm text-gray-700 focus:border-blue-500 focus:outline-none disabled:opacity-50"
            >
              {models.length === 0 && defaults[role] && (
                <option value={defaults[role]}>{defaults[role]}</option>
              )}
              {models.map(m => (
                <option key={m} value={m}>
                  {m} {m === defaults[role] ? '(default)' : ''}
                </option>
              ))}
            </select>
          </div>
        );
      })}
    </div>
  );
}
