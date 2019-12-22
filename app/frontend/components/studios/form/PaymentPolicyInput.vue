
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

  Provides an set of input widgets for the studio's payment policy (i.e. online
  and onsite payments).
-->

<template>
    <div class="grid-x grid-margin-x">
        <p class="cell">NoisyCamp supports online and onsite payments.</p>

        <div class="cell">
            <input
                id="payment-policy-has-online-payment"
                type="checkbox"
                :name="fieldName('has-online-payment')"
                v-model="hasOnlinePayment"
                value="true">

            <label for="payment-policy-has-online-payment">
                Allow online payments using credit and debit cards

                <span v-if="fieldHasError('has-online-payment')" class="error">
                    {{ fieldError('has-online-payment') }}
                </span>
            </label>

            <p class="help-text">
                Payment of booking fees will be securely collected online at the
                time the customer books his/her session.
            </p>
        </div>

        <slide-down-transition :max-height="215">
            <div
                class="cell small-12 medium-offset-1 medium-11"
                v-if="hasOnlinePayment">

                <p class="callout secondary">
                    Please complete your bank account details.<br>
                    Revenue collected from online payments will be automatically
                    added to your balance and transferred to your bank account
                    twice per month.<br>
                    <a href="#">Learn more about online payments</a>.
                </p>

                <div class="grid-x grid-margin-x">
                    <div class="cell large-8">
                        <payout-method-input
                            :name="fieldName('payout-method')"
                            :errors="errors">
                        </payout-method-input>
                    </div>
                </div>
            </div>
        </slide-down-transition>

        <div class="cell">
            <input
                id="payment-policy-has-onsite-payment"
                type="checkbox"
                :name="fieldName('has-onsite-payment')"
                v-model="hasOnsitePayment"
                value="true">

            <label for="payment-policy-has-onsite-payment">
                Allow onsite payments

                <span v-if="fieldHasError('has-onsite-payment')" class="1error">
                    {{ fieldError('has-onsite-payment') }}
                </span>
            </label>

            <p class="help-text">
                You will be in charge of collecting the fee payment from the
                customers that choose this payment method.
            </p>

            <div
                class="callout warning"
                v-if="hasCancellationNotice">
                Please note that NoisyCamp will not be able to enforce your
                booking cancellation policy on these onsite payments.<br>
                If you wish your customers to comply with your cancellation
                policy, disable onsite payments.
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import Vue from "vue";

import VueInput from '../../widgets/VueInput';
import SlideDownTransition from '../../../transitions/SlideDownTransition.vue';

import PayoutMethodInput from './PayoutMethodInput.vue';

export default Vue.extend({
    mixins: [VueInput],
    props: {
        hasCancellationNotice: { type: Boolean, default: false },
    },
    data() {
        return {
            hasOnlinePayment: true,
            hasOnsitePayment: false,
        }
    },
    components: { PayoutMethodInput, SlideDownTransition },
});
</script>

<style>
</style>
