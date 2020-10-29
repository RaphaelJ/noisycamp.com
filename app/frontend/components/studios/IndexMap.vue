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
    <div ref="map" class="studios-map"></div>
</template>

<script lang="ts">
import * as mapboxgl from 'mapbox-gl';
import * as moment from 'moment';
import Vue, { PropOptions } from "vue";

import MoneyAmount from '../widgets/MoneyAmount.vue'

import { startingPrice } from '../../misc/MoneyUtils';
import { isWeekend } from "../../misc/DateUtils";

declare var NC_CONFIG: any;

export default Vue.extend({
    props: {
        studios: <PropOptions<Object[]>>{ type: Array, required: true },

        bookingDate: { type: String, required: false, default: null },
    },
    data() {
        return {
            studiosElems: [],
            studiosMarkers: [],

            highlightedStudio: null,
        }
    },
    mounted() {
        mapboxgl.accessToken = NC_CONFIG.mapboxToken;

        this.map = new mapboxgl.Map({
            container: this.$refs.map,
            style: 'mapbox://styles/mapbox/streets-v10',
            // starting position covers whole globe
            bounds: [[-180, 90], [180, -90]],
        });

        this.map.addControl(new mapboxgl.NavigationControl());

        this.updateStudioMarkers();

        this.map.on('moveend', this.onMapMove)
    },
    methods: {
        // Centers the map on provided location.
        setLocation(place) {
            // Adds a `isApiTriggered` properties to events triggered through
            // the API.
            // That will prevents the on `moveend` event listener to trigger a
            // new studio search on these events.
            let eventData = { isApiTriggered: true, };

            if (place.bbox) {
                this.map.fitBounds(place.bbox, {}, eventData);
            } else {
                // The selected location does not have a bbox, use a default
                // zoom.
                this.map.flyTo({
                    center: place.center,
                    zoom: 9,
                }, eventData);
            }
        },

        // Updates the markers based on the value of `studios`.
        updateStudioMarkers() {
            // Removes old markers from the maps

            for (let marker of this.studiosMarkers) {
                marker.remove();
            }

            for (let elem of this.studiosElems) {
                elem.remove();
            }

            this.studiosElems = [];
            this.studiosMarkers = [];

            // Adds the new markers

            this.studios.forEach((studio, index) => {
                // Creates a DOM element for the marker

                let price = new (Vue.extend(MoneyAmount))({
                    propsData: {
                        value: startingPrice(
                            studio['pricing-policy'], isWeekend(this.bookingDate))[0],
                        showZeroCents: true,
                    }
                });
                price.$mount();

                let elem = $('<div></div>')
                    .addClass('studio-marker')
                    .append(price.$el)
                    .mouseover(() => this.$emit('studio-hover', index))
                    .mouseout(() => this.$emit('studio-hover', null))
                    .click(() => this.$emit('studio-click', index));

                this.studiosElems.push(elem);

                // Add marker to map
                let coordinates = studio.location.coordinates;
                let marker = new mapboxgl.Marker(elem[0]);
                marker.setLngLat([coordinates.long, coordinates.lat])
                    .addTo(this.map);

                // this.studiosMarkers.push(marker);
            });
        },

        // Makes the given studio more visible. Reset studio highlighting if
        // studioIdx is `null`.
        setStudioHighlight(studioIdx) {
            if (this.highlightedStudio) {
                this.highlightedStudio.removeClass('highlighted');
            }

            if (studioIdx != null) {
                this.highlightedStudio = this.studiosElems[studioIdx];
                this.highlightedStudio.addClass('highlighted');
            } else {
                this.highlightedStudio = null;
            }
        },

        onMapMove(event) {
            if (event.isApiTriggered) {
                // Ignore map view events triggered by a call to `flyTo()`.
                return;
            }

            this.$emit('map-view-change', this.map.getBounds());
        }
    },
    watch: {
        studios() {
            this.updateStudioMarkers();
        },
    }
});
</script>

<style>
.studios-map {
    height: 100%;
}

/* Creates a pseudo element with a box-shadow that recovers the entire map. */
.studios-map::before {
    content: "";
    position: absolute;
    top: 0;
    bottom: 0;
    width: 100%;
    height: 100%;

    z-index: 10;
    pointer-events: none;

    box-shadow: inset 7px 0 9px -7px rgba(0, 0, 0, 0.075);
}

/* Studio map marker */

.studios-map .studio-marker {
    padding: 0 0.65rem;
    cursor: pointer;

    opacity: 0.85;

    background-color: #2a1f0d;
    border-radius: 3px;

    /* Mapbox centers the marker on the position. Adds a margin that will
     * offset the arrow of the marker to the pointed location. */
    line-height: 35px;
    margin-top: calc(35px / 2 + 5px);

    font-size: 1.3em;
    font-weight: 700;
    color: white;
}

.studios-map .studio-marker::after {
    /** Draws an arrow on top of the marker. */

    position: absolute;
    content: "";
    bottom: 100%;
    left: 50%;
    margin-left: -5px;

    border-width: 5px;
    border-style: solid;
    border-color: transparent transparent #2a1f0d transparent;
}

.studios-map .studio-marker:hover,
.studios-map .studio-marker.highlighted {
    opacity: 1;
    z-index: 100;
}

.studios-map .studio-marker.highlighted {
    border: 2px solid #b37216;
}

.studios-map .studio-marker.highlighted::after {
    margin-left: -7px;
    border-width: 7px;
    border-color: transparent transparent #b37216 transparent;
}
</style>
