<!--
  Noisycamp is a platform for booking music studios.
  Copyright (C) 2019  Raphael Javaux <raphaeljavaux@gmail.com>

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <https://www.gnu.org/licenses/>.

  Provides an address widget with a map.
-->

<template>
    <div class="grid-x grid-margin-x">
        <div class="cell small-12">
            <address-input
                :name="fieldName('address')"
                v-model="address"
                :errors="errors['address']">
            </address-input>
        </div>

        <div class="cell small-12 address-map-container">
            <div class="address-map" ref="map"></div>

            <div class="address-map-overlay" v-show="hasMarker">
                Move the <i class="fi-marker"></i> marker to match the exact
                location of your studio
                <a
                    v-if="!shouldUpdateMarker"
                    @click="resetMarker()">
                    (reset)
                </a>.

            </div>

            <input type="hidden" :name="fieldName('coordinates.long')" :value="coordinates.long">
            <input type="hidden" :name="fieldName('coordinates.lat')" :value="coordinates.lat">
        </div>
    </div>
</template>

<script lang="ts">
import * as mapboxgl from 'mapbox-gl';
import * as mbxGeocoding from '@mapbox/mapbox-sdk/services/geocoding';
import * as _ from "lodash";
import Vue from "vue";

import VueInput from '../../../widgets/VueInput';
import AddressInput from './AddressInput.vue';

declare var NC_CONFIG: any;

export default Vue.extend({
    mixins: [VueInput],
    props: {
        value: {
            type: Object,
            default() {
                return {
                    address: {},
                    coordinates: {}
                }
            }
        }, // Overiddes VueInput
    },
    data() {
        return {
            address: this.value.address,
            coordinates: this.value.coordinates,

            // The last Geocoding object returned by Mapbox.
            place: null,

            // If set to `false`, the marker position will not be updated when
            // the address changes. Will be automatically set to `false` once
            // the marker has been manually dragged.
            shouldUpdateMarker: true,
        }
    },
    mounted() {
        // Initializes Mapbox map and geocoding plugins.
        {
            mapboxgl.accessToken = NC_CONFIG.mapboxToken;

            this.map = new mapboxgl.Map({
                container: this.$refs.map,
                style: 'mapbox://styles/mapbox/streets-v10',
            });

            this.map.addControl(new mapboxgl.NavigationControl());

            this.marker = new mapboxgl.Marker({
                color: 'var(--accent-color)',
                draggable: true,
            });
            this.marker.on('drag', this.dragMarker);

            this.geocodingClient = mbxGeocoding({
                accessToken: NC_CONFIG.mapboxToken
            });

            if (!this.coordinates.long || !this.coordinates.lat) {
                this.updateMarker();
            } else {
                this.setMarker({
                    center: [this.coordinates.long, this.coordinates.lat],
                }, false);
            }
        }
    },
    computed: {
        countryFullname() {
            if (this.address.country) {
                return NC_CONFIG.countries[this.address.country].name;
            } else {
                return null;
            }
        },

        // Returns `true` if a marker being displayed in the map.
        //
        // No marker is displayed on the map until the user select a country.
        hasMarker() {
            return this.place ? true : false;
        }
    },
    methods: {
        hasStates(country) {
            return country && NC_CONFIG.countries[country].states;
        },

        // Updates the location of the marker and map with the currently
        // provided address.
        updateMarker: _.debounce(function () {
            // Constructs the query from address components, sorted in a
            // hierarchical way (i.e. from the most component to the most
            // precise one). Does not continue to construct the query once a
            // required component is missing (e.g. will not add the address if
            // the city is not provided).

            let fieldHierachy = [
                { value: this.countryFullname, required: true },
                {
                    value: this.address['state'],
                    required: this.hasStates(this.address['country']) },
                { value: this.address['zipcode'], required: !this.address['city'] },
                { value: this.address['city'], required: !this.address['zipcode'] },
                { value: this.address['address-2'], required: false },
                { value: this.address['address-1'], required: true },
            ];

            var query: string[] = [];

            for (let field of fieldHierachy) {
                if (!field.value && field.required) {
                    break;
                }

                if (field.value) {
                    query.push(field.value);
                }
            }
            query.reverse();

            if (query.length < 1) {
                return;
            }

            // Queries Mapbox for the location.

            this.geocodingClient
                .forwardGeocode({
                    query: query.join(', '),
                    autocomplete: false,
                    limit: 1,
                })
                .send()
                .then(resp => {
                    if (resp.body.features.length > 0) {
                        this.setMarker(resp.body.features[0]);
                    }
                });
        }, 300),

        // Handles a marker drag event. Disable automatic updates of the marker
        // position with address changes.
        dragMarker() {
            let pos = this.marker.getLngLat();
            this.coordinates = { long: pos.lng, lat: pos.lat };
            this.shouldUpdateMarker = false;
        },

        // Resets the map and marker position to the pre-drag address.
        resetMarker() {
            this.shouldUpdateMarker = true;
            this.setMarker(this.place);
        },

        // Sets the marker to the given place and fly the map to its position.
        setMarker(place, animate: boolean = true) {
            let hadPlace = this.place;
            this.place = place;

            if (!this.shouldUpdateMarker) {
                return;
            }

            this.marker.setLngLat(place.center);

            this.coordinates.long = place.center[0];
            this.coordinates.lat = place.center[1];

            if (!hadPlace) {
                // For some reason, MapboxGL crashes if we don't create
                // the marker at the same time as the map. However, we
                // don't have to link it to the map until we get the
                // first Geocode place though.
                this.marker.addTo(this.map);
            }

            if (place.bbox) {
                let bbox = place.bbox;
                let bounds = [[bbox[0], bbox[1]],[bbox[2], bbox[3]]];

                if (animate) {
                    this.map.fitBounds(bounds);
                } else {
                    this.map.fitBounds(bounds, { duration: 0 });
                }
            } else {
                // Some features don't have bounding-boxes, centers the
                // map on location and use a default zoom.
                let options = {
                    center: place.center,
                    zoom: 15,
                };

                if (!animate) {
                    options['duration'] = 0;
                }

                this.map.flyTo(options);
            }
        },

        // Emits the new address and coordinate value as an 'input' signal.
        emitValueChanged() {
            this.$emit('input', {
                address: this.address,
                coordinates: this.coordinates
            });
        }
    },
    watch: {
        'address': {
            deep: true,
            handler() {
                this.updateMarker();
                this.emitValueChanged();
            }
        },

        'coordinates': {
            deep: true,
            handler() {
                this.emitValueChanged();
            }
        },
    },
    components: { AddressInput },
});
</script>

<style>
.address-map-container {
    position: relative;
}

.address-map {
    height: 370px;
}

.address-map-overlay {
    position: absolute;
    left: 0;
    top: 0;

    margin-top: 10px;
    margin-left: 10px;
    margin-right: 60px;
    padding: 3px 5px;

    border-radius: 4px;

    background: #fff;
    box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.1);

    font-size: 0.95rem;
}
</style>
