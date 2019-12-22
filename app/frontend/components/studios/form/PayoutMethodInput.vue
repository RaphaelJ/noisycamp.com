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

  Provides an set of input widget for the payout method.
-->

<template>
    <div>
        <div class="grid-x grid-margin-x">
            <div class="cell callout alert" v-if="fieldHasError('payout-method')">
                {{ fieldError('payout-method') }}
            </div>

            <label class="cell small-12 medium-6">
                Billing country

                <country-select
                    :name="fieldName('country')"
                    v-model="country">
                </country-select>

                <span v-if="fieldHasError('country')" class="error">
                    {{ fieldError('country') }}
                </span>
            </label>
        </div>

        <div class="grid-x grid-margin-x">
            <label class="cell small-12 medium-6">
                Recipient type

                <select
                    :name="fieldName('recipient-type')"
                    required>
                    <option value="business">Business</option>
                    <option value="private">Private/Personal</option>
                </select>

                <span v-if="fieldHasError('recipient-type')" class="error">
                    {{ fieldError('recipient-type') }}
                </span>
            </label>

            <label class="cell small-12 medium-6">
                Recipient's name

                <input
                    type="text"
                    :name="fieldName('recipient-name')"
                    required>

                <span v-if="fieldHasError('recipient-name')" class="error">
                    {{ fieldError('recipient-name') }}
                </span>
            </label>
        </div>

        <div class="grid-x grid-margin-x"
            v-if="accountType == 'IBAN'">
            <!-- IBAN -->
            <label class="cell small-12 medium-8">
                IBAN

                <input
                    type="text"
                    :name="fieldName('iban.iban')"
                    placeholder="DE89370400440532013000"
                    minlength="2"
                    required>

                <span v-if="fieldHasError('iban.iban')" class="error">
                    {{ fieldError('iban.iban') }}
                </span>
            </label>

            <label class="cell small-12 medium-4">
                BIC/SWIFT (optional)

                <input
                    type="text"
                    :name="fieldName('iban.bic')"
                    placeholder="GKCCBEBB"
                    pattern="(?i)[A-Z]{6}[A-Z\d]{2}([A-Z\d]{3})?">

                <span v-if="fieldHasError('iban.bic')" class="error">
                    {{ fieldError('iban.bic') }}
                </span>
            </label>
        </div>

        <div class="grid-x grid-margin-x"
            v-if="accountType == 'ABA'">
            <!-- ABA -->
            <div class="cell">
                <address-input
                    :name="fieldName('aba-account.address')"
                    :country="country"
                    :has-address-2-input="false">
                </address-input>
            </div>

            <label class="cell small-12 medium-6">
                Routing number

                <input
                    type="text"
                    :name="fieldName('aba-account.routing-number')"
                    placeholder="111000025"
                    minlength="9"
                    maxlength="9"
                    pattern="^\d{9}$"
                    required>

                <span v-if="fieldHasError('aba-account.routing-number')" class="error">
                    {{ fieldError('aba-account.routing-number') }}
                </span>
            </label>

            <label class="cell small-12 medium-6">
                Account Number

                <input
                    type="text"
                    :name="fieldName('aba-account.account-number')"
                    placeholder="12345678"
                    minlength="4"
                    maxlength="17"
                    pattern="^\d{4,17}$"
                    required>

                <span v-if="fieldHasError('aba-account.account-number')" class="error">
                    {{ fieldError('aba-account.account-number') }}
                </span>
            </label>

            <div class="cell small-12 medium-6">
                <label>
                    Account type

                    <select
                        :name="fieldName('aba-account.account-type')"
                        required>
                        <option value="checking">Checking</option>
                        <option value="savings">Savings</option>
                    </select>

                    <span v-if="fieldHasError('aba-account.account-type')" class="error">
                        {{ fieldError('aba-account.account-type') }}
                    </span>
                </label>
            </div>
        </div>

        <div class="grid-x grid-margin-x"
            v-if="accountType == 'Canadian'">
            <!-- Canadian -->
            <label class="cell small-12 medium-6">
                Institution number

                <input
                    type="text"
                    :name="fieldName('canadian-account.institution-number')"
                    placeholder="006"
                    minlength="3"
                    maxlength="3"
                    pattern="\d{3}"
                    required>

                <span v-if="fieldHasError('canadian-account.institution-number')" class="error">
                    {{ fieldError('canadian-account.institution-number') }}
                </span>
            </label>

            <label class="cell small-12 medium-6">
                Transit number

                <input
                    type="text"
                    :name="fieldName('canadian-account.transit-number')"
                    placeholder="04841"
                    minlength="5"
                    maxlength="5"
                    pattern="\d{5}"
                    required>

                <span v-if="fieldHasError('canadian-account.transit-number')" class="error">
                    {{ fieldError('canadian-account.transit-number') }}
                </span>
            </label>

            <label class="cell small-12 medium-6">
                Account Number

                <input
                    type="text"
                    :name="fieldName('canadian-account.account-number')"
                    name="account-number"
                    placeholder="1234567"
                    minlength="7"
                    maxlength="12"
                    pattern="\d{7,12}"
                    required>

                <span v-if="fieldHasError('canadian-account.account-number')" class="error">
                    {{ fieldError('canadian-account.account-number') }}
                </span>
            </label>

            <div class="cell small-12 medium-6">
                <label>
                    Account type

                    <select
                        :name="fieldName('canadian-account.account-type')"
                        required>
                        <option value="checking">Checking</option>
                        <option value="savings">Savings</option>
                    </select>

                    <span v-if="fieldHasError('canadian-account.account-type')" class="error">
                        {{ fieldError('canadian-account.account-type') }}
                    </span>
                </label>
            </div>
        </div>

        <div class="grid-x grid-margin-x"
            v-if="accountType == 'Australian'">
            <!-- Australian -->
            <div class="cell">
                <address-input
                    :name="fieldName('australian-account.address')"
                    :country="country"
                    :has-address-2-input="false">
                </address-input>
            </div>

            <label class="cell small-12 medium-6">
                BSB code

                <input
                    type="text"
                    :name="fieldName('australian-account.bsb-code')"
                    placeholder="023604"
                    minlength="6"
                    maxlength="6"
                    pattern="^\d{6}$"
                    required>

                <span v-if="fieldHasError('australian-account.bsb-code')" class="error">
                    {{ fieldError('australian-account.bsb-code') }}
                </span>
            </label>

            <label class="cell small-12 medium-6">
                Business number (optional)

                <input
                    type="text"
                    :name="fieldName('australian-account.business-number')"
                    placeholder="11 222 333 444"
                    minlength="9"
                    maxlength="14"
                    pattern="^(\d{2,3}\s?){3,4}$">

                <span v-if="fieldHasError('australian-account.business-number')" class="error">
                    {{ fieldError('australian-account.business-number') }}
                </span>
            </label>

            <label class="cell small-12 medium-6">
                Account Number

                <input
                    type="text"
                    :name="fieldName('australian-account.account-number')"
                    placeholder="123456789"
                    minlength="4"
                    maxlength="9"
                    pattern="^\d{4,9}$"
                    required>

                <span v-if="fieldHasError('australian-account.account-number')" class="error">
                    {{ fieldError('australian-account.account-number') }}
                </span>
            </label>
        </div>

        <div class="grid-x grid-margin-x"
            v-if="accountType == 'NewZealand'">
            <!-- New Zealand -->

            <label class="cell small-12 large-12">
                Account Number

                <input
                    type="text"
                    name="account-number"
                    :name="fieldName('new-zealand-account.account-number')"
                    placeholder="03-1587-0050000-00"
                    minlength="15"
                    maxlength="22"
                    pattern="^\d{2}([- ])?\d{4}\1?\d{7}\1?\d{2,3}$"
                    required>

                <span v-if="fieldHasError('new-zealand-account.account-number')" class="error">
                    {{ fieldError('new-zealand-account.account-number') }}
                </span>
            </label>
        </div>
    </div>
</template>

<script lang="ts">
import * as _ from "lodash";
import Vue from "vue";

import VueInput from '../../widgets/VueInput';
import CountrySelect from '../../widgets/CountrySelect.vue'

import AddressInput from './AddressInput.vue';

declare var NC_CONFIG: any;

export default Vue.extend({
    mixins: [VueInput],
    props: {
    },
    data() {
        return {
            country: null,

            address: null,
        };
    },
    computed: {
        accountType() {
            if (this.country) {
                return NC_CONFIG.countries[this.country].accountType;
            } else {
                return null;
            }
        },

        requireAddress() {
            return this.accountType == 'ABA' || this.accountType == 'Australian';
        },
    },
    components: { AddressInput, CountrySelect },
});
</script>

<style>

</style>
