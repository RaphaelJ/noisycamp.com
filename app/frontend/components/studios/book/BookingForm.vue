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
    <form
        class="payment-method-form"
        method="post"
        :action="bookingSubmitUrl">

        <input
            v-if="csrfToken"
            type="hidden" name="csrfToken"
            :value="csrfToken">

        <input type="hidden"
            name="booking-times.begins-at"
            :value="bookingTimes['begins-at']">
        <input type="hidden"
            name="booking-times.duration"
            :value="bookingTimes['duration']">

        <input 
            v-for="equip in equipments"
            :key="equip.id"
            type="hidden"
            name="equipments[]"
            :value="equip.id">

        <h3 class="text-sans-serif"><small>Please choose your payment method</small></h3>

        <fieldset>
            <div class="grid-y">
                <div
                    class="cell"
                    v-if="hasOnlinePayment">
                    <div class="radio-group reduced-bottom-margin">
                        <input
                            type="radio"
                            name="payment-method"
                            v-model="paymentMethod"
                            value="online"
                            id="payment-method-online">
                        <label for="payment-method-online">
                            Credit/Debit card

                            <span class="card-icons">
                                <img
                                    v-for="card in cardIcons"
                                    :alt="card[0]"
                                    :src="card[1]">
                            </span>
                        </label>
                    </div>
                </div>

                <div
                    class="cell"
                    v-if="hasOnsitePayment">
                    <div class="radio-group">
                        <input
                            type="radio"
                            name="payment-method"
                            v-model="paymentMethod"
                            value="onsite"
                            id="payment-method-onsite">
                        <label for="payment-method-onsite">On site</label>
                    </div>
                    <p class="help-text">
                        Payment will be requested by the studio's manager before or after you begin
                        your session.
                    </p>
                </div>
            </div>
        </fieldset>

        <hr>

        <div class="text-right">
            <button
                type="submit"
                class="button primary large small-only-expanded">

                <span v-if="paymentMethod == 'online'">
                    Continue to checkout
                </span>
                <span v-if="paymentMethod == 'onsite'">
                    Confirm booking
                </span>
            </button>
        </div>
    </form>
</template>

<script lang="ts">
import Vue, { PropOptions } from "vue";

import { fromCDN } from '../../../misc/URL';

import BookingPricingCalculator from '../BookingPricingCalculator.vue';

declare var NC_ROUTES: any;

export default Vue.extend({
    props: {
        studioId: { type: Number, required: true },

        openingSchedule: <PropOptions<Object[]>>{ type: Array, required: true },
        pricingPolicy: { type: Object, required: true },

        bookingTimes: { type: Object, required: true },

        equipments: <PropOptions<Object[]>>{ type: Array, required: true },

        hasOnlinePayment: { type: Boolean, required: true },
        hasOnsitePayment: { type: Boolean, required: true },

        csrfToken: { type: String, required: false },
    },
    data() {
        return {
            // Default payment method to online if availaible.
            paymentMethod: this.hasOnlinePayment ? 'online' : 'onsite',
        }
    },
    computed: {
        bookingSubmitUrl() {
            return NC_ROUTES.controllers.studios.BookingController.submit(this.studioId).url;
        },

        cardIcons() {
            let cards = [
                ['Visa', 'visa.svg'],
                ['MasterCard', 'mastercard-1.svg'],
                ['American Express', 'amex.svg']
            ];

            return cards.map(card => [
                card[0],
                fromCDN(NC_ROUTES.controllers.Assets.versioned(
                    "images/vendor/card-icons/" + card[1]
                ))
            ]);
        }
    },
    components: { BookingPricingCalculator }
});
</script>

<style>
.payment-method-form .card-icons {
    margin-left: 1.5rem;
}

.payment-method-form .card-icons img {
    height: 1.5rem;
    margin-left: 0.3rem;
    margin-right: 0.3rem;
}

.payment-method-form .radio-group.reduced-bottom-margin {
    margin-bottom: 1rem;
}
</style>
