<!--
  Noisycamp is a platform for booking music studios.
  Copyright (C) 2019 2020 2021  Raphael Javaux <raphael@noisycamp.com>

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
        <h2 class="cell">
            Book a rehearsal or recording studio
        </h2>

        <div class="cell medium-12 large-auto">
            <label>
                Location
                <location-input ref="locationInput" v-model="location">
                </location-input>
            </label>

            <p class="help-text">
                Or use your
                <a @click="setCurrentLocation()">
                    current location&nbsp; <i class="fi-marker"></i>
                </a>
            </p>
        </div>

        <div class="cell medium-12 large-shrink">
            <label class="show-for-large">
                &nbsp;<!-- Force button alignment with input -->
            </label>
            <a
                :href="searchUrl"
                class="button primary large medium-down-expanded">
                <i class="fi-magnifying-glass"></i>&nbsp;
                Search
            </a>
        </div>
    </div>
</template>

<script lang="ts">
import Vue from "vue";

declare var NC_ROUTES: any;

import LocationInput from '../widgets/LocationInput.vue';

import { serializeFeature } from '../../misc/GeoUtils';

export default Vue.extend({
    props: {
    },
    data() {
        return {
            location: null
        }
    },
    computed: {
        searchUrl() {
            var url = NC_ROUTES.controllers.StudiosController.index().url;

            if (this.location) {
                url += '#' + serializeFeature(this.location);
            }

            return url;
        },
    },
    methods: {
        setCurrentLocation() {
            this.$refs.locationInput.setCurrentLocation();

            return false;
        }
    },
    components: { LocationInput }
});
</script>

<style>
</style>
