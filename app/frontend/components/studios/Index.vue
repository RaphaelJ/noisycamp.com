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
                        :search-is-processing="searchIsProcessing"
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
import axios from 'axios';
import Vue from "vue";

declare var NC_ROUTES: any;

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

        if (params['available-on']) {
            filters['available-on'] = params['available-on'];
        }

        return {
            studios: [],
            searchIsProcessing: false,
            filters: filters
        }
    },
    mounted() {
        // If we parsed some location value from the URL parameter, immediatly moves the map.
        if (this.filters['location']) {
            this.$refs.map.setLocation(this.filters['location']);
        }

        this.searchStudios();
    },
    methods: {
        // Executes a HTTP request to the search endpoint with the current filters' values.
        searchStudios() {
            this.searchIsProcessing = true;

            this.studios = [];

            let url = NC_ROUTES.controllers.StudiosController.search().url;

            let params = { };
            let filters = this.filters;

            if (filters['location']) {
                if (filters['location']['bbox']) {
                    let bbox = filters['location']['bbox'];
                    params['location.bbox'] = `${bbox[3]},${bbox[1]},${bbox[0]},${bbox[2]}`;
                }

                if (filters['location']['center']) {
                    params['location.center'] = filters['location']['center'].join(',');
                }
            }

            if (filters['available-on']) {
                params['available-on'] = filters['available-on'];
            }

            axios.get(url, { params: params })
                .then(response => { this.studios = response.data['results']; })
                .catch(error => { })
                .then(() => { this.searchIsProcessing = false; });
        },

        // Sets the URL hash parameters to the current filter values.
        setUrlHashParams() {
            let filters = this.filters;

            let hashValues = [];

            if (filters['location']) {
                if (filters['location']['place_name']) {
                    let encodedName = encodeURIComponent(filters['location']['place_name']);
                    hashValues.push('location.place_name=' + encodedName);
                }

                if (filters['location']['bbox']) {
                    hashValues.push('location.bbox=' + filters['location']['bbox'].join(','));
                }

                if (filters['location']['center']) {
                    hashValues.push('location.center=' + filters['location']['center'].join(','));
                }
            }

            if (filters['available-on']) {
                hashValues.push('available-on=' + filters['available-on']);
            }

            if (hashValues.length > 0) {
                window.location.hash = '#' + hashValues.join('&');
            }
        },

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
        filters: {
            handler(val) {
                this.searchStudios();
                this.setUrlHashParams();

                if (val.location) {
                    this.$refs.map.setLocation(val['location']);
                }
            },
            deep: true,
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
    overflow-y: scroll;
}
</style>
