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

  Provides an input field with an address autocompletion.
-->

<template>
    <span class="location-input">
        <input
            type="text"
            v-model="location"
            @focus="showAutocomplete = true"
            @keyup.enter="keyEnter()"
            @keyup.down="keyDown()"
            @keyup.up="keyUp()">
        <ul
            class="location-autocomplete"
            v-show="showAutocomplete">

            <li
                @click="setCurrentLocation()"
                :class="{ selected: selected == 0 }">

                <i class="fi-marker"></i>&nbsp;&nbsp;

                <span v-if="currentLocationError">
                    Unable to retrieve your location
                </span>
                <span v-else>
                    Current location
                    <span v-if="currentLocation != null">
                        ({{ currentLocation.place_name }})
                    </span>
                </span>
            </li>

            <li
                v-for="(match, index) in matches"
                :class="{ selected: index + 1 == selected }"
                @click="setLocation(match)">
                {{ match.place_name }}
            </li>
        </ul>
    </span>
</template>

<script lang="ts">
import * as mbxGeocoding from '@mapbox/mapbox-sdk/services/geocoding';
import * as _ from "lodash";
import Vue from "vue";

declare var NC_CONFIG: any;

export default Vue.extend({
    props: {
        // A GeoJSON feature that contains a `place_name` field.
        value: { type: Object, required: false },
    },
    data() {
        var location = '';

        if (this.value) {
            location = this.value['place_name'];
        }

        return {
            location: location,

            currentLocation: null,
            currentLocationError: false,

            matches: [],

            // Will be set to true when the next change of the location <input>
            // field should be ignored.
            skipAutocomplete: false,
            showAutocomplete: false,

            // The index of the autocomplete item that is currently selected.
            selected: null,
        }
    },
    mounted() {
        this.geocodingClient = mbxGeocoding({
            accessToken: NC_CONFIG.mapboxToken
        });

        // Detects when the user click outside of the input and autocomplete
        // box.
        let inputEl = this.$el.querySelector('input[type="text"]');
        let autocompleteEl = this.$el.querySelector('.location-autocomplete');

        let compnt = this;
        this.clickEventListener = function(event) {
            let target = event.target;
            if (!autocompleteEl.contains(target) && target != inputEl) {
                compnt.showAutocomplete = false;
            }
        }

        document.addEventListener('click', this.clickEventListener);
    },
    beforeDestroy() {
        document.removeEventListener('click', this.clickEventListener);
    },
    methods: {
        // Updates the autocomplete results with Mapbox.
        autocomplete: _.debounce(function () {
            this.geocodingClient
                .forwardGeocode({
                    query: this.location,
                    limit: 15,
                })
                .send()
                .then(response => {
                    this.resetAutocomplete();
                    this.showAutocomplete = true;
                    this.matches = response.body.features;
                });
        }, 300),

        resetAutocomplete() {
            this.matches = [];
            this.showAutocomplete = false;
            this.selected = null;
        },

        // Sets the location input value to the given Mapbox match.
        setLocation(place) {
            this.skipAutocomplete = true;
            this.resetAutocomplete();

            this.location = place.place_name;
            this.$emit('input', place);
        },

        setCurrentLocation() {
            if (!navigator.geolocation) {
                this.currentLocationError = true;
                return;
            }

            let compnt = this;

            function success(pos) {
                compnt.currentLocationError = false;

                compnt.geocodingClient
                    .reverseGeocode({
                        query: [pos.coords.longitude, pos.coords.latitude],
                        limit: 1,
                        types: [
                            'address', 'poi', 'country', 'region', 'district',
                            'place', 'locality', 'neighborhood'
                        ]
                    })
                    .send()
                    .then(response => {
                        if (response.body.features.length >= 0) {
                            compnt.currentLocationError = false;
                            compnt.currentLocation = response.body.features[0];
                            compnt.setLocation(compnt.currentLocation);
                        } else {
                            compnt.currentLocationError = true;
                        }
                    });
            }

            function error() {
                compnt.currentLocationError = true;
            }

            navigator.geolocation.getCurrentPosition(success, error);
        },

        // Handles keyboard ↑, ↓ and ↩︎ events to navigate autocomplete.

        keyEnter() {
            if (this.selected == null) {
                return;
            } else if (this.selected == 0) {
                this.setCurrentLocation();
            } else {
                this.setLocation(this.matches[this.selected - 1]);
            }
        },

        keyDown() {
            if (this.selected == null) {
                this.selected = 0;
            } else {
                this.selected = Math.min(
                    this.matches.length, this.selected + 1
                );
            }
        },

        keyUp() {
            if (this.selected == null) {
                return
            } else if (this.selected == 0) {
                this.selected = null;
            } else {
                this.selected = Math.max(0, this.selected - 1);
            }
        }
    },
    watch: {
        location(newVal, oldVal) {
            if (this.skipAutocomplete) {
                this.skipAutocomplete = false;
                return;
            }

            if (oldVal != newVal) {
                this.matches = [];
                this.autocomplete();
            }
        }
    },
});
</script>

<style>
.location-input {
    position: relative;

    display: block;
}

.location-input .location-autocomplete {
    /* Position the autocomplete results below the input field. */
    position: absolute;
    top: 100%;
    left: 1px;
    right: 1px;

    max-height: 300px;
    overflow-y: auto;

    padding: 0;
    margin: 0;
    border-radius: 0 0 3px 3px;
    box-shadow: 1px 1px 3px rgba(0, 0, 0, 0.2);
    background-color: white;

    z-index: 99;

    list-style: none;
}

.location-input .location-autocomplete li {
    border-bottom: 1px solid #b372161a;
    padding: 5px 10px;

    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;

    cursor: pointer;
}

.location-input .location-autocomplete li:hover {
    background-color: #b3721620;
}

.location-input .location-autocomplete li.selected {
    background-color: #b372164d;
}
</style>
