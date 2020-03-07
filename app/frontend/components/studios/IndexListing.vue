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
    <div>
        <ul class="grid-x grid-margin-x grid-padding-y studios">
            <li v-for="(studio, index) in studios"
                :ref="'studio-' + index"
                class="cell small-12 studio"
                :class="{
                    'highlighted': highlightedStudio == index,
                }"
                @mouseover="$emit('studio-hover', index)"
                @mouseout="$emit('studio-hover', null)">

                <a
                    :href="studioURL(studio)"
                    target="_blank"
                    class="grid-x grid-padding-x">

                    <div class="cell small-4 pictures">
                        <picture-carousel
                            :alt="studio.name + ' picture'"
                            :picture-ids="studio.picturesIds"
                            :width="400"
                            :height="225"
                            classes="border-radius">
                        </picture-carousel>
                    </div>

                    <div class="cell small-8">
                        <h5>
                            {{ studio.name }}
                        </h5>

                        <p class="details">
                            <span class="location">
                                <i class="fi-marker"></i>&nbsp;
                                {{ studio.location.address.city }},
                                {{ studio.location.address.country.name }}
                            </span>

                            <span class="schedule">
                                <i class="fi-clock"></i>&nbsp;
                                Mon - Tue - <strong>Wed</strong> - Thu - <strong>Fri</strong>
                                - Sat - Sun
                            </span>
                        </p>
                    </div>
                </a>
            </li>

            <!-- <li
                v-for="(studio, index) in studios"
                :ref="'studio-' + index"
                class="cell small-12 medium-6 studio"
                :class="{
                    'large-6': index < 2,
                    'large-4': index >= 2,
                    'highlighted': highlightedStudio == index,
                }"
                @mouseover="$emit('studio-hover', index)"
                @mouseout="$emit('studio-hover', null)">
                <ul class="pictures">
                    <li>
                        <img
                            :src="studio.pictures[0]"
                            :alt="studio.name">
                    </li>
                </ul>

                <div
                    v-if="studio.instant_booking"
                    class="instant-booking"
                    title="Instant booking availaible for that place">
                    <i class="fi-target"></i>
                </div>

                <div class="details">
                    <span class="name">{{ studio.name }}</span>
                    <span class="price">
                        <strong>{{ renderCurrency(studio.price) }}</strong>
                        per hour
                    </span>
                </div>
            </li> -->
        </ul>
    </div>
</template>

<script lang="ts">
import Vue, { PropOptions } from "vue";

import PictureCarousel from '../widgets/PictureCarousel.vue';

declare var NC_ROUTES: any;

export default Vue.extend({
    props: {
        studios: <PropOptions<Object[]>>{ type: Array, required: true },
    },
    data() {
        return {
            location: null,
            highlightedStudio: null,
        }
    },
    methods: {
        studioURL(studio) {
            return NC_ROUTES.controllers.StudiosController.show(studio.id).url;
        },

        renderCurrency(value) {
            return new Intl.NumberFormat(
                navigator.language, { style: 'currency', currency: 'EUR' }
            ).format(value / 100);
        },

        // Makes the given studio more visible. Reset studio highlighting if
        // studioIdx is `null`.
        setStudioHighlight(studioIdx) {
            this.highlightedStudio = studioIdx;
        },

        // Scrolls to the given studio index.
        studioScroll(studioIdx) {
            let elem = this.$refs['studio-' + studioIdx];
            $(this.$el).parent().scrollTop(elem[0].offsetTop);
        }
    },
    components: { PictureCarousel },
});
</script>

<style>
ul.studios {
    list-style-type: none;
}

ul.studios li.studio {
    border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

ul.studios li.studio:hover,
ul.studios li.studio.highlighted {
    background-color: rgba(255, 255, 255, 0.4);
}

/* Enlarges studio on over/activate */
ul.studios li.studio:hover,
ul.studios li.studio.highlighted {
    transform: scale(1.015);
    overflow: hidden; /* makes the block not overflow when enlarged */
}

ul.studios li.studio .details {
    color: #777;
    font-size: 0.87em;
}

ul.studios li.studio .details span.location,
ul.studios li.studio .details span.schedule {
    display: block;
}

ul.studios li.studio .instant-booking {
    position: absolute;
    top: 0;
    right: 0;
    padding: 5px 15px;
    margin: 0;

    color: white;
    text-shadow: 0 0 3px rgba(0, 0, 0, 0.8);
    font-size: 1.5rem;

    cursor: help;
}

/* ul.studios li.studio .details {
    position: absolute;
    top: calc(100% - 45px);
    left: 0;
    height: 45px;
    width: 100%;
    box-sizing: border-box;
    padding-left: 1rem;
    padding-right: 1rem;

    border-top: 2px solid #b37216;
    border-bottom-left-radius: 3px;
    border-bottom-right-radius: 3px;

    background-color: rgba(0, 0, 0, 0.75);
    color: white;
    line-height: 45px;
    font-size: 1.15rem;
}

ul.studios li.studio .details .name {
    float: left;
} */

ul.studios li.studio .details .price {
    float: right;
}
</style>
