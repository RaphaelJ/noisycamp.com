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
    <div class="carousel">
        <i
            class="no-picture fi-photo"
            v-if="pictureIds.length == 0"
            title="No picture">
        </i>

        <div
            class="picture-container"
            v-for="(picId, i) in pictureIds"
            key="picId">

            <transition name="fade">
                <reactive-picture
                    v-show="i == selected"
                    :picture-id="picId"
                    :alt="alt"
                    :width="width"
                    :height="height">
                </reactive-picture>
            </transition>
        </div>

        <span
            class="left-arrow"
            v-show="hasPrevious"
            @click="previous()">

            <arrow direction="left"></arrow>
        </span>

        <span
            class="right-arrow"
            v-show="hasNext"
            @click="next()">

            <arrow direction="right"></arrow>
        </span>
    </div>
</template>

<script lang="ts">
import Vue, { PropOptions } from "vue";

import Arrow from './Arrow.vue';
import ReactivePicture from './ReactivePicture.vue';

declare var NC_ROUTES: any;

export default Vue.extend({
    props: {
        pictureIds: <PropOptions<Object[]>>{ type: Array, required: true },
        alt: { type: String, required: true },

        width: { type: Number, required: true },
        height: { type: Number, required: true },
    },
    data() {
        return {
            selected: 0,
        }
    },
    computed: {
        hasNext() {
            return (this.selected + 1) < this.pictureIds.length
        },
        hasPrevious() {
            return this.selected > 0;
        },
    },
    methods: {
        next() {
            ++this.selected;
        },
        previous() {
            --this.selected;
        }
    },
    components: { Arrow, ReactivePicture },
});
</script>

<style>
.carousel {
    position: relative;

    /* Keeps a 16/9 aspect ratio. */
    width: 100%;
    height: 0;
    padding-bottom: calc(9 / 16 * 100%);

    background-color: #e3e3e3;

    /* Prevents div selection when double clicking. */
    user-select: none;
    -moz-user-select: none;
    -webkit-user-select: none;
    -ms-user-select: none;
}

.carousel .no-picture {
    font-size: 150px;
    opacity: 0.15;

    position: absolute;
    top: 50%;
    left: 50%;
    width: 250px;
    margin-left: -125px;
    margin-top: -100px;

    text-align: center;
}

.carousel .picture-container {
    position: absolute;

    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
}

.carousel .fade-enter-active,
.carousel .fade-leave-active {
    transition: opacity .15s;
}

.carousel .fade-leave-to,
.carousel .fade-enter {
    opacity: 0;
}

.carousel .left-arrow,
.carousel .right-arrow {
    cursor: pointer;
    opacity: 0.7;

    position: absolute;
    top: 50%;
    height: 40px;
    margin-top: -20px;
}

.carousel .left-arrow:hover,
.carousel .right-arrow:hover {
    opacity: 1;
}

.carousel .left-arrow {
    left: 0;
    margin-left: 0.5rem;
}

.carousel .right-arrow {
    right: 0;
    margin-right: 0.5rem;
}

.carousel .left-arrow svg,
.carousel .right-arrow svg {
    height: 2.5rem;
    width: 2.5rem;
    fill: rgba(255, 255, 255, 1);
}
</style>
