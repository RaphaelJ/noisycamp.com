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
    <div>
        <div class="grid-container section">
            <div class="grid-x grid-padding-x">
                <div class="medium-6 cell">
                    <label>
                        Location
                        <location-input
                            :mapbox-token="mapboxToken"
                            default-location="Amsterdam">
                        </location-input>
                    </label>

                    <p class="help-text">
                        Or use your
                        <a href="#">current location &nbsp;<i class="fi-marker"></i></a>
                    </p>
                </div>

                <div class="medium-6 cell">
                    <label>
                        Dates
                        <input type="text">
                    </label>

                    <p class="help-text">
                        Will only show studios that are available on at least
                        one of these days
                    </p>
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import * as mbxGeocoding from '@mapbox/mapbox-sdk/services/geocoding';
import * as _ from "lodash";
import Vue from "vue";

import LocationInput from '../widgets/LocationInput.vue'

export default Vue.extend({
    props: {
        mapboxToken: { type: String, required: true },
        studios: { type: Array, required: true }
    },
    data() {
        return {
        }
    },
    components: { LocationInput },
});
</script>
