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

  Renders an amount of money.
-->

<template>
    <span class="money-amount">{{ renderedAmount }}</span>
</template>

<script lang="ts">
import Vue from "vue";

import * as currency from 'currency.js';

import { asCurrency } from '../../misc/MoneyUtils';

declare var NC_CONFIG: any;

export default Vue.extend({
    props: {
        // The amount of money, as a currency.js object, or as a
        // { currency, value } object.
        value: { type: Object, required: true },

        // If false, will truncate the decimal part if the decimal value amount
        // to zero.
        showZeroCents: { type: Boolean, required: false, default: false },
    },
    computed: {
        currencyValue() {
            if ('currency' in this.value && 'value' in this.value) {
                return asCurrency(this.value);
            } else {
                return this.value;
            }
        },

        renderedAmount() {
            // Does not show zero decimals if `showZeroCents` is false.
            let hasDecimals = this.currencyValue.cents() > 0;
            let precision = this.showZeroCents || hasDecimals
                ? this.currencyValue.s.precision
                : 0;

            let currencyConfig = {
                formatWithSymbol: true,
                symbol: this.currencyValue.s.symbol,
                precision: precision,
            };

            return currency(this.currencyValue.value, currencyConfig).format();
        }
    }
});
</script>

<style>
</style>
