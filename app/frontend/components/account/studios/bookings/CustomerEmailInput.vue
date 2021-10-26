<!--
  Noisycamp is a platform for booking music studios.
  Copyright (C) 2021 Raphael Javaux <raphael@noisycamp.com>

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
    <div class="grid-x grid-padding-x">
        {{ errors }}

        <div class="cell small-12">
            <div class="checkbox-group">
                <input
                    id="has-customer-email"
                    type="checkbox"
                    v-model="hasCustomerEmail"
                    value="true">

                <label for="has-customer-email">
                    Send customer emails on booking updates.
                </label>
            </div>
        </div>

        <slide-down-transition :max-height="85">
            <div
                class="cell small-12 medium-11 medium-offset-1"
                v-if="hasCustomerEmail">
                <label>
                    Customer email

                    <input
                        type="email"
                        :name="fieldName()"
                        :required="hasCustomerEmail"
                        v-model="customerEmail">

                    <span v-if="globalError" class="error">
                        {{ globalError }}
                    </span>
                </label>
            </div>
        </slide-down-transition>
    </div>
</template>

<script lang="ts">
import Vue from "vue";

import VueInput from '../../../widgets/VueInput';
import SlideDownTransition from '../../../../transitions/SlideDownTransition.vue';

declare var NC_CONFIG: any;

export default Vue.extend({
    mixins: [VueInput],
    props: {
        // The customer's email address.
        value: { type: String, required: false },
    },
    data() {
        return {
            hasCustomerEmail: !!this.value,
            customerEmail: this.value,
        }
    },
    watch: {
        hasCustomerEmail(val) {
            if (!val) {
                this.customerEmail = null;
            }
        }
    },
    components: { SlideDownTransition },
});
</script>

<style>
</style>
