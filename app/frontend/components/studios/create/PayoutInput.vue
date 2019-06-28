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

  Provides an set of input widget for
-->

<template>
    <div class="grid-x grid-margin-x">
        <div class="cell large-12">
            <label>Billing country</label>

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
        </div>
    </div>
</template>

<script lang="ts">
import * as _ from "lodash";
import Vue from "vue";

declare var NC_CONFIG: any;

export default Vue.extend({
    props: {
    },
    data() {
        return {
            country: null;
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
    },
    methods: {
    },
});
</script>

<style>

</style>
