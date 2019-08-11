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

  Provides an set of input widgets for the studio's pricing policy.
-->

<template>
    <div class="grid-x grid-margin-x">
        <div class="cell small-12 medium-4 large-3">
            <label>
                Price per hour

                <div class="input-group">
                    <span class="input-group-label">$</span>
                    <input
                        class="input-group-field"
                        type="number"
                        v-model="pricePerHour">
                </div>
            </label>
        </div>

        <!-- Evening sessions -->

        <div class="cell small-12">
            <input
                id="pricing-has-evening-pricing"
                type="checkbox"
                v-model="hasEveningPricing">

            <label for="pricing-has-evening-pricing">
                This place has a different pricing for evening sessions
            </label>
        </div>

        <div
            class="cell small-12 medium-4 medium-offset-1 large-3"
            v-if="hasEveningPricing">
            <label>
                Evening begins at

                <input type="time"
                    v-model="eveningBeginsAt"
                    pattern="[0-9]{2}:[0-9]{2}"
                    required>
            </label>
        </div>

        <div
            class="cell small-12 medium-4 medium-offset-1 large-3"
            v-if="hasEveningPricing">
            <label>
                Price per hour

                <div class="input-group">
                    <span class="input-group-label">$</span>
                    <input
                        class="input-group-field"
                        type="number"
                        v-model="eveningPricePerHour">
                </div>
            </label>
        </div>

        <!-- Weekend sessions -->

        <div class="cell small-12">
            <input
                id="pricing-has-weekend-pricing"
                type="checkbox"
                v-model="hasWeekendPricing">

            <label for="pricing-has-weekend-pricing">
                This place has a different pricing for weekend sessions
            </label>
        </div>

        <div
            class="cell small-12 medium-4 medium-offset-1 large-3"
            v-if="hasWeekendPricing">
            <label>
                Price per hour

                <div class="input-group">
                    <span class="input-group-label">$</span>
                    <input
                        class="input-group-field"
                        type="number"
                        v-model="weekendPricePerHour">
                </div>
            </label>
        </div>
    </div>
</template>

<script lang="ts">
import Vue from "vue";

export default Vue.extend({
    props: {
    },
    data() {
        return {
            pricePerHour: null,

            hasEveningPricing: false,
            eveningBeginsAt: null,
            eveningPricePerHour: null,

            hasWeekendPricing: false,
            weekendPricePerHour: null,
        };
    },
    computed: {
    },
});
</script>

<style>
</style>
