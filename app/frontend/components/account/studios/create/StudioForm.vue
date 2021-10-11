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

  Provides a form to edit or create a new studio.
-->

<template>
    <div>
        <input type="hidden" name="csrfToken" :value="csrfToken">

        <!-- General information -->

        <slide-down-transition :max-height="700">
            <div
                class="cell"
                id="general-info"
                v-if="isShown('general-info')">

                <h2>General information</h2>

                <general-info-input
                    name="general-info"
                    :errors="errors['general-info']"
                    :value="value['general-info']">
                </general-info-input>

                <hr>
            </div>
        </slide-down-transition>

        <!-- Location -->

        <slide-down-transition :max-height="850">
            <div
                class="cell"
                id="location"
                v-if="isShown('location')">

                <h2>Location</h2>

                <location-input
                    name="location"
                    v-model="location"
                    :errors="errors['location']">
                </location-input>
                <hr>
            </div>
        </slide-down-transition>

        <!-- Opening schedule -->

        <slide-down-transition :max-height="650">
            <div
                class="cell"
                id="opening-schedule"
                v-if="isShown('opening-schedule')">

                <h2>Opening schedule</h2>

                <p>Select studio's opening days and hours.</p>

                <opening-schedule-input
                    name="opening-schedule"
                    :value="value['opening-schedule']"
                    :errors="errors['opening-schedule']">
                </opening-schedule-input>

                <hr>
            </div>
        </slide-down-transition>

        <!-- Pricing -->

        <slide-down-transition :max-height="300">
            <div
                class="cell"
                id="pricing-policy"
                v-if="isShown('pricing-policy')">

                <h2>Pricing</h2>

                <pricing-policy-input
                    name="pricing-policy"
                    :value="value['pricing-policy']"
                    :errors="errors['pricing-policy']"
                    :currency="currency">
                </pricing-policy-input>

                <hr>
            </div>
        </slide-down-transition>

        <!-- Booking and cancellation policy -->

        <slide-down-transition :max-height="315">
            <div
                class="cell"
                id="booking-policy"
                v-if="isShown('booking-policy')">

                <h2>Booking &amp; cancellation policy</h2>

                <booking-policy-input
                    name="booking-policy"
                    :value="value['booking-policy']"
                    :errors="errors['booking-policy']"
                    v-on:cancellation-policy-changed="cancellationPolicyChanged">
                </booking-policy-input>

                <hr>
            </div>
        </slide-down-transition>

        <!-- Payment policy -->

        <slide-down-transition :max-height="315">
            <div
                class="cell"
                id="payment-policy"
                v-if="isShown('payment-policy')">

                <h2>Payment policy</h2>

                <payment-policy-input
                    name="payment-policy"
                    :value="value['payment-policy']"
                    :errors="errors['payment-policy']"
                    :can-cancel-anytime="canCancelAnytime">
                </payment-policy-input>

                <hr>
            </div>
        </slide-down-transition>

        <!-- Extra rules -->

        <slide-down-transition :max-height="315">
            <div
                class="cell"
                id="extra-rules"
                v-if="isShown('extra-rules')">

                <h2>Extra rules</h2>

                <hr>
            </div>
        </slide-down-transition>

        <!-- Equipments & instruments -->

        <slide-down-transition :max-height="315">
            <div
                class="cell"
                id="equipments"
                v-if="isShown('equipments')">

                <h2>Equipment &amp; instruments</h2>

                <p>
                    List the instruments and various equipments that come with your music space.
                </p>

                <equipment-input
                    name="equipments"
                    :value="value['equipments']"
                    :errors="errors['equipments']"
                    :can-add-fee="canAddFee"
                    :currency="currency">
                </equipment-input>

                <hr>
            </div>
        </slide-down-transition>

        <!-- Pictures -->

        <slide-down-transition :max-height="320">
            <div
                class="cell"
                id="pictures"
                v-if="isShown('pictures')">

                <h2>Pictures</h2>

                <picture-input
                    name="pictures[]"
                    :value="value['pictures']"
                    :errors="errors['pictures']">
                </picture-input>

                <hr>
            </div>
        </slide-down-transition>
    </div>
</template>

<script lang="ts">
import Vue, { PropOptions } from "vue";

import BookingPolicyInput from './BookingPolicyInput.vue';
import EquipmentInput from './EquipmentInput.vue';
import GeneralInfoInput from './GeneralInfoInput.vue';
import LocationInput from './LocationInput.vue';
import OpeningScheduleInput from './OpeningScheduleInput.vue';
import PaymentPolicyInput from './PaymentPolicyInput.vue';
import PictureInput from './PictureInput.vue';
import PricingPolicyInput from './PricingPolicyInput.vue';
import SlideDownTransition from '../../../../transitions/SlideDownTransition.vue';

declare var NC_CONFIG: any;

export default Vue.extend({
    props: {
        csrfToken: { type: String, required: false },

        // The form data as a JSON object.
        value: {
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

        canAddFee: { type: Boolean, default: false },

        // If defined, will only show the specified sections of the form.
        shownSections: <PropOptions<String[]>>{
            type: Array, required: false,
            default() {
                return [
                    'general-info', 'location', 'opening-schedule', 'pricing-policy',
                    'booking-policy', 'payment-policy', 'equipments', 'pictures',
                ];
            }
        },
    },
    data() {
        var location;
        if ('location' in this.value) {
            location = this.value['location'];
        } else {
            location = {
                address: {},
                coordinates: {}
            };
        }

        var cancellationPolicy = null;
        if ('booking-policy' in this.value) {
            cancellationPolicy = this.value['booking-policy'];
        }

        return {
            location: location,
            cancellationPolicy: cancellationPolicy,
        };
    },
    computed: {
        // The currency of the money input fields. Default to Euro, but changes
        // once the user sets the studio location.
        currency() {
            let address = this.location.address;

            if (address && address.country) {
                return NC_CONFIG.countries[address.country].currency;
            } else {
                return 'EUR';
            }
        },

        canCancelAnytime() {
            if (!this.cancellationPolicy || !this.cancellationPolicy['cancellation-notice']) {
                return null;
            } else {
                return this.cancellationPolicy['can-cancel']
                    && this.cancellationPolicy['cancellation-notice'] == 0;
            }
        }
    },
    methods: {
        isShown(section) {
            return this.shownSections.includes(section);
        },

        cancellationPolicyChanged(policy) {
            this.cancellationPolicy = policy;
        }
    },
    components: {
        BookingPolicyInput, EquipmentInput, GeneralInfoInput, LocationInput,
        OpeningScheduleInput, PaymentPolicyInput, PictureInput, PricingPolicyInput,
        SlideDownTransition,
    },
});
</script>

<style>
</style>
