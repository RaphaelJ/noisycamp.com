<!--
  Noisycamp is a platform for booking music studios.
  Copyright (C) 2019 2020  Raphael Javaux <raphael@noisycamp.com>

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
            <div class="checkbox-group">
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
            </div>

            <p class="help-text">
                Payment of booking fees will be securely collected online at the time the customer
                books their session.<br>
                To allow secure online payments, we will ask you for a valid bank account and we
                will verify your identity.
            </p>
        </div>

        <div class="cell">
            <div class="checkbox-group">
                <input
                    id="payment-policy-has-onsite-payment"
                    type="checkbox"
                    :name="fieldName('has-onsite-payment')"
                    v-model="hasOnsitePayment"
                    value="true">

                <label for="payment-policy-has-onsite-payment">
                    Allow onsite payments

                    <span v-if="fieldHasError('has-onsite-payment')" class="error">
                        {{ fieldError('has-onsite-payment') }}
                    </span>
                </label>
            </div>

            <p class="help-text">
                You will be in charge of collecting the booking payment from the
                customers that choose this payment method.
            </p>

            <slide-down-transition :max-height="100">
                <div
                    class="grid-x grid-margin-y grid-margin-x callout secondary"
                    v-if="hasOnsitePayment && canCancelAnytime === false">

                    <h5>
                        <strong>
                            <i class="fi-alert"></i>&nbsp;
                            We will not be able to enforce your booking cancellation policy on onsite
                            payments.
                        </strong>
                    </h5>

                    <p>
                        If you wish your customers to always comply with your cancellation policy,
                        disable onsite payments, or adapt your cancellation policy.
                    </p>
                </div>
            </slide-down-transition>
        </div>
    </div>
</template>

<script lang="ts">
import Vue from "vue";

import VueInput from '../../widgets/VueInput';
import SlideDownTransition from '../../../transitions/SlideDownTransition.vue';

export default Vue.extend({
    mixins: [VueInput],
    props: {
        value: { type: Object, default() { return {}; } },
        canCancelAnytime: { type: Boolean },
    },
    data() {
        let data =  {
            hasOnlinePayment: true,
            hasOnsitePayment: false,
        };

        if ('has-online-payment' in this.value) {
            data.hasOnlinePayment = this.value['has-online-payment'] == 'true';
        }

        if ('has-onsite-payment' in this.value) {
            data.hasOnsitePayment = this.value['has-onsite-payment'] == 'true';
        }

        return data;
    },
    components: { SlideDownTransition },
});
</script>

<style>
</style>
