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
        <div
            class="cell small-12 error"
            v-if="fieldHasError('')">
            {{ fieldError('') }}
        </div>

        <div class="cell small-12 medium-4 large-3">
            <label>
                Price per hour

                <currency-input
                    :name="fieldName('price-per-hour')"
                    :currency="currency"
                    v-model="pricePerHour"
                    required="true">
                </currency-input>

                <span v-if="fieldHasError('price-per-hour')" class="error">
                    {{ fieldError('price-per-hour') }}
                </span>
            </label>
        </div>

        <!-- Evening sessions -->

        <div class="cell small-12">
            <input
                id="pricing-has-evening-pricing"
                type="checkbox"
                :name="fieldName('has-evening-pricing')"
                v-model="hasEveningPricing"
                value="true">

            <label for="pricing-has-evening-pricing">
                This place has a different pricing for evening sessions

                <span v-if="fieldHasError('has-evening-pricing')" class="error">
                    {{ fieldError('has-evening-pricing') }}
                </span>
            </label>
        </div>

        <div class="cell small-12 medium-4 medium-offset-1 large-3">
            <label>
                Evening begins at

                <input type="time"
                    :name="fieldName('evening-begins-at')"
                    v-model="eveningBeginsAt"
                    pattern="[0-9]{2}:[0-9]{2}"
                    :disabled="!hasEveningPricing"
                    :required="hasEveningPricing">

                <span v-if="fieldHasError('evening-begins-at')" class="error">
                    {{ fieldError('evening-begins-at') }}
                </span>
            </label>
        </div>

        <div class="cell small-12 medium-4 medium-offset-1 large-3">
            <label>
                Price per hour

                <currency-input
                    :name="fieldName('evening-price-per-hour')"
                    :currency="currency"
                    v-model="eveningPricePerHour"
                    :disabled="!hasEveningPricing"
                    :required="hasEveningPricing">
                </currency-input>

                <span v-if="fieldHasError('evening-price-per-hour')" class="error">
                    {{ fieldError('evening-price-per-hour') }}
                </span>
            </label>
        </div>

        <!-- Weekend sessions -->

        <div class="cell small-12">
            <input
                id="pricing-has-weekend-pricing"
                type="checkbox"
                :name="fieldName('has-weekend-pricing')"
                v-model="hasWeekendPricing"
                value="true">

            <label for="pricing-has-weekend-pricing">
                This place has a different pricing for weekend sessions

                <span v-if="fieldHasError('has-weekend-pricing')" class="error">
                    {{ fieldError('has-weekend-pricing') }}
                </span>
            </label>
        </div>

        <div class="cell small-12 medium-4 medium-offset-1 large-3">
            <label>
                Price per hour

                <currency-input
                    :name="fieldName('weekend-price-per-hour')"
                    :currency="currency"
                    v-model="weekendPricePerHour"
                    :disabled="!hasWeekendPricing"
                    :required="hasWeekendPricing">
                </currency-input>

                <span v-if="fieldHasError('weekend-price-per-hour')" class="error">
                    {{ fieldError('weekend-price-per-hour') }}
                </span>
            </label>
        </div>
    </div>
</template>

<script lang="ts">
import Vue from "vue";

import VueInput from '../../widgets/VueInput';
import CurrencyInput from '../../widgets/CurrencyInput.vue';

export default Vue.extend({
    mixins: [VueInput],
    props: {
        currency: { type: String, required: false },
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
    components: { CurrencyInput, }
});
</script>

<style>
</style>
