# Access Limits
Die Limits sind pro Tag (daily) und pro Minute (per minute) angegeben. Sie begrenzen Anfragen (Requests), um Fairness zu gewährleisten:


| Endpunkt | Daily Limit | Per Minute Limit |
|----------|-------------|------------------|
|Directions V2| 2000| 40 |
|Export V2|100|5|
|Isochrones V2|500|20|
|Matrix V2|500|40|
|Snap V2|2000|100|
|Elevation Line|200|40|
|Elevation Point|2000|100|
|Geocode Autocomplete|1000|100|
|Geocode Reverse|1000|100|
|Geocode Search|1000|100|
|Optimization|500|40|
|POIs|500|60|

**Directions V2** berechnet Routen zwischen Start- und Zielpunkten für Modi wie Auto, Fahrrad, Fußweg oder LKW. Es liefert Navigation, Entfernung, Zeit und Anweisungen unter Berücksichtigung von Einschränkungen wie Straßenarten oder Höchstgeschwindigkeit.

**Export V2** ermöglicht das Herunterladen von Routen, Isochronen oder Matrix-Daten als GeoJSON, GPX oder andere Formate. Nützlich für GIS-Software oder Offline-Nutzung.

**Isochrones V2** erzeugt Erreichbarkeitsbereiche (Konturen als Polygone) von einem Punkt aus in einer bestimmten Zeit (z. B. 30 Minuten zu Fuß) oder Distanz. Ideal für Analyse von zugänglichen Zonen.

**Matrix V2** berechnet Zeiten und Distanzen zwischen mehreren Ursprungs- und Zielpunkten (one-to-many oder many-to-many). Skalierbar für bis zu 2500 Paare, ohne detaillierte Routen.

**Snap V2** "schnappt" GPS-Punkte an die nächste Straße im Netzwerk (innerhalb eines Radius). Wird intern für präzise Routing-Vorbereitung genutzt, z. B. in Batch-Anfragen.

**Elevation Line** gibt Höhenprofile entlang einer Linie oder Route zurück (z. B. für Wanderkarten). Limitierter Einsatz durch hohe Rechenlast.

**Elevation Point** liefert Höhenwerte für einzelne Punkte. Schnell für viele Koordinaten, ergänzt Karten mit Geländedaten.

**Geocode Autocomplete** schlägt Adressen oder Orte vor, während du tippst (z. B. "Züri" → "Zürich, Schweiz"). Für Suchfelder in Apps.

**Geocode Reverse** wandelt Koordinaten (Lat/Lng) in Adressen um (z. B. 47.377, 8.539 → "Bahnhofstrasse, Zürich").

**Geocode Search** sucht Orte anhand von Text (z. B. "Zürich Hauptbahnhof") und gibt Koordinaten zurück.

**Optimization** löst Vehicle-Routing-Probleme (z. B. Tourenplanung für Flotten). Optimiert Jobs/Shipments mit Fahrzeug-, Zeit- und Kapazitätsbeschränkungen.

**POIs** sucht Points of Interest (z. B. Restaurants, Parks) in einem Bereich, Pfad oder Geofence. Unterstützt Filter wie Kategorien, Barrierefreiheit und Statistiken.