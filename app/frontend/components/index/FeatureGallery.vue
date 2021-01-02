<!--
  Noisycamp is a platform for booking music studios.
  Copyright (C) 2020  Raphael Javaux <raphaeljavaux@gmail.com>

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
    <div class="grid-x grid-padding-x grid-margin-x grid-padding-y feature-gallery">
        <div class="cell small-12 feature-image-container">
            <img
                v-for="feature in features"
                :key="feature"
                alt="Feature screenshot"
                :class="{ active: currentFeature == feature }"
                :src="featureImage(feature)">
        </div>

        <div
            class="cell medium-6 feature-gallery-text"
            :class="{ 'active': currentFeature == 'online_presence' }"
            @click="featureClicked('online_presence')">
            <h3>Reach new musicians</h3>

            <h5>A modern online presence for your studio</h5>

            <p>
                NoisyCamp is a build on top of a global community of 1,000s of musicians.<br>
                By listing your studio on NoisyCamp, you will reach new customers, and increase your
                revenue.<br>
                NoisyCamp is available on desktop and mobile devices.
            </p>
        </div>


        <div
            class="cell medium-6 feature-gallery-text"
            :class="{ 'active': currentFeature == 'calendar' }"
            @click="featureClicked('calendar')">
            <h3>Online bookings</h3>
            <h5>Spend less time managing bookings</h5>
            <p>
                Enable musicians to book your practice or recording spaces online, reducing the
                time you will spend answering emails and phone calls.
            </p>
        </div>
    </div>
</template>

<script lang="ts">
import Vue from "vue";

import { fromCDN } from '../../misc/CDN';

declare var NC_ROUTES: any;

export default Vue.extend({
    props: { },
    data() {
        let features = ['online_presence', 'calendar'];

        return {
            features: features,
            currentFeature: features[0],

            // Handler of the clock timer that cycles through the features. Will be stopped the
            // first time the user clicks on a feature's text.
            intervalTimer: null,
        };
    },
    mounted() {
        let FEATURE_CYCLE_DELAY = 5000;

        this.intervalTimer = setInterval(function () {
            if (this.currentFeature == 'online_presence') {
                this.currentFeature = 'calendar';
            } else if (this.currentFeature == 'calendar') {
                this.currentFeature = 'online_presence';
            }
        }.bind(this), FEATURE_CYCLE_DELAY);
    },
    methods: {
        featureImage(featureName: string) {
            return fromCDN(
                NC_ROUTES.controllers.Assets.versioned(`images/index/${featureName}.png`));
        },

        featureClicked(featureName: string) {
            if (this.intervalTimer) {
                clearInterval(this.intervalTimer);
                this.intervalTimer = null;
            }

            this.currentFeature = featureName;
        },
    }
});
</script>

<style scoped>
.feature-gallery .feature-image-container {
    text-align: center;
}

.feature-gallery .feature-image-container img {
    display: none;

    max-height: 30rem;
}

.feature-gallery .feature-image-container img.active {
    display: inline-block;
}

.feature-gallery .feature-gallery-text {
    position: relative;

    margin-top: 1rem;
}

.feature-gallery .feature-gallery-text,
.feature-gallery .feature-gallery-text::after {
    border: 1px solid transparent;
    transition: background 0.2s ease-in-out, border 0.2s ease-in-out;

    cursor: pointer;
}

.feature-gallery .feature-gallery-text::after {
    display: block;
    content: "";

    position: absolute;
    top: calc(-1.5rem / 2);
    left: calc(50% - 1.5rem / 2);

    height: 1.5rem;
    width: 1.5rem;

    transform: rotate(45deg);
}

.feature-gallery .feature-gallery-text.active,
.feature-gallery .feature-gallery-text.active::after {
    background-color: #fefefe;
    border: 1px solid rgba(0, 0, 0, 0.1);
}

.feature-gallery .feature-gallery-text.active::after {
    border-right: 1px solid transparent;
    border-bottom: 1px solid transparent;
}

/* Hide the arrow on small screens */
@media screen and (max-width: 39.9375em) {
    .feature-gallery .feature-gallery-text.active::after {
        display: none;
    }
}

</style>
