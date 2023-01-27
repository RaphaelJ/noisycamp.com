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

// Similar to MapboxGL BBox, Center and Feature objects, but does not require the dependency to the
// library.

export class BBox {
    readonly north: number;
    readonly south: number;
    readonly west: number;
    readonly east: number;

    constructor(north: number, south: number, west: number, east: number) {
        this.north = north;
        this.south = south;
        this.west = west;
        this.east = east;
    }

    toGeoJSON(): number[] {
        return [this.west, this.south, this.east, this.north];
    }

    static fromGeoJSON(value: number[]): BBox {
        return new BBox(value[3], value[1], value[0], value[2]);
    }

    static fromMapboxBBox(value): BBox {
        return new BBox(value.getNorth(), value.getSouth(), value.getWest(), value.getEast());
    }

    serialize(): string {
        return `${this.north},${this.south},${this.west},${this.east}`;
    }

    static unserialize(value: string): BBox {
        if (!value) {
            return null;
        }

        let values = value.split(',');

        let north = parseFloat(values[0]),
            south = parseFloat(values[1]),
            west = parseFloat(values[2]),
            east = parseFloat(values[3]);

        return new BBox(north, south, west, east);
    }
}

export class Coordinates {
    readonly long: number;
    readonly lat: number;

    constructor(long: number, lat: number) {
        this.long = long;
        this.lat = lat;
    }

    toGeoJSON(): number[] {
        return [this.long, this.lat];
    }

    static fromGeoJSON(value: number[]): Coordinates {
        return new Coordinates(value[0], value[1]);
    }

    static fromMapboxCenter(value): Coordinates {
        return new Coordinates(value.lng, value.lat);
    }

    serialize(): string {
        return `${this.long},${this.lat}`;
    }

    static unserialize(value: string): Coordinates {
        if (!value) {
            return null;
        }

        let values = value.split(',');

        let long = parseFloat(values[0]),
            lat = parseFloat(values[1]);

        return new Coordinates(long, lat);
    }
}

export class Feature {
    place_name?: string;
    bbox?: BBox;
    center?: Coordinates;

    static fromGeoJSON(value): Feature {
        let feature = new Feature();

        if (value['place_name']) {
            feature.place_name = value['place_name'];
        }

        if (value['bbox']) {
            feature.bbox = BBox.fromGeoJSON(value['bbox']);
        }

        if (value['center']) {
            feature.center = Coordinates.fromGeoJSON(value['center']);
        }

        return feature;
    }

    serialize() {
        let values = [];

        if (this.place_name) {
            let encodedName = encodeURIComponent(this.place_name);
            values.push(`location.place_name=${encodedName}`);
        }

        if (this.bbox) {
            values.push(`location.bbox=${this.bbox.serialize()}`);
        }

        if (this.center) {
            values.push(`location.center=${this.center.serialize()}`)
        }

        return values.join('&')
    }

    static unserialize(value: string): Feature {
        let feature = new Feature();

        // Extracts the query string parameters as a JS Object.
        let params = {};
        value.
            split('&').
            forEach(str => {
                let keyValue = str.split('=');

                if (keyValue.length == 2) {
                    params[keyValue[0]] = decodeURIComponent(keyValue[1]);
                }
            });

        if (params['location.place_name']) {
            feature.place_name = params['location.place_name'];
        }

        if (params['location.bbox']) {
            feature.bbox = BBox.unserialize(params['location.bbox']);
        }

        if (params['location.center']) {
            feature.center = Coordinates.unserialize(params['location.center']);
        }

        return feature;
    }
}
