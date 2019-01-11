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
    <div class="grid-container-fluid">
        <div class="grid-x">
            <div class="cell medium-12 large-7">
                <div class="section">
                    <div class="grid-x grid-padding-x">
                        <div class="medium-6 cell">
                            <label>
                                Location
                                <location-input
                                    :mapbox-token="mapboxToken"
                                    v-model="location">
                                </location-input>
                            </label>
                        </div>

                        <div class="medium-6 cell">
                            <label>
                                Dates
                                <input type="text">
                            </label>

                            <p class="help-text">
                                Will only list studios that are available on at least
                                one of these days
                            </p>
                        </div>
                    </div>
                </div>

                <div class="grid-x grid-padding-x">
                    <div class="cell small-12">
                        <studios-index-listing :studios="studios">
                        </studios-index-listing>
                    </div>
                </div>
            </div>

            <div class="cell large-5 show-for-large">
                <studios-index-map
                    ref="map"
                    :mapbox-token="mapboxToken"
                    :studios="studios">
                </studios-index-map>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import Vue from "vue";

import LocationInput from '../widgets/LocationInput.vue'
import StudiosIndexListing from './IndexListing.vue';
import StudiosIndexMap from './IndexMap.vue';

export default Vue.extend({
    props: {
        mapboxToken: { type: String, required: true }
    },
    data() {
        return {
            location: null,
            studios: [
                {
                    price: 1400,
                    location: [4.81032652885753, 52.37886749365077],
                    name: 'My Studio',
                    pictures: [
                        'https://images.pexels.com/photos/96380/pexels-photo-96380.jpeg?auto=compress&cs=tinysrgb&dpr=2&w=500',
                    ]
                },
                {
                    price: 2400,
                    location: [4.948984376124656, 52.36697405119352],
                    name: 'My other studio',
                    pictures: [
                        'https://images.pexels.com/photos/265672/pexels-photo-265672.png?auto=compress&cs=tinysrgb&dpr=2&w=500',
                    ]
                },
                {
                    price: 1239,
                    location: [4.8922607113389915, 52.37746843135494],
                    name: 'Super studio',
                    pictures: [
                        'https://images.pexels.com/photos/995301/pexels-photo-995301.jpeg?auto=compress&cs=tinysrgb&dpr=2&w=500',
                        'https://images.pexels.com/photos/265672/pexels-photo-265672.png?auto=compress&cs=tinysrgb&dpr=2&w=500',
                    ]
                },
                {
                    price: 2300,
                    location: [4.861320530534101, 52.37152192207486],
                    name: 'Awesome place',
                    pictures: [
                        'https://images.pexels.com/photos/744318/pexels-photo-744318.jpeg?auto=compress&cs=tinysrgb&dpr=2&w=500',
                    ]
                },
            ]
        }
    },
    watch: {
        location(place) {
            this.$refs.map.setLocation(place);
        }
    },
    components: { LocationInput, StudiosIndexListing, StudiosIndexMap }
});
</script>
