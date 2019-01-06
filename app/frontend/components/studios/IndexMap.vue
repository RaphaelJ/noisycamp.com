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
    <div class="map"></div>
</template>

<script lang="ts">
import Vue from "vue";

import * as mapboxgl from 'mapbox-gl';

export default Vue.extend({
    props: {
        'mapboxToken': String, 'studios': Array
    },
    data() { },
    mounted() {
        mapboxgl.accessToken = this.mapboxToken;

        var map = new mapboxgl.Map({
            container: this.$el,
            style: 'mapbox://styles/mapbox/streets-v10',
            center: [4.9036, 52.3680], // starting position
            zoom: 10, // starting zoom
        });

        map.addControl(new mapboxgl.NavigationControl());

        this.studios.forEach(function (studio) {
            var priceStr =
                new Intl.NumberFormat(
                    navigator.language, { style: 'currency', currency: 'EUR' })
                .format(studio.price / 100)

            // Creates a DOM element for the marker
            var elem = $('<div></div>')
                .addClass('studio-marker')
                .text(priceStr)
                .click(function() { window.alert(studio.name); });

            // add marker to map
            new mapboxgl.Marker(elem[0])
                .setLngLat(studio.location)
                .addTo(map);
        });
    }
});
</script>

<style>
.map {
    height: calc(100vh - 56px);
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

.map .studio-marker {
    padding: 0.2rem 0.4rem;
    cursor: pointer;

    background-color: #2a1f0d;
    opacity: 0.8;

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

.map .studio-marker:hover {
    opacity: 1;
}
</style>
