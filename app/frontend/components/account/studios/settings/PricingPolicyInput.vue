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

                <money-input
                    :name="fieldName('price-per-hour')"
                    :currency="currency"
                    v-model="pricePerHour"
                    :required="true">
                </money-input>

                <span v-if="errors['price-per-hour']" class="error">
                    {{ errors['price-per-hour'] }}
                </span>
            </label>
        </div>

        <!-- Evening sessions -->

        <div class="cell small-12">
            <div class="checkbox-group">
                <input
                    id="pricing-has-evening-pricing"
                    type="checkbox"
                    :name="fieldName('has-evening-pricing')"
                    v-model="hasEveningPricing"
                    value="true">

                <label for="pricing-has-evening-pricing">
                    Pricing is different for evening sessions

                    <span v-if="errors['has-evening-pricing']" class="error">
                        {{ errors['has-evening-pricing'] }}
                    </span>
                </label>
            </div>
        </div>

        <slide-down-transition :max-height="85">
            <div
                class="cell grid-x"
                v-if="hasEveningPricing">
                <div class="cell small-12 medium-4 medium-offset-1 large-3">
                    <label>
                        Evening begins at

                        <input type="time"
                            :name="fieldName('evening-begins-at')"
                            v-model="eveningBeginsAt"
                            pattern="[0-9]{2}:[0-9]{2}"
                            :disabled="!hasEveningPricing"
                            :required="hasEveningPricing">

                        <span v-if="errors['evening-begins-at']" class="error">
                            {{ errors['evening-begins-at'] }}
                        </span>
                    </label>
                </div>

                <div class="cell small-12 medium-4 medium-offset-1 large-3">

                    <label>
                        Price per hour

                        <money-input
                            :name="fieldName('evening-price-per-hour')"
                            :currency="currency"
                            v-model="eveningPricePerHour"
                            :disabled="!hasEveningPricing"
                            :required="hasEveningPricing">
                        </money-input>

                        <span v-if="errors['evening-price-per-hour']" class="error">
                        {{ errors['evening-price-per-hour'] }}
                        </span>
                    </label>
                </div>
            </div>
        </slide-down-transition>

        <!-- Weekend sessions -->

        <div class="cell small-12">
            <div class="checkbox-group">
                <input
                    id="pricing-has-weekend-pricing"
                    type="checkbox"
                    :name="fieldName('has-weekend-pricing')"
                    v-model="hasWeekendPricing"
                    value="true">

                <label for="pricing-has-weekend-pricing">
                    Pricing is different for weekend sessions

                    <span v-if="errors['has-weekend-pricing']" class="error">
                        {{ errors['has-weekend-pricing'] }}
                    </span>
                </label>
            </div>
        </div>

        <slide-down-transition :max-height="85">
            <div
                class="cell small-12 medium-4 medium-offset-1 large-3"
                v-if="hasWeekendPricing">

                <label>
                    Price per hour

                    <money-input
                        :name="fieldName('weekend-price-per-hour')"
                        :currency="currency"
                        v-model="weekendPricePerHour"
                        :disabled="!hasWeekendPricing"
                        :required="hasWeekendPricing">
                    </money-input>

                    <span v-if="errors['weekend-price-per-hour']" class="error">
                        {{ errors['weekend-price-per-hour'] }}
                    </span>
                </label>
            </div>
        </slide-down-transition>

        <div
            class="cell"
            v-if="globalError">
            <label>
                <span class="error">{{ globalError }}</span>
            </label>
        </div>
    </div>
</template>

<script lang="ts">
import Vue from "vue";

import VueInput from '../../../widgets/VueInput';
import MoneyInput from '../../../widgets/MoneyInput.vue';
import SlideDownTransition from '../../../../transitions/SlideDownTransition.vue';

export default Vue.extend({
    mixins: [VueInput],
    props: {
        value: { type: Object, default() { return {}; } },

        currency: { type: String, required: false },
    },
    data() {
        let data = {
            pricePerHour: null,

            hasEveningPricing: false,
            eveningBeginsAt: null,
            eveningPricePerHour: null,

            hasWeekendPricing: false,
            weekendPricePerHour: null,
        };

        if ('price-per-hour' in this.value) {
            data.pricePerHour = this.value['price-per-hour'];
        }

        if ('has-evening-pricing' in this.value) {
            data.hasEveningPricing = this.value['has-evening-pricing'] == 'true';
        }
        if ('evening-begins-at' in this.value) {
            data.eveningBeginsAt = this.value['evening-begins-at'];
        }
        if ('evening-price-per-hour' in this.value) {
            data.eveningPricePerHour = this.value['evening-price-per-hour'];
        }

        if ('has-weekend-pricing' in this.value) {
            data.hasWeekendPricing = this.value['has-weekend-pricing'] == 'true';
        }
        if ('weekend-price-per-hour' in this.value) {
            data.weekendPricePerHour = this.value['weekend-price-per-hour'];
        }

        return data;
    },
    components: { MoneyInput, SlideDownTransition }
});
</script>

<style>
</style>
