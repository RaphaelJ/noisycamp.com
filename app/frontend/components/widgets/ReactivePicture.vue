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

  Renders the requested picture using the `srcset` parameter to support varying
  screen pixel densities.
-->

<template>
    <div 
        class="reactive-picture-container"
        :style="{
            'padding-top': (imageRatio * 100) + '%'
        }">

        <i 
            class="reactive-picture-placeholder fi-photo"
            :class="classes">
        </i>

        <img
            class="reactive-picture"
            v-if="pictureId"
            :alt="alt"
            :srcset="pictureUrl(1) + ' , '  +
                pictureUrl(1.5) + ' 1.5x, ' +
                pictureUrl(2) + ' 2x, ' +
                pictureUrl(2.5) + ' 2.5x'"
            :src="pictureUrl(1)"
            :class="classes">
    </div>
</template>

<script lang="ts">
import Vue, { PropOptions } from "vue";

import { fromCDN } from '../../misc/CDN';

declare var NC_ROUTES: any;

export default Vue.extend({
    props: {
        // If not defined, will show a placeholder icon.
        pictureId: { type: String, required: false },

        alt: { type: String, required: true },
        classes: { default() { return []; } },

        width: { type: Number, required: true },
        height: { type: Number, required: true },
    },
    computed: {
        imageRatio() {
            return this.height / this.width;
        },
    },
    methods: {
        pictureUrl(screenRatio) {
            if (this.pictureId) {
                let width = Math.round(this.width * screenRatio);
                let height = Math.round(this.height * screenRatio);

                return fromCDN(NC_ROUTES.controllers.PictureController.cover(
                    this.pictureId, width + 'x' + height
                ));
            } else {
                return null;
            }
        }
    }
});
</script>

<style>
.reactive-picture-container {
    position: relative;
    width: 100%;
    height: 0;

    background-color: #f5f3f2c2;
}

.reactive-picture-container .reactive-picture {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
}

.reactive-picture-container .reactive-picture-placeholder {
    font-size: 100px;
    color: black;
    opacity: 0.15;

    position: absolute;
    top: 50%;
    left: 50%;
    width: 150px;
    margin-left: -75px;
    margin-top: -75px;

    text-align: center;
}
</style>
