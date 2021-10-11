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
    <div class="grid-y">
        <div
            class="cell"
            v-if="hasCountrySelector">
            <label>
                Country

                <country-select
                    :name="fieldName('country')"
                    v-model="address['country']"
                    required>
                </country-select>

                <span v-if="errors['country']" class="error">
                    {{ errors['country'] }}
                </span>
            </label>
        </div>

        <input
            v-if="!hasCountrySelector"
            type="hidden"
            :name="fieldName('country')"
            v-model="address['country']">

        <div class="cell">
            <label>
                Street address

                <input
                    type="text"
                    :name="fieldName('address-1')"
                    v-model="address['address-1']"
                    :disabled="!address.country"
                    required>

                <span v-if="errors['address-1']" class="error">
                    {{ errors['address-1'] }}
                </span>
            </label>
        </div>

        <div
            class="cell"
            v-if="hasAddress2Input">
            <label>
                Street address (line 2)

                <input
                    type="text"
                    :name="fieldName('address-2')"
                    v-model="address['address-2']"
                    :disabled="!address['country']">

                    <span v-if="errors['address-2']" class="error">
                        {{ errors['address-2'] }}
                    </span>
            </label>
        </div>

        <div class="cell">
            <div class="grid-x grid-margin-x">
                <div class="cell medium-4">
                    <label>
                        City

                        <input
                            type="text"
                            :name="fieldName('city')"
                            v-model="address['city']"
                            :disabled="!address['country']"
                            required>

                        <span v-if="errors['city']" class="error">
                            {{ errors['city'] }}
                        </span>
                    </label>
                </div>

                <div class="cell medium-3">
                    <label>
                        Zipcode

                        <input
                            type="text"
                            :name="fieldName('zipcode')"
                            v-model="address['zipcode']"
                            :disabled="!address['country'] || !hasZipCode"
                            :required="hasZipCode">

                        <span v-if="errors['zipcode']" class="error">
                            {{ errors['zipcode'] }}
                        </span>
                    </label>
                </div>

                <div class="cell medium-5">
                    <label>
                        State/Province

                        <select
                            :name="fieldName('state')"
                            v-model="address['state']"
                            :disabled="!hasStates"
                            :required="hasStates">
                            <option
                                v-if="hasStates"
                                value="" disabled>
                                Please select a state or province
                            </option>
                            <option
                                v-for="state in orderedStates"
                                :key="state.code"
                                :value="state.code">
                                {{ state.name }}
                            </option>
                        </select>

                        <span v-if="errors['state']" class="error">
                            {{ errors['state'] }}
                        </span>
                    </label>
                </div>
            </div>
        </div>

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
import CountrySelect from '../../../widgets/CountrySelect.vue';

declare var NC_CONFIG: any;

export default Vue.extend({
    mixins: [VueInput],
    props: {
        value: {
            type: Object,
            default() {
                return {
                    country: null, 'address-1': null, 'address-2': null, city: null,
                    zipcode: null, state: null
                }
            }
        },

        // The 2-letters ISO country code. If specified, the input will not
        // display a country select field.
        country: { type: String, required: false },

        hasAddress2Input: { type: Boolean, default: true },
    },
    data() {
        return {
            address: {
                country: this.country ? this.country : this.value['country'],
                'address-1': this.value['address-1'],
                'address-2': this.value['address-2'],
                city: this.value['city'],
                zipcode: this.value['zipcode'],
                state: this.value['state'],
            },
        }
    },
    computed: {
        hasCountrySelector() {
            return !this.country;
        },

        countryValue() {
            return this.country ? this.country : this.value['country'];
        },

        hasStates() {
            return this.countryValue && NC_CONFIG.countries[this.countryValue].states;
        },

        hasZipCode() {
            return this.countryValue && NC_CONFIG.countries[this.countryValue].hasZipCode;
        },

        // Returns the list of states/provinces of the currently selected
        // country, if it has any.
        states() {
            if (this.hasStates) {
                return NC_CONFIG.countries[this.countryValue].states;
            } else {
                return null;
            }
        },

        // Available countries, sorted by name.
        orderedCountries() {
            return Object.values(NC_CONFIG.countries)
                .sort((lhs: any, rhs: any) => lhs.name.localeCompare(rhs.name));
        },

        // States of the currently selected country, sorted by name.
        orderedStates() {
            if (this.hasStates) {
                return Object.keys(this.states)
                    .map(code => ({ code: code, name: this.states[code] }))
                    .sort((lhs: any, rhs: any) =>
                        lhs.name.localeCompare(rhs.name));
            } else {
                return null;
            }
        },
    },
    watch: {
        'address': {
            deep: true,
            handler() {
                this.$emit('input', this.address);
            }
        },

        'address.country': function() {
            // Resets the state field on country change.
            this.address['state'] = null;
        }
    },
    components: { CountrySelect },
});
</script>

<style>
</style>
