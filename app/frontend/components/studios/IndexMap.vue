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
    <div ref="map" class="map"></div>
</template>

<script lang="ts">
import * as mapboxgl from 'mapbox-gl';
import Vue, { PropOptions } from "vue";

export default Vue.extend({
    props: {
        mapboxToken: { type: String, required: true },
        studios: <PropOptions<Object[]>>{ type: Array, required: true },
    },
    data() {
        return {
        }
    },
    mounted() {
        mapboxgl.accessToken = this.mapboxToken;

        this.map = new mapboxgl.Map({
            container: this.$refs.map,
            style: 'mapbox://styles/mapbox/streets-v10',
            center: [4.9036, 52.3680], // starting position
            zoom: 10, // starting zoom
        });

        this.map.addControl(new mapboxgl.NavigationControl());

        let map = this.map;
        let studiosElems = [];
        this.studios.forEach(function (studio) {
            var priceStr =
                new Intl.NumberFormat(
                    navigator.language, {
                        style: 'currency', currency: 'EUR'
                    })
                .format(studio.price / 100)

            // Creates a DOM element for the marker
            var elem = $('<div></div>')
                .addClass('studio-marker')
                .text(priceStr)
                .click(function() { window.alert(studio.name); });

            studiosElems.push(elem);

            // add marker to map
            new mapboxgl.Marker(elem[0])
                .setLngLat(studio.location)
                .addTo(map);
        });

        this.studiosElems = studiosElems;
        this.activeStudio = null;
    },
    methods: {
        // Centers the map on provided location.
        setLocation(place) {
            if (place.bbox) {
                let bbox = place.bbox;
                this.map.fitBounds([[bbox[0], bbox[1]],[bbox[2], bbox[3]]]);
            } else {
                // The selected location does not have a bbox, use a default
                // zoom.
                this.map.flyTo({
                    center: place.center,
                    zoom: 9,
                });
            }
        },

        // Highlight the given studio
        setActiveStudio(studioIdx) {
            if (this.activeStudio) {
                this.activeStudio.removeClass('active');

            }

            if (studioIdx) {
                this.activeStudio = this.studiosElems[studioIdx];
                this.activeStudio.addClass('active');
            } else {
                this.activeStudio = null;
            }
        }
    },
});
</script>

<style>
.map {
    height: 100%;
}

/* Creates a pseudo element with a box-shadow that recovers the entire map. */
.map::before {
    content: "";
    position: absolute;
    top: 0;
    bottom: 0;
    width: 100%;
    height: 100%;

    z-index: 10;
    pointer-events: none;

    box-shadow: inset 7px 0 9px -7px rgba(0, 0, 0, 0.2);
}

/* Studio map marker */

.map .studio-marker {
    padding: 0.4rem 0.65rem;
    cursor: pointer;

    opacity: 0.85;

    background-color: #2a1f0d;
    border-radius: 3px;

    font-size: 1.3em;
    font-weight: 700;
    color: white;
}

.map .studio-marker::after {
    position: absolute;
    content: "";
    bottom: 100%;
    left: 50%;
    margin-left: -5px;

    border-width: 5px;
    border-style: solid;
    border-color: transparent transparent #2a1f0d transparent;
}

.map .studio-marker:hover, .map .studio-marker.active {
    opacity: 1;
    z-index: 100;
}

.map .studio-marker.active {
    box-shadow: 0 0 0 2px #b37216;
}
</style>
