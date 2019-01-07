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
-->

<template>
    <span class="location-input">
        <input type="text" v-model="location">
        <ul
            class="location-autocomplete"
            v-show="showAutocomplete && matches.length > 0">
            <li
                v-for="match in matches"
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

export default Vue.extend({
    props: {
        mapboxToken: { type: String, required: true },
        defaultLocation: { type: String, required: false, default: '' },
    },
    data() {
        return {
            location: this.defaultLocation ? this.defaultLocation : '',
            matches: [],
            // Will be set to true when the next change of the location <input>
            // field should be ignored.
            skipAutocomplete: false,
            showAutocomplete: false,
        }
    },
    mounted() {
        this.geocodingClient = mbxGeocoding({ accessToken: this.mapboxToken });

        // Detects when the user click outside of the input and autocomplete
        // box.
        let inputEl = this.$el.querySelector('input[type="text"]');
        let autocompleteEl = this.$el.querySelector('.location-autocomplete');

        self = this
        this.clickEventListener = function(event) {
            let target = event.target;
            if (!autocompleteEl.contains(target) && target != inputEl) {
                self.showAutocomplete = false;
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
                    limit: 5,
                })
                .send()
                .then(response => {
                    this.showAutocomplete = true;
                    this.matches = response.body.features;
                });
        }, 300),

        // Sets the location input value to the given Mapbox match.
        setLocation(match) {
            this.skipAutocomplete = true;
            this.matches = [];
            this.showAutocomplete = false;

            this.location = match.place_name;
        },
    },
    watch: {
        location: function(oldVal, newVal) {
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
}

.location-input input[type="text"] {

}

.location-input .location-autocomplete {
    /* Position the autocomplete results below the input field. */
    position: absolute;
    top: 100%;
    left: 0;

    padding: 0;
    margin: 0;

    z-index: 99;

    background-color: grey;
    border: 1px solid black;
}
</style>
