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

  Provides an set of input widget for
-->

<template>
    <div>
        <div class="grid-x grid-margin-x">
            <div class="cell small-12 medium-6 large-3">
                <label>Billing country</label>

                <country-select
                    name="country"
                    v-model="country">
                </country-select>
            </div>

            <label class="cell small-12 medium-6 large-3">
                Recipient type

                <select
                    name="recipient-type"
                    required>
                    <option value="business">Business</option>
                    <option value="private">Private</option>
                </select>
            </label>
        </div>

        <input type="hidden" name="payout-method" :value="payoutMethod">

        <div class="grid-x grid-margin-x"
            v-if="payoutMethod == 'IBAN'">
            <!-- IBAN -->
            <label class="cell small-12 medium-8 large-4">
                IBAN

                <input
                    type="text"
                    name="iban"
                    placeholder="DE89370400440532013000"
                    minlength="2"
                    required>
            </label>

            <label class="cell small-12 medium-4 large-2">
                Bank code (BIC/SWIFT)

                <input
                    type="text"
                    name="bic"
                    placeholder="GKCCBEBB"
                    pattern="(?i)[A-Z]{6}[A-Z\\d]{2}([A-Z\\d]{3})?"
                    required>
            </label>
        </div>

        <div class="grid-x grid-margin-x"
            v-if="payoutMethod == 'ABA'">
            <!-- ABA -->
            <label class="cell small-12 medium-4 large-3">
                Routing number

                <input
                    type="text"
                    name="routing-number"
                    placeholder="111000025"
                    minlength="9"
                    maxlength="9"
                    pattern="^\\d{9}$"
                    required>
            </label>

            <label class="cell small-12 medium-4 large-3">
                Account Number

                <input
                    type="text"
                    name="account-number"
                    placeholder="12345678"
                    minlength="4"
                    maxlength="17"
                    pattern="^\\d{4,17}$"
                    required>
            </label>

            <div class="cell small-12 medium-4 large-3">
                <label>
                    Account type

                    <select
                        name="account-type"
                        required>
                        <option value="checking">Checking</option>
                        <option value="savings">Savings</option>
                    </select>
                </label>
            </div>
        </div>

        <div class="grid-x grid-margin-x"
            v-if="payoutMethod == 'Canadian'">
            <!-- Canadian -->
            <label class="cell small-12 medium-4 large-3">
                Institution number

                <input
                    type="text"
                    name="institution-number"
                    placeholder="006"
                    minlength="3"
                    maxlength="3"
                    pattern="\\d{3}"
                    required>
            </label>

            <label class="cell small-12 medium-4 large-3">
                Transit number

                <input
                    type="text"
                    name="institution-number"
                    placeholder="04841"
                    minlength="5"
                    maxlength="5"
                    pattern="\\d{5}"
                    required>
            </label>

            <label class="cell small-12 medium-4 large-3">
                Account Number

                <input
                    type="text"
                    name="account-number"
                    placeholder="1234567"
                    minlength="7"
                    maxlength="12"
                    pattern="\\d{7,12}"
                    required>
            </label>

            <div class="cell small-12 medium-4 large-3">
                <label>
                    Account type

                    <select
                        name="account-type"
                        required>
                        <option value="checking">Checking</option>
                        <option value="savings">Savings</option>
                    </select>
                </label>
            </div>
        </div>

        <div class="grid-x grid-margin-x"
            v-if="payoutMethod == 'Australian'">
            <!-- Australian -->
            <label class="cell small-12 medium-4 large-3">
                BSB code

                <input
                    type="text"
                    name="bsb-code"
                    placeholder="023604"
                    minlength="6"
                    maxlength="6"
                    pattern="^\\d{6}$"
                    required>
            </label>

            <label class="cell small-12 medium-4 large-3">
                Business number (optional)

                <input
                    type="text"
                    name="business-number"
                    placeholder="11 222 333 444"
                    minlength="9"
                    maxlength="14"
                    pattern="^(\\d{2,3}\\s?){3,4}$">
            </label>

            <label class="cell small-12 medium-4 large-3">
                Account Number

                <input
                    type="text"
                    name="account-number"
                    placeholder="123456789"
                    minlength="4"
                    maxlength="9"
                    pattern="^\\d{4,9}$"
                    required>
            </label>
        </div>

        <div class="grid-x grid-margin-x"
            v-if="payoutMethod == 'NewZealand'">
            <!-- New Zealand -->

            <label class="cell small-12 large-6">
                Account Number

                <input
                    type="text"
                    name="account-number"
                    placeholder="03-1587-0050000-00"
                    minlength="15"
                    maxlength="22"
                    pattern="^\\d{2}([- ])?\\d{4}\\1?\\d{7}\\1?\\d{2,3}$"
                    required>
            </label>
        </div>
    </div>
</template>

<script lang="ts">
import * as _ from "lodash";
import Vue from "vue";

import CountrySelect from '../../widgets/CountrySelect.vue'

declare var NC_CONFIG: any;

export default Vue.extend({
    props: {
    },
    data() {
        return {
            country: null,
        };
    },
    computed: {
        payoutMethod() {
            if (this.country) {
                return NC_CONFIG.countries[this.country].payoutMethod;
            } else {
                return null;
            }
        }
    },
    components: { CountrySelect },
});
</script>

<style>

</style>
