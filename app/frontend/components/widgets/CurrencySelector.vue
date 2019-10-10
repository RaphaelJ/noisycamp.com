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

  Provides a widget to select a currency.
-->

<template>
    <select
        v-model="currency"
        @change="setCurrency(currency)">
        <option disabled value="">Please select a currency</option>
        <option
            v-for="c in orderedCurrencies"
            :value="c.isoCode"
            @selected>
            {{ c.name }} - {{ c.symbol }}
        </option>
    </select>
</template>

<script lang="ts">
import * as _ from "lodash";
import Vue from "vue";

declare var NC_CONFIG: any;
declare var NC_ROUTES: any;

export default Vue.extend({
    props: {
        value: { type: String, required: false }, // Currency ISO code.
    },
    data() {
        return {
            currency: this.value,
        }
    },
    computed: {
        // Available currencies, sorted by name.
        orderedCurrencies() {
            return Object.values(NC_CONFIG.currencies)
                .sort((lhs: any, rhs: any) => lhs.name.localeCompare(rhs.name));
        },
    },
    methods: {
        setCurrency(value) {
            if (!value) {
                return;
            }

            let url = NC_ROUTES.controllers.i18n.Currency.set(
                value, NC_CONFIG.currentUri
            ).url;

            window.location.href = url;
        }
    }
});
</script>

<style>
</style>
