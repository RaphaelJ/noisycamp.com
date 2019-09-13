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

  Provides a form to edit or create a new studio.
-->

<template>
    <form
        :action="action" :method="method"
        class="grid-x">

        <input
            v-if="csrfToken"
            type="hidden" name="csrfToken"
            :value="csrfToken">

        <!-- General information -->

        <div
            class="cell"
            v-if="isShown('general-info')">

            <h2>General information</h2>

            <general-info-input name="general-info" :errors="errors"></general-info-input>

            <hr>
        </div>

        <!-- Location -->

        <div
            class="cell"
            v-if="isShown('location')">

            <h2>Location</h2>

            <address-input name="location" :errors="errors"></address-input>

            <hr>
        </div>

        <!-- Opening times -->

        <div
            class="cell"
            v-if="isShown('opening-times')">

            <h2>Opening times</h2>

            <p>Select studio's opening days and hours.</p>

            <p>
                This will define the general avalaibility of your studio. More specific opening
                schedule, like holidays, can be defined later in the calendar application.
            </p>

            <opening-times-input name="opening-times" :errors="errors"></opening-times-input>

            <hr>
        </div>

        <!-- Pricing -->

        <div
            class="cell"
            v-if="isShown('pricing')">

            <h2>Pricing</h2>

            <pricing-policy-input
                name="pricing"
                currency="ISK">
            </pricing-policy-input>

            <hr>
        </div>

        <!-- Booking and cancellation policy -->

        <div
            class="cell"
            v-if="isShown('booking-policy')">

            <h2>Booking &amp; cancellation policy</h2>

            <booking-policy-input name="booking-policy"></booking-policy-input>

            <hr>
        </div>

        <!-- Extra rules -->

        <div
            class="cell"
            v-if="isShown('extra-rules')">

            <h2>Extra rules</h2>

            <hr>
        </div>

        <!-- Pictures -->

        <div
            class="cell"
            v-if="isShown('pictures')">

            <h2>Pictures</h2>

            <picture-input name="pictures"></picture-input>

            <hr>
        </div>

        <!-- Equipment & instruments -->

        <div
            class="cell"
            v-if="isShown('equipment')">

            <h2>Equipment &amp; instruments</h2>

            <p>
                List the instruments and various equipments that come with your rehearsal place.
                This will allow user to easily find places that match their needs.<br>
                You can request an extra fee for some of these items.
            </p>

            <equipment-input
                name="equipment"
                currency="ISK">
            </equipment-input>

            <hr>
        </div>

        <!-- Payout -->

        <div
            class="cell"
            v-if="isShown('payout')">

            <h2>Payout</h2>

            <p>
                Noisycamp will send the revenue from your rentals twice per month using international
                wire transfer. Read more about payouts.
            </p>

            <payout-input name="payout"></payout-input>

            <hr>
        </div>

        <div class="cell text-right">
            <button type="submit" class="button large small-only-expanded">
                Save and continue
            </button>
        </div>
    </form>
</template>

<script lang="ts">
import Vue from "vue";

import AddressInput from '../widgets/AddressInput.vue';

import BookingPolicyInput from './form/BookingPolicyInput.vue';
import EquipmentInput from './form/EquipmentInput.vue';
import GeneralInfoInput from './form/GeneralInfoInput.vue';
import OpeningTimesInput from './form/OpeningTimesInput.vue';
import PayoutInput from './form/PayoutInput.vue';
import PictureInput from './form/PictureInput.vue';
import PricingPolicyInput from './form/PricingPolicyInput.vue';

export default Vue.extend({
    props: {
        // The target and method of the form (i.e. URL and GET/POST).
        action: { type: String, required: true },
        method: { type: String, required: true },

        csrfToken: { type: String, required: false },

        // The form data, indexed by the field's name.
        data: {
            type: Object, required: false,
            default: function () { return {}; }
        },

        // The form errors, indexed by the field's name (see
        // `play.api.data.Form.errorsAsJson`)
        errors: {
            type: Object, required: false,
            default: function () { return {}; }
        },

        // If true, the form will be displayed as a multipage form with next
        // and previous navigation buttons.
        isMultiPage: { type: Boolean, required: false, default: false },
    },
    data() {
        return {
        };
    },
    methods: {
        isShown(section) {
            return [
                /*'general-info', 'location', 'opening-times', 'pricing', */ 'booking-policy'
            ].includes(section);
        }
    },
    components: {
        AddressInput, BookingPolicyInput, EquipmentInput, GeneralInfoInput,
        OpeningTimesInput, PayoutInput, PictureInput, PricingPolicyInput,
    },
});
</script>

<style>
</style>
