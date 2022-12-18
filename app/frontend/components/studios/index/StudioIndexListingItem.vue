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
    <div class="grid-y grid-padding-y">
        <div class="cell panel-picture">
            <reactive-picture
                :alt="studio.name + ' picture'"
                :picture-id="studio.picture"
                :width="600"
                :height="400"
                classes="border-radius">
            </reactive-picture>
        </div>

        <div class="cell">
            <div class="grid-y info">
                <h2 class="cell shrink panel-title text-overflow-ellipsis">
                    {{ studio.name }}<br>
                    <small>{{ location }}</small>
                </h2>

                <h3 class="cell shrink panel-subtitle">
                    <small v-if="pricing[1]">Starting at </small>
                    <money-amount :value="pricing[0]">
                    </money-amount>
                    <small>per hour</small>
                </h3>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import Vue from "vue";

import MoneyAmount from '../../widgets/MoneyAmount.vue'
import ReactivePicture from '../../widgets/ReactivePicture.vue';

import { startingPrice } from '../../../misc/MoneyUtils';
import { isWeekend } from "../../../misc/DateUtils";

declare var NC_CONFIG: any;

export default Vue.extend({
    props: {
        studio: { type: Object, required: true },

        bookingDate: { type: String, required: false, default: null },
    },
    computed: {
        location(): string {
            let city = this.studio.location.address.city;
            let countryCode = this.studio.location.address['country-code'];
            let country = NC_CONFIG.countries[countryCode].name;
            return `${city}, ${country}`
        },

        pricing(): [currency, boolean] {
            return startingPrice(this.studio['pricing-policy'], isWeekend(this.bookingDate));
        },
    },
    components: { MoneyAmount, ReactivePicture },
});
</script>

<style>
</style>
