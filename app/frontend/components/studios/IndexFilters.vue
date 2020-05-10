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
    <div class="grid-x grid-padding-x">
        <div class="cell small-12 medium-6">
            <label>
                Location
                <location-input v-model="location">
                </location-input>
            </label>
        </div>

        <div class="cell small-12 medium-6 hide-for-small-only">
            <label>
                Available on

                <input
                    type="date"
                    v-model="availableOn"
                    :min="currentDate"
                    pattern="[0-9]{4}-[0-9]{2}-[0-9]{2}"
                    placeholder="YYYY-MM-DD">
            </label>
        </div>
    </div>
</template>

<script lang="ts">
import * as _ from "lodash";
import Vue from "vue";

import * as moment from 'moment';

import LocationInput from '../widgets/LocationInput.vue'

export default Vue.extend({
    props: {
        // A object containing the filter values
        //
        // Example:
        // --------
        // {
        //     // A GeoJSON feature that contains a `place_name` field.
        //     location: {
        //         place_name: 'Amsterdam, Noord-Holland, Netherlands',
        //     },
        //     availableOn: '2020-03-06'
        // }
        value: { type: Object, required: false },
    },
    data() {
        return {
            location: this.value.location,
            availableOn: this.value.availableOn,
        };
    },
    computed: {
        currentDate() {
            return moment().format('YYYY-MM-DD');
        },
    },
    methods: {
        emitValueChanged() {
            this.$emit('input', {
                location: this.location,
                availableOn: this.availableOn,
            });
        }
    },
    watch: {
        location() {
            this.$emit('input-location', this.location);
            this.emitValueChanged();
        },
        availableOn() {
            this.$emit('input-available-on', this.availableOn);
            this.emitValueChanged();
        },
    },
    components: { LocationInput }
});
</script>

<style scoped>
label {
    margin-top: 0.6rem;
}
</style>
