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

  Provides a widget that displays a map with a marker on the given
  coordinates.
-->

<template>
    <div class="location-map" ref="map"></div>
</template>

<script lang="ts">
import * as mapboxgl from 'mapbox-gl';
import Vue from "vue";

declare var NC_CONFIG: any;

export default Vue.extend({
    props: {
        long: { type: Number, required: true },
        lat: { type: Number, required: true },

        zoom: { type: Number, required: false, default: 9 },
    },
    mounted() {
        let center = [this.long, this.lat];

        mapboxgl.accessToken = NC_CONFIG.mapboxToken;

        this.map = new mapboxgl.Map({
            container: this.$refs.map,
            style: 'mapbox://styles/mapbox/streets-v10',
            center: center,
            zoom: this.zoom
        });

        this.map.addControl(new mapboxgl.NavigationControl());

        new mapboxgl.Marker({
            color: '#b37216',
            draggable: false,
        })
            .setLngLat(center)
            .addTo(this.map);
    },
});
</script>

<style>
.location-map {
    /* Keeps a 2:1 aspect ratio. */
    width: 100%;
    height: 0;
    padding-bottom: 50%;
}
</style>
