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

  Provides an input widget for money amounts.
-->

<template>
    <div class="input-group">
        <span class="input-group-label" :title="currencyObj.name">
            {{ currencyObj.symbol }}
        </span>
        <input
            class="input-group-field"
            :name="name"
            type="number"
            min="0"
            :step="currencyStep"
            :placeholder="placeholder"
            :value="value"
            :disabled="disabled"
            :required="required"
            @input="$emit('input', $event.target.value)">
    </div>
</template>

<script lang="ts">
import Vue from "vue";

import VueInput from './VueInput';

declare var NC_CONFIG: any;

export default Vue.extend({
    mixins: [VueInput],
    props: {
        currency: { type: String, required: false, default: 'EUR' },

        value: { type: String, required: false },

        disabled: { type: Boolean, required: false, default: false },
        required: { type: Boolean, required: false, default: false },
    },
    data() {
        return {
            amount: this.value,
        };
    },
    computed: {
        currencyObj() {
            return NC_CONFIG.currencies[this.currency];
        },

        currencyStep() {
            return Math.pow(10, -this.currencyObj.decimals);
        },

        placeholder() {
            if (this.currencyObj.decimals > 0) {
                return '25.' + '0'.repeat(this.currencyObj.decimals);
            } else {
                return '25';
            }
        }
    },
});
</script>

<style>
</style>
