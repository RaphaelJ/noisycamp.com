<!--
  Noisycamp is a platform for booking music studios.
  Copyright (C) 2019, 2020  Raphael Javaux <raphaeljavaux@gmail.com>

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
        <div class="cell medium-12 large-6">
            <div class="grid-y grid-padding-x results">
                <div class="cell shrink section">
                    <studios-index-filters
                        :value="initialFilters"
                        @input-location="onFilterInputLocation"
                        @input-available-on="onFilterInputAvailaibleOn">
                    </studios-index-filters>
                </div>

                <div class="cell listing">
                    <studios-index-listing
                        ref="listing"
                        :studios="studios"
                        :search-is-processing="searchIsProcessing"
                        @studio-hover="onListingStudioHover">
                    </studios-index-listing>
                </div>
            </div>
        </div>

        <div
            class="cell large-6 show-for-large map"
            ref="map-container">

            <studios-index-map
                ref="map"
                :studios="studios"
                @studio-hover="onMapStudioHover"
                @studio-click="onMapStudioClick"
                @map-view-change="onMapViewChange">
            </studios-index-map>
        </div>
    </div>
</template>

<script lang="ts">
import axios from 'axios';
import * as mapboxgl from 'mapbox-gl';
import Vue from "vue";

declare var NC_ROUTES: any;

import LocationInput from '../widgets/LocationInput.vue';
import StudiosIndexFilters from './IndexFilters.vue';
import StudiosIndexListing from './IndexListing.vue';
import StudiosIndexMap from './IndexMap.vue';
import { serializeBBox, unserializeBBox, serializeCenter, unserializeCenter,
    serializeFeature } from '../../misc/GeoUtils';

export default Vue.extend({
    props: {
    },
    data() {
        // Extracts the `location` and `availableOn` values based on the URL
        // hash parameters.
        //
        // i.e. /studios#location.place_name=London

        let location = {};
        var availableOn = null;

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
            location['place_name'] = params['location.place_name'];

            if (params['location.bbox']) {
                location['bbox'] = unserializeBBox(params['location.bbox']);
            }

            if (params['location.center']) {
                location['center'] = unserializeCenter(params['location.center']);
            }
        }

        if (params['available-on']) {
            availableOn = params['available-on'];
        }

        return {
            studios: [],
            searchIsProcessing: false,

            location: location,
            availableOn: availableOn,

            initialFilters: {
                'location': location,
                'available-on': availableOn,
            },
        }
    },
    mounted() {
        // If we parsed some location value from the URL parameter, immediatly moves the map.
        if (this.location['center'] || this.location['bbox']) {
            this.$refs.map.setLocation(this.location);
        }

        this.searchStudios(true);
    },
    methods: {
        // Executes a HTTP request to the search endpoint with the current filters' values.
        searchStudios(removeResultFirst) {
            this.searchIsProcessing = true;

            if (removeResultFirst) {
                this.studios = [];
            }

            let url = NC_ROUTES.controllers.StudiosController.search().url;

            let params = { };

            if (this.location) {
                let bbox = this.location['bbox'];
                if (bbox) {
                    params['location.bbox'] = serializeBBox(bbox);
                }

                let center = this.location['center'];
                if (center) {
                    params['location.center'] = serializeCenter(center);
                }
            }

            if (this.availableOn) {
                params['available-on'] = this.availableOn;
            }

            axios.get(url, { params: params })
                .then(response => { this.studios = response.data['results']; })
                .catch(error => { })
                .then(() => { this.searchIsProcessing = false; });
        },

        // Sets the URL hash parameters to the current filter values.
        setUrlHashParams() {
            let hashValues = [];

            if (this.location) {
                hashValues.push(serializeFeature(this.location));
            }

            if (this.availableOn) {
                hashValues.push('available-on=' + this.availableOn);
            }

            if (hashValues.length > 0) {
                window.location.hash = '#' + hashValues.join('&');
            }
        },

        onFilterInputLocation(location) {
            this.location = location;

            if (this.location) {
                this.$refs.map
                .setLocation(this.location);
            }

            this.searchStudios(true);
            this.setUrlHashParams();
        },

        onFilterInputAvailaibleOn(availableOn) {
            this.availableOn = availableOn;

            this.searchStudios(true);
            this.setUrlHashParams();
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

        onMapViewChange(bbox) {
            let isMapDisplayed = window
                .getComputedStyle(this.$refs['map-container'])
                .display != "none";

            if (isMapDisplayed) {
                // Overrides the filter's bbox on user map drag/zoom.
                this.location['bbox'] = bbox;

                this.searchStudios(false);
                this.setUrlHashParams();
            }
        },
    },
    components: { StudiosIndexFilters, StudiosIndexListing, StudiosIndexMap }
});
</script>

<style>
@media print, screen and (min-width: 64em) {
    .index .results, .index .map {
        height: calc(100vh - var(--top-bar-height));
    }

    .index .listing {
        /* Makes the listing scollable and expand as much as possible. */
        overflow-y: scroll;
        flex: 1 1 0px;
    }
}
</style>
