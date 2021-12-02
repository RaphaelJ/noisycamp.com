<!--
  Noisycamp is a platform for booking music studios.
  Copyright (C) 2019 2020  Raphael Javaux <raphaeljavaux@gmail.com>

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
        <div class="picture-container">
            <div v-if="hasPicture">
                <div
                    v-for="(picId, i) in pictureIds"
                    :key="picId">
                    <reactive-picture
                        v-show="i == selected"
                        :picture-id="picId"
                        :alt="alt"
                        :width="width"
                        :height="height"
                        :classes="classes">
                    </reactive-picture>
                </div>
            </div>

            <!-- Shows a placeholder icon when there is no picture in the carousel. -->
            <div v-else>
                <reactive-picture
                    :alt="alt"
                    :width="width"
                    :height="height"
                    :classes="classes">
                </reactive-picture>
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

        <div
            v-if="thumbnails && hasPicture"
            class="thumbnails"
            ref="thumbnails">
            <div
                v-for="(picId, i) in pictureIds"
                :key="picId"
                :ref="'thumbnail-' + i"
                :class="{
                    'thumbnail-container': true,
                    'active': i == selected,
                }"
                @click="select(i)">
                <reactive-picture
                    alt="Thumbnail"
                    :picture-id="picId"
                    :width="96"
                    :height="54"
                    :placeholder="false">
                </reactive-picture>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import Vue, { PropOptions } from "vue";

import Arrow from './Arrow.vue';
import ReactivePicture from './ReactivePicture.vue';

export default Vue.extend({
    props: {
        pictureIds: <PropOptions<Object[]>>{ type: Array, required: true },
        alt: { type: String, required: true },
        classes: { default: () => { return []; } },

        width: { type: Number, required: true },
        height: { type: Number, required: true },

        thumbnails: { type: Boolean, default: false },
    },
    data() {
        return {
            selected: 0,
        }
    },
    computed: {
        hasPicture() {
            return this.pictureIds.length > 0;
        },

        hasNext() {
            return (this.selected + 1) < this.pictureIds.length
        },

        hasPrevious() {
            return this.selected > 0;
        },
    },
    methods: {
        next() {
            this.select(this.selected + 1);
        },

        previous() {
            this.select(this.selected - 1);
        },

        select(i: number) {
            if (i < 0 || i >= this.pictureIds.length) {
                return;
            }

            this.selected = i;
            this.makeThumbnailVisible();
        },

        // Scroll the thumbnail box to make the currently selected thumbnail fully visible.
        makeThumbnailVisible() {
            if (!this.thumbnails) {
                return;
            }

            let container = $(this.$refs["thumbnails"]);
            let thumbnail = $(this.$refs["thumbnail-" + this.selected]);

            let containerLeft = container.offset().left;
            let containerRight = containerLeft + container.width();

            let thumbnailLeft = thumbnail.offset().left;
            let thumbnailRight = thumbnailLeft + thumbnail.outerWidth();

            var scrollLeft = container.scrollLeft();
            if (containerRight < thumbnailRight) {
                // Scroll to the right
                scrollLeft += thumbnailRight - containerRight + 1;
            } else if (thumbnailLeft < containerLeft) {
                // Scroll to the left
                scrollLeft -= containerLeft - thumbnailLeft;
            }

            if (scrollLeft != container.scrollLeft()) {
                container.animate({ scrollLeft: scrollLeft }, 150);
            }
        },
    },
    components: { Arrow, ReactivePicture },
});
</script>

<style>
.carousel .picture-container {
    position: relative;

    /* Keeps a 16/9 aspect ratio. */
    width: 100%;
    height: 0;
    padding-bottom: calc(9 / 16 * 100%);

    /* Prevents div selection when double clicking. */
    user-select: none;
    -moz-user-select: none;
    -webkit-user-select: none;
    -ms-user-select: none;
}

.carousel .picture-container .reactive-picture {
    position: absolute;

    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
}

.carousel .left-arrow,
.carousel .right-arrow {
    --arrow-size: 40px;

    cursor: pointer;
    opacity: 0.8;

    position: absolute;
    height: var(--arrow-size);
    top: calc(50% - var(--arrow-size) / 2);

    z-index: 1;

    box-shadow: 0 0 4px #00000085;
    background-color: #0000004d;
    border-radius: 50%;
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

.carousel .thumbnails {
    width: 100%;
    margin: 1rem 0;

    overflow-x: auto;
    white-space: nowrap;
}

.carousel .thumbnails .thumbnail-container {
    display: inline-block;
    box-sizing: border-box;
    opacity: 0.35;
    width: 96px;

    border: 2px solid transparent;

    cursor: pointer;
}

.carousel .thumbnails .thumbnail-container:not(last-child) {
    margin-right: 0.5em;
}

.carousel .thumbnails .thumbnail-container.active {
    opacity: 1;
    border-color: var(--accent-color);
}
</style>
