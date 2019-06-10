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
        <div class="cell large-6">
            <div class="grid-y">
                <div class="cell">
                    <label>
                        Country

                        <select
                            :name="countryName"
                            v-model="address.country"
                            @change="updateMarker()"
                            required>
                            <option disabled value="">Please select a country</option>
                            <option
                                v-for="country in orderedCountries"
                                :value="country.isoCode">
                                {{ country.name }}
                            </option>
                        </select>

                        <span v-if="countryError" class="error">
                            {{ countryError }}
                        </span>
                    </label>
                </div>

                <div class="cell">
                    <label>
                        Street address

                        <input
                            type="text"
                            :name="address1Name"
                            v-model="address.address1"
                            :disabled="!address.country"
                            @change="updateMarker()"
                            required>

                        <span v-if="address1Error" class="error">
                            {{ address1Error }}
                        </span>
                    </label>
                </div>

                <div class="cell">
                    <label>
                        Street address (line 2)

                        <input
                            type="text"
                            :name="address2Name"
                            v-model="address.address2"
                            :disabled="!address.country"
                            @change="updateMarker()">

                        <span v-if="address2Error" class="error">
                            {{ address2Error }}
                        </span>
                    </label>
                </div>

                <div class="cell">
                    <div class="grid-x grid-margin-x">
                        <div class="cell medium-4">
                            <label>
                                City

                                <input
                                    type="text"
                                    :name="cityName"
                                    v-model="address.city"
                                    :disabled="!address.country"
                                    @change="updateMarker()"
                                    required>

                                <span v-if="cityError" class="error">
                                    {{ cityError }}
                                </span>
                            </label>
                        </div>

                        <div class="cell medium-3">
                            <label>
                                Zipcode

                                <input
                                    type="text"
                                    :name="zipcodeName"
                                    v-model="address.zipcode"
                                    :disabled="!address.country"
                                    @change="updateMarker()"
                                    required>

                                <span v-if="zipcodeError" class="error">
                                    {{ zipcodeError }}
                                </span>
                            </label>
                        </div>

                        <div class="cell medium-5">
                            <label>
                                State/Province

                                <select
                                    :name="stateName"
                                    v-model="address.state"
                                    :disabled="!hasStates(address.country)"
                                    :required="hasStates(address.country)"
                                    @change="updateMarker()">
                                    <option
                                        v-if="hasStates(address.country)"
                                        value="" disabled>
                                        Please select a state or province
                                    </option>
                                    <option
                                        v-for="state in orderedStates"
                                        :value="state.code">
                                        {{ state.name }}
                                    </option>
                                </select>

                                <span v-if="stateError" class="error">
                                    {{ stateError }}
                                </span>
                            </label>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="cell large-6 address-map-container">
            <div class="address-map" ref="map"></div>

            <div class="address-map-overlay" v-show="hasMarker">
                Move the <i class="fi-marker"></i> marker to match the exact
                location of your studio.
            </div>

            <input type="hidden" name="long" :value="coordinates.long">
            <input type="hidden" name="lat" :value="coordinates.lat">
        </div>
    </div>
</template>

<script lang="ts">
import * as mapboxgl from 'mapbox-gl';
import * as mbxGeocoding from '@mapbox/mapbox-sdk/services/geocoding';
import * as _ from "lodash";
import Vue from "vue";

declare var NC_CONFIG: any;

export default Vue.extend({
    props: {
        // Form field names and values

        countryName: { type: String, required: true },
        country: { type: String, required: false },
        countryError: { type: String, required: false },

        address1Name: { type: String, required: true },
        address1: { type: String, required: false },
        address1Error: { type: String, required: false },

        address2Name: { type: String, required: true },
        address2: { type: String, required: false },
        address2Error: { type: String, required: false },

        cityName: { type: String, required: true },
        city: { type: String, required: false },
        cityError: { type: String, required: false },

        zipcodeName: { type: String, required: true },
        zipcode: { type: String, required: false },
        zipcodeError: { type: String, required: false },

        stateName: { type: String, required: true },
        state: { type: String, required: false },
        stateError: { type: String, required: false },

        longName: { type: String, required: true },
        long: { type: Number, required: false },

        latName: { type: String, required: true },
        lat: { type: Number, required: false },
    },
    data() {
        return {
            address: {
                country: this.country,
                address1: this.address1,
                address2: this.address2,
                city: this.city,
                zipcode: this.zipcode,
                state: this.state,
            },

            coordinates: {
                long: this.long,
                lat: this.lat,
            },

            // The last Geocoding object returned by Mapbox.
            place: null,

            // If set to `false`, the marker position will not be updated when
            // the address changes. Will be automatically set to `false` once
            // the marker has been manually dragged.
            shouldUpdateMarker: !this.long || !this.lat,
        }
    },
    mounted() {
        mapboxgl.accessToken = NC_CONFIG.mapboxToken;

        this.map = new mapboxgl.Map({
            container: this.$refs.map,
            style: 'mapbox://styles/mapbox/streets-v10',
        });

        this.map.addControl(new mapboxgl.NavigationControl());

        this.marker = new mapboxgl.Marker({
                color: '#b37216',
                draggable: true,
            });
        this.marker.on('drag', this.dragMarker);

        this.geocodingClient = mbxGeocoding({
            accessToken: NC_CONFIG.mapboxToken
        });

        if (!this.long || !this.lat) {
            this.updateMarker();
        } else {
            this.setMarker({
                center: [this.long, this.lat],
            });
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

        // Returns the list of states/provinces of the currently selected
        // country, if it has any.
        states() {
            if (this.hasStates(this.address.country)) {
                return NC_CONFIG.countries[this.address.country].states;
            } else {
                return null;
            }
        },

        // Available countries, sorted by name.
        orderedCountries() {
            return Object.values(NC_CONFIG.countries)
                .sort((lhs: any, rhs: any) => lhs.name.localeCompare(rhs.name));
        },

        // States of the currently selected country, sorted by name.
        orderedStates() {
            if (this.hasStates(this.address.country)) {
                return Object.keys(this.states)
                    .map(code => ({ code: code, name: this.states[code] }))
                    .sort((lhs: any, rhs: any) =>
                        lhs.name.localeCompare(rhs.name));
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
            if (!this.shouldUpdateMarker) {
                return;
            }

            // Constructs the query from address components, sorted in a
            // hierarchical way (i.e. from the most component to the most
            // precise one). Does not continue to construct the query once a
            // required component is missing (e.g. will not add the address if
            // the city is not provided).

            let fieldHierachy = [
                { value: this.countryFullname, required: true },
                {
                    value: this.address.state,
                    required: this.hasStates(this.address.country) },
                { value: this.address.zipcode, required: !this.address.city },
                { value: this.address.city, required: !this.address.zipcode },
                { value: this.address.address2, required: false },
                { value: this.address.address1, required: true },
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
                .then(resp => { this.setMarker(resp.body.features[0]); });
        }, 300),

        // Handles a marker drag event. Disable automatic updates of the marker
        // position.
        dragMarker() {
            var pos = this.marker.getLngLat();
            this.shouldUpdateMarker = false;
        },

        // Resets the map and marker position to the pre-drag address.
        resetMarker() {
            this.shouldUpdateMarker = true;
            this.setMarker(this.place);
        },

        // Sets the marker to the given place and fly the map to its position.
        setMarker(place) {
            let hadPlace = this.place;
            this.place = place;

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
                this.map.fitBounds(
                    [[bbox[0], bbox[1]],[bbox[2], bbox[3]]]
                );
            } else {
                // Some features don't have bounding-boxes, centers the
                // map on location and use a default zoom.
                this.map.flyTo({
                    center: place.center,
                    zoom: 15,
                });
            }
        },
    },
    watch: {
        'address.country': function() {
            // Resets the state field on country change.
            this.address.state = null;
        }
    }
});
</script>

<style>
.address-map-container {
    position: relative;
}

.address-map {
    height: 300px;
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
