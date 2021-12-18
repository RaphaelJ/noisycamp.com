<!--
  Noisycamp is a platform for booking music studios.
  Copyright (C) 2020  Raphael Javaux <raphael@noisycamp.com>

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

  Provides a form to edit or create a new studio.
-->

<template>
    <div class="grid-x grid-margin-x">
        <p class="cell small-12">
            Getting your music studio listed on NoisyCamp only takes a few minutes. We will need a
            little bit of information about the place you want to add.
        </p>

        <div class="cell medium-4 show-for-medium">
            <div class="grid-y feature-block">
                <div class="cell shrink feature-image">
                    <img
                        alt="Globe icon"
                        :src="assetURL('images/vendor/business-bubble-icons/063-earth-globe.svg')">
                </div>

                <h4 class="cell feature-title">Increased exposure</h4>

                <p class="cell feature-text">
                    Reach thousands of new musicians from all over the world and increase your
                    customer base and revenue.
                </p>
            </div>
        </div>

        <div class="cell medium-4 show-for-medium">
            <div class="grid-y feature-block">
                <div class="cell shrink feature-image">
                    <img
                        alt="Calendar icon"
                        :src="assetURL('images/vendor/business-bubble-icons/090-calendar.svg')">
                </div>

                <h4 class="cell feature-title">Online bookings</h4>

                <p class="cell feature-text">
                    Enable musicians to see real-time availabilities of your studio and make a
                    booking any time of the day.
                </p>
            </div>
        </div>

        <div
            class="cell medium-4 show-for-medium"
            v-if="!isPremium">
            <div class="grid-y feature-block">
                <div class="cell shrink feature-image">
                    <img
                        alt="Piggy bank icon"
                        :src="assetURL('images/vendor/business-bubble-icons/041-piggy-bank.svg')">
                </div>

                <h4 class="cell feature-title">First studio is free</h4>

                <p class="cell feature-text">
                    <span class="text-serif">Free</span> accounts can list up to one room for free.
                    No credit card will be asked.
                </p>
            </div>
        </div>

        <div
            class="cell medium-4 show-for-medium"
            v-if="isPremium">
            <div class="grid-y feature-block">
                <div class="cell shrink feature-image">
                    <img
                        alt="Discounted rate"
                        :src="assetURL('images/vendor/business-bubble-icons/074-credit-card.svg').url">
                </div>

                <h4 class="cell feature-title">Online payments</h4>

                <p class="cell feature-text">
                    Choose to accept accept online payments and reduce the loss of revenue when
                    musicians do not show up.
                </p>
            </div>
        </div>

        <div class="cell small-12">
            <hr>
        </div>

        <div class="cell small-12">
            <studio-form
                :csrf-token="csrfToken"
                :value="value"
                :errors="errors"
                :can-onsite-payments="canOnsitePayments"
                :can-add-fee="canAddFee"
                :shownSections="shownSections">
            </studio-form>
        </div>

        <div
            class="cell small-12"
            :class="{
                'text-center': isFirstStep,
                'text-right': !isFirstStep,
            }">
            <button
                type="submit" class="button primary large small-only-expanded"
                @click="continueClicked">
                <span v-if="isFirstStep">Let's go!</span>
                <span v-else-if="!isLastStep">Continue</span>
                <span v-else>Save and continue</span>
            </button>
        </div>
    </div>
</template>

<script lang="ts">
import Vue from "vue";

import StudioForm from './StudioForm.vue';

import { fromCDN } from '../../../../misc/URL';

declare var NC_ROUTES: any;

export default Vue.extend({
    props: {
        isPremium: { type: Boolean, required: false, default: false },

        // StudioForm's properties
        csrfToken: { type: String, required: false },
        value: { type: Object, required: false },
        errors: { type: Object, required: false },

        canOnsitePayments: { type: Boolean, default: false },
        canAddFee: { type: Boolean, default: false },
    },
    data() {
        return {
            sections: [
                'general-info', 'location', 'opening-schedule', 'pricing-policy',
                'booking-policy', 'payment-policy', 'equipments', 'pictures',
            ],

            step: 0,
        };
    },
    computed: {
        shownSections() {
            return this.sections.slice(0, this.step);
        },

        isFirstStep() {
            return this.step == 0;
        },

        isLastStep() {
            return this.step == this.sections.length;
        },
    },
    methods: {
        continueClicked(event) {
            if (!this.isLastStep) {
                ++this.step;

                // Slides to the next step.
                let sectionName = this.sections[this.step - 1];
                setTimeout(() => {
                    $('html, body').animate({
                        scrollTop: $("#" + sectionName).offset().top
                    }, 500);
                }, 500);

                event.preventDefault(); // Prevents form submission
            }
        },

        assetURL(assetName: string) {
            return fromCDN(NC_ROUTES.controllers.Assets.versioned(assetName));
        }
    },
    components: { StudioForm },
});
</script>

<style>
</style>
