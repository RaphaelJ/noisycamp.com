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
    <img
        :alt="alt"
        :srcset="pictureUrl(1) + ' , '  +
            pictureUrl(1.5) + ' 1.5x, ' +
            pictureUrl(2) + ' 2x, ' +
            pictureUrl(2.5) + ' 2.5x'"
        :src="pictureUrl(1)"
        :class="classes">
</template>

<script lang="ts">
import Vue, { PropOptions } from "vue";

import { fromCDN } from '../../misc/CDN';

declare var NC_ROUTES: any;

export default Vue.extend({
    props: {
        pictureId: { type: String, required: true },
        alt: { type: String, required: true },
        classes: { default() { return []; } },

        width: { type: Number, required: true },
        height: { type: Number, required: true },
    },
    methods: {
        pictureUrl(screenRatio) {
            let width = Math.round(this.width * screenRatio);
            let height = Math.round(this.height * screenRatio);

            return fromCDN(NC_ROUTES.controllers.PictureController.cover(
                this.pictureId, width + 'x' + height
            ));
        }
    }
});
</script>

<style>
</style>
