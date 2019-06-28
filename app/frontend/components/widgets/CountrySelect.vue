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

  Provides a select input for countries.
-->

<template>
    <select
        :name="name"
        v-model="country"
        :required="required">
        <option disabled value="">Please select a country</option>
        <option
            v-for="c in orderedCountries"
            :value="c.isoCode">
            {{ c.name }}
        </option>
    </select>
</template>

<script lang="ts">
import * as _ from "lodash";
import Vue from "vue";

declare var NC_CONFIG: any;

export default Vue.extend({
    props: {
        name: { type: String, required: true },
        value: { type: String, required: false }, // Country ISO code.
        required: { type: Boolean, required: false },
    },
    data() {
        return {
            country: this.value,
        }
    },
    computed: {
        // Available countries, sorted by name.
        orderedCountries() {
            return Object.values(NC_CONFIG.countries)
                .sort((lhs: any, rhs: any) => lhs.name.localeCompare(rhs.name));
        },
    },
    watch: {
        country(val) {
            this.$emit('input', val);
        }
    },
});
</script>

<style>
</style>
