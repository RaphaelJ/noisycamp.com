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
        <div class="cell small-12 large-6">
            <label>
                Repeat

                <select
                    :name="fieldName('frequency')"
                    v-model="frequency">
                    <option :value="null">Never</option>
                    <option
                        v-for="o in options"
                        :key="o.value"
                        :value="o.value">
                        {{ o.title }}
                    </option>
                </select>

                <span v-if="errors['frequency']" class="error">
                    {{ errors['frequency'] }}
                </span>
            </label>
        </div>

        <slide-down-transition :max-height="100">
            <fieldset
                class="cell small-12 medium-11 medium-offset-1"
                v-if="frequency">
                <div class="grid-x grid-padding-x">
                    <div class="cell small-12 medium-6">
                        <div class="radio-group">
                            <input
                                type="radio"
                                v-model="repeatType"
                                value="count"
                                id="repeat-type-after">
                            <label for="repeat-type-after">Ends after</label>
                        </div>

                        <label>
                            Times

                            <input
                                type="number"
                                min="1" step="1"
                                :max="mMaxCount"
                                :name="fieldName('count')"
                                v-model="count"
                                :disabled="repeatType != 'count'"
                                :required="repeatType == 'count'">

                            <span v-if="errors['count']" class="error">
                                {{ errors['count'] }}
                            </span>
                        </label>
                    </div>

                    <div class="cell small-12 medium-6">
                        <div class="radio-group">
                            <input
                                type="radio"
                                v-model="repeatType"
                                value="until"
                                id="repeat-type-until">
                            <label for="repeat-type-until">Ends on date</label>
                        </div>

                        <label>
                            End date

                            <input
                                type="date"
                                :name="fieldName('until')"
                                v-model="until"
                                :min="mCurrentTime.format('YYYY-MM-DD')"
                                :max="mMaxUntil.format('YYYY-MM-DD')"
                                pattern="[0-9]{4}-[0-9]{2}-[0-9]{2}"
                                placeholder="yyyy-mm-dd"
                                :disabled="repeatType != 'until'"
                                :required="repeatType == 'until'">

                            <span v-if="errors['until']" class="error">
                                {{ errors['until'] }}
                            </span>
                        </label>
                    </div>
                </div>
            </fieldset>
        </slide-down-transition>

        <div
            class="cell small-12"
            v-if="globalError">
            <label>
                <span class="error">{{ globalError }}</span>
            </label>
        </div>
    </div>
</template>

<script lang="ts">
import Vue from "vue";

import * as moment from 'moment';

import VueInput from '../../../widgets/VueInput';
import SlideDownTransition from '../../../../transitions/SlideDownTransition.vue';

declare var NC_CONFIG: any;

export default Vue.extend({
    mixins: [VueInput],
    props: {
        // The current local time (as a ISO 8601 date string without timezone).
        currentTime: { type: String, required: true },

        // The last repeatable day (exclusive, as a ISO 8601 date string without timezone).
        end: { type: String, required: false },

        // A `frequency`, `count`, `until` object.
        value: {
            type: Object, required: false,
            default: function() {
                return {
                    frequency: null,
                    count: null,
                    until: null,
                }
            }
        },
    },
    data() {
        return {
            options: [
                { value: 'daily', title: 'Every day' },
                { value: 'weekly', title: 'Every week' },
                { value: 'monthly', title: 'Every month' },
                { value: 'yearly', title: 'Every year' },
            ],

            frequency: this.value.frequency ? this.value.frequency : null,
            count: this.value.count ? this.value.count : null,
            until: this.value.until ? this.value.until : null,

            repeatType: this.value.until ? 'until' : 'count',
        }
    },
    computed: {
        mCurrentTime() {
            return moment(this.currentTime);
        },

        mEnd() {
            if (this.end) {
                return moment(this.end);
            } else {
                return null;
            }
        },

        // The max (inclusive) value to be passed to the `<input type="number">` widget.
        mMaxCount() {
            if (this.mMaxUntil && this.frequency) {
                let unit = {
                    'daily': 'days',
                    'weekly': 'weeks',
                    'monthly': 'months',
                    'yearly': 'years'
                }[this.frequency];

                return this.mEnd.
                    diff(this.mCurrentTime.clone().startOf('day'), unit);
            } else {
                return null;
            }
        },

        // The max (inclusive) date value to be passed to the `<input type="date">` widget.
        mMaxUntil() {
            if (this.mEnd) {
                return this.mEnd.clone().subtract(1, 'days');
            } else {
                return null;
            }
        },
    },
    methods: {
    },
    components: { SlideDownTransition },
});
</script>

<style>
</style>
