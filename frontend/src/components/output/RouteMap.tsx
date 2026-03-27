import { useEffect } from 'react';
import { MapContainer, TileLayer, GeoJSON, Marker, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';
import type { RouteResult } from '../../types/pipeline';

// Fix default marker icons (Leaflet + bundler issue)
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png';
import markerIcon from 'leaflet/dist/images/marker-icon.png';
import markerShadow from 'leaflet/dist/images/marker-shadow.png';

L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
});

interface Props {
  route: RouteResult;
}

function FitBounds({ route }: Props) {
  const map = useMap();

  useEffect(() => {
    if (route.waypoints.length > 0) {
      const bounds = L.latLngBounds(
        route.waypoints.map(wp => [wp.lat, wp.lon] as [number, number])
      );
      map.fitBounds(bounds, { padding: [40, 40] });
    }
  }, [map, route.waypoints]);

  return null;
}

export function RouteMap({ route }: Props) {
  const center = route.waypoints.length > 0
    ? [route.waypoints[0].lat, route.waypoints[0].lon] as [number, number]
    : [47.5, 7.6] as [number, number];

  return (
    <div className="rounded-lg border border-gray-200 bg-white shadow-sm">
      <div className="border-b border-gray-100 bg-gray-50 px-4 py-3">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-semibold text-gray-700">Routenkarte</h2>
          {route.totalDistanceKm > 0 && (
            <span className="text-xs text-gray-500">
              Gesamtdistanz: {route.totalDistanceKm.toFixed(1)} km
            </span>
          )}
        </div>
      </div>
      <div style={{ height: '450px' }}>
        <MapContainer
          center={center}
          zoom={10}
          style={{ height: '100%', width: '100%', borderRadius: '0 0 0.5rem 0.5rem' }}
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
          />

          <FitBounds route={route} />

          {route.geojson && (
            <GeoJSON
              data={route.geojson as GeoJSON.GeoJsonObject}
              style={{ color: '#2563eb', weight: 4, opacity: 0.8 }}
            />
          )}

          {route.waypoints.map((wp) => (
            <Marker key={`${wp.name}-${wp.dayNumber}`} position={[wp.lat, wp.lon]}>
              <Popup>
                <strong>Tag {wp.dayNumber}</strong><br />
                {wp.name}
              </Popup>
            </Marker>
          ))}
        </MapContainer>
      </div>
    </div>
  );
}
