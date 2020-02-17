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
    <div class="booking-pricing-calculator">
        <div
            class="grid-x grid-margin-x"
            v-if="pricingBreakdown['regular']">

            <div class="cell shrink">
                {{ renderDurationAsHours(pricingBreakdown['regular']['duration']) }} h
                ×
                <money-amount :value="pricingPolicy['price-per-hour']">
                </money-amount>
            </div>
            <div class="cell auto text-right">
                <money-amount :value="pricingBreakdown['regular']['total']">
                </money-amount>
            </div>
        </div>

        <div
            class="grid-x grid-margin-x"
            v-if="pricingBreakdown['evening']">

            <div class="cell shrink">
                {{ renderDurationAsHours(pricingBreakdown['evening']['duration']) }} h
                ×
                <money-amount :value="pricingPolicy['evening']['price-per-hour']">
                </money-amount> (evening)
            </div>
            <div class="cell auto text-right">
                <money-amount :value="pricingBreakdown['evening']['total']">
                </money-amount>
            </div>
        </div>

        <div
            class="grid-x grid-margin-x"
            v-if="pricingBreakdown['weekend']">

            <div class="cell shrink">
                {{ renderDurationAsHours(pricingBreakdown['weekend']['duration']) }} h
                ×
                <money-amount :value="pricingPolicy['weekend']['price-per-hour']">
                </money-amount> (weekend)
            </div>
            <div class="cell auto text-right">
                <money-amount :value="pricingBreakdown['weekend']['total']">
                </money-amount>
            </div>
        </div>

        <div class="grid-x grid-margin-x total">
            <div class="cell shrink">
                <strong>Total</strong>
            </div>
            <div class="cell auto text-right">
                <strong>
                    <money-amount :value="total"></money-amount>
                </strong>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import Vue, { PropOptions } from "vue";

import * as currency from 'currency.js';
import * as moment from 'moment';

import {
    renderDuration, dateComponent, timeComponent, withTimeComponent
} from '../../misc/DateUtils';
import { asCurrency } from '../../misc/MoneyUtils';
import MoneyAmount from '../widgets/MoneyAmount.vue'

export default Vue.extend({
    props: {
        // An array of {is-open, opens-at, closes-at} 7 objects. Starts on Monday.
        openingSchedule: <PropOptions<Object[]>>{ type: Array, required: true },

        pricingPolicy: { type: Object, required: true },

        bookingTimes: { type: Object, required: true },
    },
    data() {
        return {
        }
    },
    computed: {
        // Returns a JavaScript object with three `regular`, `evening` and
        // `weekend` durations in seconds.
        pricingBreakdown() {
            // Collects information about the booked day.

            let beginsAt = moment(this.bookingTimes['begins-at']);

            let beginsAtTime = timeComponent(beginsAt);

            var bookingDate = beginsAt.clone().startOf('day');
            var weekDay = bookingDate.isoWeekday();
            var openingTimes = this.openingSchedule[weekDay - 1];

            // The schedule information might come from the previous day's
            // schedule if the booking starts during the night.

            if (beginsAtTime < openingTimes['opens-at']) {
                bookingDate = bookingDate.clone().subtract(1, 'day');
                weekDay = bookingDate.isoWeekday();
                openingTimes = this.openingSchedule[weekDay - 1];
            }

            let opensAt = openingTimes['opens-at'];
            let closesAt = openingTimes['closes-at'];

            // Computes the durations for the 3 pricing policies.

            let durations = { };

            let isWeekend = weekDay == 6 || weekDay == 7;

            if (isWeekend && this.pricingPolicy['weekend']) {
                durations['regular'] = 0;
                durations['evening'] = 0;
                durations['weekend'] = this.bookingTimes['duration'];
            } else {
                durations['weekend'] = 0;

                let eveningPolicy = this.pricingPolicy['evening'];

                if (eveningPolicy) {
                    let eveningBeginsAt =
                        opensAt <= eveningPolicy['begins-at']
                        ? withTimeComponent(
                            bookingDate, eveningPolicy['begins-at']
                        )
                        : withTimeComponent(
                            bookingDate.clone().add(1, 'days'),
                            eveningPolicy['begins-at']
                        );

                    durations['regular'] = Math.max(
                        0,
                        Math.min(
                            eveningBeginsAt.diff(beginsAt, 'seconds'),
                            this.bookingTimes['duration']
                        )
                    );
                    durations['evening'] =
                        this.bookingTimes['duration']
                        - durations['regular'];
                } else {
                    durations['regular'] = this.bookingTimes.duration;
                    durations['evening'] = 0;
                }
            }

            // Computes the total prices given the durations.

            function toPricingBreakdown(pricePerHour, duration) {
                let total = asCurrency(pricePerHour)
                    .multiply(duration)
                    .divide(3600);

                return {
                    duration: duration,
                    total: total,
                };
            };

            return {
                regular:
                    durations['regular']
                    ? toPricingBreakdown(
                        this.pricingPolicy['price-per-hour'], durations['regular']
                    )
                    : null,
                evening:
                    durations['evening']
                    ? toPricingBreakdown(
                        this.pricingPolicy['evening']['price-per-hour'],
                        durations['evening']
                    )
                    : null,
                weekend:
                    durations['weekend']
                    ? toPricingBreakdown(
                        this.pricingPolicy['weekend']['price-per-hour'],
                        durations['weekend']
                    )
                    : null,
            };
        },

        total() {
            // Creates a 0-valued sum currency.js object with the same currency
            // as the pricing policy.
            let pricePerHour = asCurrency(this.pricingPolicy['price-per-hour'])
            var sum = pricePerHour.subtract(pricePerHour);

            if (this.pricingBreakdown['regular']) {
                sum = sum.add(this.pricingBreakdown['regular']['total']);
            }

            if (this.pricingBreakdown['evening']) {
                sum = sum.add(this.pricingBreakdown['evening']['total']);
            }

            if (this.pricingBreakdown['weekend']) {
                sum = sum.add(this.pricingBreakdown['weekend']['total']);
            }

            return sum;
        }
    },
    methods: {
        // Returns a string representation of the booking duration, as a
        // fraction of hours.
        renderDurationAsHours(duration) {
            let hours = duration / 3600;
            let hoursInt = Math.floor(hours)
            let decimals = hours - hoursInt;

            if (decimals) {
                return hours.toString();
            } else {
                return hoursInt.toString();
            }
        },
    },
    components: { MoneyAmount }
});
</script>

<style>
.booking-pricing-calculator .total {
    font-size: 1.2em;
}
</style>
