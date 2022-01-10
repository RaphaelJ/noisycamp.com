
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
    <div
        class="grid-x grid-padding-x grid-padding-y"
        ref="container">

        <div
            v-if="searchIsProcessing"
            class="cell small-12 text-center">
            Searching ...
        </div>

        <div
            v-if="!searchIsProcessing && studios.length == 0"
            class="cell small-12 text-center">
            No match found.<br>
            Do you own a music studio?
            <a :href="becomeAHostURL">List it on NoisyCamp now</a>.
        </div>

        <div
            v-for="(studio, index) in studios"
            :ref="'studio-' + index"
            :key="studio.id"
            class="cell small-12 large-6">

            <a
                :href="studio.url"
                :ref="'studio-' + index + '-link'"
                target="_blank"
                class="studio panel-section hide-for-small-only"
                :class="{ 'highlighted': highlightedStudio == index, }"
                @mouseover="$emit('mouseover')"
                @mouseout="$emit('mouseout')">

                <studios-index-listing-item
                    v-if="!searchIsProcessing"
                    :studio="studio"
                    :booking-date="bookingDate">
                </studios-index-listing-item>
            </a>

            <a
                :href="studio.url"
                class="studio panel-section show-for-small-only"
                :class="{ 'highlighted': highlightedStudio == index, }"
                @mouseover="$emit('mouseover')"
                @mouseout="$emit('mouseout')">

                <studios-index-listing-item
                    v-if="!searchIsProcessing"
                    :studio="studio"
                    :booking-date="bookingDate">
                </studios-index-listing-item>
            </a>
        </div>
    </div>
</template>

<script lang="ts">
import Vue, { PropOptions } from "vue";

declare var NC_ROUTES: any;

import StudiosIndexListingItem from './StudioIndexListingItem.vue';

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
    computed: {
        becomeAHostURL() {
            return NC_ROUTES.controllers.IndexController.becomeAHost().url;
        }
    },
    methods: {

        // Makes the given studio more visible. Reset studio highlighting if
        // studioIdx is `null`.
        setStudioHighlight(studioIdx: number) {
            this.highlightedStudio = studioIdx;
        },

        // Scrolls to the given studio index.
        studioScroll(studioIdx: number) {
            let container = $(this.$refs['container']).parent();
            let elem = this.$refs['studio-' + studioIdx][0];

            container.animate({
                scrollTop: elem.offsetTop - container[0].offsetTop
            }, 500);
        },

        // Simulates a click on the studio's item
        studioClick(studioIdx: number) {
            let elem = this.$refs['studio-' + studioIdx + '-link'][0];
            elem.click();
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

.studio {
    display: block;
}

.studio.highlighted {
    box-shadow: 0 0 0 2px var(--accent-color);
}
</style>
