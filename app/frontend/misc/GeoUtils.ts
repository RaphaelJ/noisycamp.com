/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2020  Raphael Javaux <raphaeljavaux@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import * as mapboxgl from 'mapbox-gl';

// Returns a string version of a BBox-like object.
export function serializeBBox(bbox) {
    bbox = mapboxgl.LngLatBounds.convert(bbox);
    return `${bbox.getNorth()},${bbox.getSouth()},` +
        `${bbox.getWest()},${bbox.getEast()}`;
}

// Parses a BBox-like object encoded as a string.
export function unserializeBBox(str) {
    if (str == null) {
        return null
    }

    let values = str.split(',');

    let north = parseFloat(values[0]),
        south = parseFloat(values[1]),
        west = parseFloat(values[2]),
        east = parseFloat(values[3]);

    return new mapboxgl.LngLatBounds(
        new mapboxgl.LngLat(west, south),
        new mapboxgl.LngLat(east, north)
    );
}

// Returns a string version of a LngLat object.
export function serializeCenter(center) {
    center = mapboxgl.LngLat.convert(center);
    return `${center.lng},${center.lat}`;
}

// Parses a LngLat-like object encoded as a string.
export function unserializeCenter(str) {
    if (str == null) {
        return null
    }

    let values = str.split(',');

    let long = parseFloat(values[0]),
        lat = parseFloat(values[1]);

    return new mapboxgl.LngLat(long, lat);
}
