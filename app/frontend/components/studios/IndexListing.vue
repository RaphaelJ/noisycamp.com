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
    <div ref="listing">
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
            class="grid-x studios">
            <li v-for="(studio, index) in studios"
                :ref="'studio-' + index"
                class="cell small-12">

                <studios-index-listing-item
                    :studio="studio"
                    :booking-date="bookingDate"
                    :highlighted="highlightedStudio == index"

                    @mouseover="$emit('studio-hover', index)"
                    @mouseout="$emit('studio-hover', null)">
                </studios-index-listing-item>
            </li>
        </ul>
    </div>
</template>

<script lang="ts">
import Vue, { PropOptions } from "vue";

declare var NC_CONFIG: any;

import StudiosIndexListingItem from './IndexListingItem.vue';

export default Vue.extend({
    props: {
        studios: <PropOptions<Object[]>>{ type: Array, required: true },

        bookingDate: { type: String, required: false, default: null },
        searchIsProcessing: { type: Boolean, required: false, default: false },
    },
    data() {
        return {
            location: null,
            highlightedStudio: null,
        }
    },
    methods: {

        // Makes the given studio more visible. Reset studio highlighting if
        // studioIdx is `null`.
        setStudioHighlight(studioIdx) {
            this.highlightedStudio = studioIdx;
        },

        // Scrolls to the given studio index.
        studioScroll(studioIdx) {
            let container = $(this.$refs['listing']).parent();
            let elem = this.$refs['studio-' + studioIdx][0];

            container.animate({
                scrollTop: elem.offsetTop - container[0].offsetTop
            }, 500);
        },
    },
    components: { StudiosIndexListingItem },
});
</script>

<style scoped>
.message {
    margin-top: 1.5rem;

    text-align: center;
}

ul.studios {
    list-style-type: none;
    margin-left: 0;
}

</style>
