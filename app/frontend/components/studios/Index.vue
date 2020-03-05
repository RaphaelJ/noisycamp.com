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
    <div class="grid-x index">
        <div class="cell medium-12 large-5">
            <div class="grid-y grid-padding-x results">
                <div class="cell shrink section">
                    <studios-index-filters v-model="filters">
                    </studios-index-filters>
                </div>

                <div class="cell auto listing">
                    <studios-index-listing
                        ref="listing"
                        :studios="studios"
                        @studio-hover="onListingStudioHover">
                    </studios-index-listing>
                </div>
            </div>
        </div>

        <div class="cell large-7 show-for-large map">
            <studios-index-map
                ref="map"
                :studios="studios"
                @studio-hover="onMapStudioHover"
                @studio-clicked="onMapStudioClick">
            </studios-index-map>
        </div>
    </div>
</template>

<script lang="ts">
import * as _ from "lodash";
import Vue from "vue";

import LocationInput from '../widgets/LocationInput.vue'
import StudiosIndexFilters from './IndexFilters.vue';
import StudiosIndexListing from './IndexListing.vue';
import StudiosIndexMap from './IndexMap.vue';

export default Vue.extend({
    props: {
    },
    data() {
        var filters = {};

        // Extracts filter values based on the URL hash parameters.
        //
        // i.e. /studios#location.place_name=London

        // Extracts the hash query parameters as a JS Object.
        let params = {};
        window.location.hash.
            substring(1).
            split('&').
            forEach(str => {
                let keyValue = str.split('=');

                if (keyValue.length == 2) {
                    params[keyValue[0]] = decodeURIComponent(keyValue[1]);
                }
            });

        // Updates the filter object
        if (
            params['location.place_name']
            && (params['location.bbox'] || params['location.center'])
        ) {
            filters['location'] = { 'place_name': params['location.place_name'] };

            if (params['location.bbox']) {
                filters['location']['bbox'] = params['location.bbox'].split(',');
            }

            if (params['location.center']) {
                filters['location']['center'] = params['location.center'].split(',');
            }
        }

        if (params['date']) {
            filters['date'] = params['date'];
        }

        return {
            studios: [
                {
                    price: 1850,
                    location: [5.231320530534101, 51.36752192207486],
                    name: 'Awesome place',
                    instant_booking: true,
                    pictures: [
                        'https://static.quickstudio.com/media/W1siZiIsInN0dWRpb3MvMjAxNy8xMS8zMC8yNzg4ZDg2ZjI0LmpwZyJdLFsicCIsInRodW1iIiwiNjAwMDAwQCJdXQ',
                    ]
                },
                {
                    price: 1400,
                    location: [4.81032652885753, 52.37886749365077],
                    name: 'My Studio',
                    instant_booking: false,
                    pictures: [
                        'https://images.pexels.com/photos/96380/pexels-photo-96380.jpeg?auto=compress&cs=tinysrgb&dpr=2&w=500',
                    ]
                },
                {
                    price: 2400,
                    location: [4.948984376124656, 52.36697405119352],
                    name: 'My other studio',
                    instant_booking: true,
                    pictures: [
                        'https://images.pexels.com/photos/1327426/pexels-photo-1327426.jpeg?auto=compress&cs=tinysrgb&dpr=2&h=750&w=1260',
                    ]
                },
                {
                    price: 1239,
                    location: [4.8922607113389915, 52.37746843135494],
                    name: 'Super studio',
                    instant_booking: false,
                    pictures: [
                        'https://images.pexels.com/photos/995301/pexels-photo-995301.jpeg?auto=compress&cs=tinysrgb&dpr=2&w=500',
                        'https://images.pexels.com/photos/265672/pexels-photo-265672.png?auto=compress&cs=tinysrgb&dpr=2&w=500',
                    ]
                },
                {
                    price: 2300,
                    location: [4.861320530534101, 52.37152192207486],
                    name: 'Awesome place',
                    instant_booking: true,
                    pictures: [
                        'https://images.pexels.com/photos/744318/pexels-photo-744318.jpeg?auto=compress&cs=tinysrgb&dpr=2&w=500',
                    ]
                },
                {
                    price: 2300,
                    location: [4.831320530534101, 52.36752192207486],
                    name: 'Awesome place',
                    instant_booking: true,
                    pictures: [
                        'https://images.pexels.com/photos/1327430/pexels-photo-1327430.jpeg?auto=compress&cs=tinysrgb&dpr=2&h=750&w=1260',
                    ]
                },
                {
                    price: 2175,
                    location: [5.576180, 50.596520],
                    name: 'Casa de la Mama',
                    instant_booking: true,
                    pictures: [
                        'https://www.jetstudio.com/wp-content/uploads/layerslider/Jet-Studio-Home/jet-Studio-index-1.jpg',
                    ]
                },
            ],
            filters: filters
        }
    },
    mounted() {
        // If we parsed some location value from the URL parameter, immediatly moves the map.
        if (this.filters['location']) {
            this.$refs.map.setLocation(this.filters['location']);
        }
    },
    methods: {
        onListingStudioHover(studioIdx) {
            this.$refs.map.setStudioHighlight(studioIdx);
        },

        onMapStudioHover(studioIdx) {
            this.$refs.listing.setStudioHighlight(studioIdx);
        },

        onMapStudioClick(studioIdx) {
            this.$refs.listing.studioScroll(studioIdx);
        },
    },
    watch: {
        filters(val) {
            if (val.location) {
                this.$refs.map.setLocation(val['location']);
            }

            // Updates the URL parameters

            let hashValues = [];

            if (val['location']) {
                if (val['location']['place_name']) {
                    let encodedName = encodeURIComponent(val['location']['place_name']);
                    hashValues.push('location.place_name=' + encodedName);
                }

                if (val['location']['bbox']) {
                    hashValues.push('location.bbox=' + val['location']['bbox'].join(','));
                }

                if (val['location']['center']) {
                    hashValues.push('location.center=' + val['location']['center'].join(','));
                }
            }

            if (val['date']) {
                hashValues.push('date=' + val['date']);
            }

            if (hashValues.length > 0) {
                window.location.hash = '#' + hashValues.join('&');
            }
        },
    },
    components: { StudiosIndexFilters, StudiosIndexListing, StudiosIndexMap }
});
</script>

<style>
.index .results, .index .map {
    height: calc(100vh - var(--top-bar-height));
}

.index .listing {
    padding-top: 1rem;
    padding-bottom: 1rem;

    overflow-y: scroll;
}
</style>
