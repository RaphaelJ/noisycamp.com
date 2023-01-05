/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2022  Raphael Javaux <raphael@noisycamp.com>
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

// Returns a url-encoded string version of a GeoJSON feature object.
export function serializeFeature(feature) {
    let values = []

    if (feature['place_name']) {
        let encodedName = encodeURIComponent(feature['place_name'])
        values.push('location.place_name=' + encodedName)
    }

    if (feature['bbox']) {
        values.push('location.bbox=' + serializeFeatureBBox(feature))
    }

    if (feature['center']) {
        values.push('location.center=' + serializeFeatureCenter(feature))
    }

    return values.join('&')
}

// Similar as GeoUtils' serializeBBox, but does not require MapboxJS.
export function serializeFeatureBBox(feature) {
    let [west, north, east, south] = feature['bbox']

    return `${north},${south},${west},${east}`
}

// Similar as GeoUtils' serializeCenter, but does not require MapboxJS.
export function serializeFeatureCenter(feature) {
    let [long, lat] = feature['center']
    return `${long},${lat}`
}
