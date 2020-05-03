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
        <div
            v-if="searchIsProcessing"
            class="message">
            Searching ...
        </div>

        <div
            v-if="!searchIsProcessing && studios.length == 0"
            class="message">
            No match found.
        </div>

        <ul
            v-if="!searchIsProcessing && studios.length > 0"
            class="grid-x grid-margin-x grid-padding-y studios">
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
                        <reactive-picture
                            :alt="studio.name + ' picture'"
                            :picture-id="studio.picture"
                            :width="400"
                            :height="225"
                            classes="border-radius">
                        </reactive-picture>
                    </div>

                    <div class="cell small-8">
                        <h5>
                            {{ studio.name }}
                        </h5>

                        <p class="details">
                            <span class="location">
                                <i class="fi-marker"></i>
                                {{ studioLocation(studio) }}
                            </span>

                            <span
                                class="schedule"
                                title="Opening schedule">
                                <i class="fi-clock"></i>

                                <span
                                    v-for="(day, index) in studio['opening-schedule']"
                                    :key="index"
                                    :class="{ 'is-open': day['is-open'], 'is-closed': !day['is-open'] }">

                                    {{ weekDays[index] }}
                                    <span v-if="index < 6">-</span>
                                </span>
                            </span>
                        </p>
                    </div>
                </a>
            </li>
        </ul>
    </div>
</template>

<script lang="ts">
import Vue, { PropOptions } from "vue";

import ReactivePicture from '../widgets/ReactivePicture.vue';

declare var NC_CONFIG: any;
declare var NC_ROUTES: any;

export default Vue.extend({
    props: {
        studios: <PropOptions<Object[]>>{ type: Array, required: true },
        searchIsProcessing: { type: Boolean, required: false, default: false }
    },
    data() {
        return {
            location: null,
            highlightedStudio: null,
        }
    },
    computed: {
        weekDays() {
            return ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
        },
    },
    methods: {
        studioURL(studio) {
            return NC_ROUTES.controllers.StudiosController.show(studio.id).url;
        },

        studioLocation(studio) {
            let city = studio.location.address.city;
            let country = NC_CONFIG.countries[studio.location.address['country-code']].name;
            return `${city}, ${country}`
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
        },
    },
    components: { ReactivePicture },
});
</script>

<style>
.message {
    margin-top: 1.5rem;

    text-align: center;
}

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

ul.studios li.studio .details span.location .fi-marker,
ul.studios li.studio .details span.schedule .fi-clock {
    display: inline-block;
    width: 13px;
}

ul.studios li.studio .details span.schedule .is-open {
    font-weight: bold;
}

ul.studios li.studio .details span.schedule .is-closed {
    color: #999;
}
</style>
