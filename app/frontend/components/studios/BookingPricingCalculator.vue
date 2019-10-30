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
    <div class="grid-x grid-margin-x">
        <div
            class="cell small-6"
            v-if="pricingBreakdown['regular']">

            {{ renderDurationAsHours(pricingBreakdown['regular']['duration']) }} h
            ×
            <money-amount
                :value="pricingPolicy['price-per-hour']"
                currency="EUR">
            </money-amount>
        </div>
        <div
            class="cell small-6 text-right"
            v-if="pricingBreakdown['regular']">

            <money-amount
                :value="pricingBreakdown['regular']['total'].format()"
                currency="EUR">
            </money-amount>
        </div>

        <div
            class="cell small-6"
            v-if="pricingBreakdown['evening']">

            {{ renderDurationAsHours(pricingBreakdown['evening']['duration']) }} h
            ×
            <money-amount
                :value="pricingPolicy['evening']['price-per-hour']"
                currency="EUR">
            </money-amount> (evening)
        </div>
        <div
            class="cell small-6 text-right"
            v-if="pricingBreakdown['evening']">

            <money-amount
                :value="pricingBreakdown['evening']['total'].format()"
                currency="EUR">
            </money-amount>
        </div>

        <div
            class="cell small-6"
            v-if="pricingBreakdown['weekend']">

            {{ renderDurationAsHours(pricingBreakdown['weekend']['duration']) }} h
            ×
            <money-amount
                :value="pricingPolicy['weekend']['price-per-hour']"
                currency="EUR">
            </money-amount> (weekend)
        </div>
        <div
            class="cell small-6 text-right"
            v-if="pricingBreakdown['weekend']">

            <money-amount
                :value="pricingBreakdown['weekend']['total'].format()"
                currency="EUR">
            </money-amount>
        </div>

        <div class="cell small-6">
            <strong>Total</strong>
        </div>
        <div class="cell small-6 text-right">
            <strong>
                <money-amount
                    :value="total.format()"
                    currency="EUR">
                </money-amount>
            </strong>
        </div>
    </div>
</template>

<script lang="ts">
import Vue, { PropOptions } from "vue";

import * as currency from 'currency.js';
import * as moment from 'moment';

import { renderDuration, withTimeComponent } from '../../misc/DateUtils';
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

            let bookingDate = moment(this.bookingTimes['date']);
            let weekDay = bookingDate.isoWeekday();
            let opensAt = this.openingSchedule[weekDay]['opens-at'];
            // The moment's instance the booking starts.
            let bookingStartAt =
                this.bookingTimes.time < opensAt
                ? withTimeComponent(bookingDate, this.bookingTimes['time'])
                : withTimeComponent(
                    bookingDate.clone().add(1, 'days'),
                    this.bookingTimes['time']
                );

            // Computes the durations for the 3 pricing policies.

            let durations = { };

            let isWeekend =
                bookingDate.isoWeekday() == 6
                || bookingDate.isoWeekday() == 7;

            if (isWeekend && this.pricingPolicy['weekend']) {
                durations['regular'] = 0;
                durations['evening'] = 0;
                durations['weekend'] = this.bookingTimes['duration'];
            } else {
                durations['weekend'] = 0;

                if (this.pricingPolicy['evening']) {
                    let eveningPolicy = this.pricingPolicy['evening'];
                    let eveningBeginsAt =
                        eveningPolicy['begins-at'] < opensAt
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
                            eveningBeginsAt.diff(bookingStartAt) / 1000,
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
                let total = currency(pricePerHour)
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
            var sum = currency(0);

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
</style>
