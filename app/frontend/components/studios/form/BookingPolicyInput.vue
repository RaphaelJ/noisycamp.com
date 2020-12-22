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

  Provides an set of input widgets for the studio's booking and cancellation policies.
-->

<template>
    <div class="grid-x grid-margin-x">
        <div class="cell small-12 medium-10 large-5">
            <label>
                Minimum booking duration

                <select
                    :name="fieldName('min-booking-duration')"
                    v-model="minBookingDuration"
                    required>
                    <option value="" disabled>Please select a value</option>
                    <option value="3600">1 hour</option>
                    <option value="7200">2 hours</option>
                    <option value="14400">4 hours</option>
                    <option value="28800">8 hours</option>
                </select>

                <span v-if="fieldHasError('min-booking-duration')" class="error">
                    {{ fieldError('min-booking-duration') }}
                </span>
            </label>
        </div>

        <div class="cell small-12">
            <div class="checkbox-group">
                <input
                    id="booking-policy-automatic-approval"
                    type="checkbox"
                    :name="fieldName('automatic-approval')"
                    v-model="automaticApproval"
                    value="true">

                <label for="booking-policy-automatic-approval">
                    Automatically accept booking requests

                    <span v-if="fieldHasError('automatic-approval')" class="error">
                        {{ fieldError('automatic-approval') }}
                    </span>
                </label>
            </div>

            <p class="help-text">
                If disabled, every booking request will have to be manually approved within 7 days,
                or will be automatically cancelled and the customer fully refunded
            </p>
        </div>

        <div class="cell small-12">
            <div class="checkbox-group">
                <input
                    id="booking-policy-can-cancel"
                    type="checkbox"
                    :name="fieldName('can-cancel')"
                    v-model="canCancel"
                    value="true">

                <label for="booking-policy-can-cancel">
                    Reimburse customers who cancel their booking

                    <span v-if="fieldHasError('can-cancel')" class="error">
                        {{ fieldError('can-cancel') }}
                    </span>
                </label>
            </div>
        </div>

        <slide-down-transition :max-height="135">
            <div
                class="cell small-12 medium-10 medium-offset-1 large-5"
                v-if="canCancel">

                <label>
                    Minimum cancellation notice

                    <select
                        :name="fieldName('cancellation-notice')"
                        v-model="cancellationNotice"
                        :disabled="!canCancel"
                        :required="canCancel">
                        <option value="" disabled>Please select a value</option>
                        <option value="0">No notice</option>
                        <option value="3600">1 hour before the session</option>
                        <option value="43200">12 hours before the session</option>
                        <option value="86400">24 hours before the session</option>
                        <option value="172800">48 hours before the session</option>
                        <option value="604800">1 week before the session</option>
                    </select>

                    <span v-if="fieldHasError('cancellation-notice')" class="error">
                        {{ fieldError('cancellation-notice') }}
                    </span>
                </label>

                <p class="help-text">
                    Customers that cancel their booking before that time period will be fully refunded.
                </p>
            </div>
        </slide-down-transition>
    </div>
</template>

<script lang="ts">
import Vue from "vue";

import VueInput from '../../widgets/VueInput';
import SlideDownTransition from '../../../transitions/SlideDownTransition.vue';

export default Vue.extend({
    mixins: [VueInput],
    props: {
        value: { type: Object, default() { return {}; } }
    },
    data() {
        let data = {
            automaticApproval: false,

            minBookingDuration: null,

            canCancel: false,
            cancellationNotice: null,
        };

        if ('automatic-approval' in this.value) {
            data.automaticApproval = this.value['automatic-approval'] == 'true';
        }

        if ('min-booking-duration' in this.value) {
            data.minBookingDuration = this.value['min-booking-duration'];
        }

        if ('can-cancel' in this.value) {
            data.canCancel = this.value['can-cancel'] == 'true';
        }
        if ('cancellation-notice' in this.value) {
            data.cancellationNotice = this.value['cancellation-notice'];
        }

        return data;
    },
    methods: {
        emitCancellationPolicyChanged() {
            this.$emit('cancellation-policy-changed', {
                'can-cancel': this.canCancel,
                'cancellation-notice': this.cancellationNotice,
            });
        },
    },
    watch: {
        canCancel() {
            this.emitCancellationPolicyChanged();
        },

        cancellationNotice() {
            this.emitCancellationPolicyChanged();
        },
    },
    components: { SlideDownTransition },
});
</script>

<style>
</style>
