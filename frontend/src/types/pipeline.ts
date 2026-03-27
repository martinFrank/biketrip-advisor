export type AgentRole = 'CHAT' | 'REASONING' | 'PLANNING' | 'LANGUAGE';

export interface AgentStepResult {
  role: AgentRole;
  modelUsed: string;
  input: string;
  output: string;
  durationMs: number;
}

export interface RouteWaypoint {
  name: string;
  lat: number;
  lon: number;
  dayNumber: number;
}

export interface RouteResult {
  waypoints: RouteWaypoint[];
  geojson: GeoJSON.GeoJsonObject | null;
  totalDistanceKm: number;
}

export interface PipelineResult {
  steps: AgentStepResult[];
  finalReport: string;
  route: RouteResult | null;
}

export interface PipelineRequest {
  userMessage: string;
  modelOverrides?: Record<string, string>;
}

export type PipelineStatus = 'idle' | 'running' | 'complete' | 'error';

export interface PipelineState {
  status: PipelineStatus;
  steps: AgentStepResult[];
  route: RouteResult | null;
  error: string | null;
}

export const AGENT_ROLES: { role: AgentRole; label: string; description: string }[] = [
  { role: 'CHAT', label: 'Chat + RAG', description: 'Erfasst die Anfrage und reichert sie mit Kontext an' },
  { role: 'REASONING', label: 'Reasoning', description: 'Analysiert Machbarkeit und Risiken' },
  { role: 'PLANNING', label: 'Planning', description: 'Erstellt einen detaillierten Tagesplan' },
  { role: 'LANGUAGE', label: 'Language', description: 'Formuliert den finalen Reisebericht' },
];
