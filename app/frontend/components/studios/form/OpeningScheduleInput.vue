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

  Provides an set of input widgets for the studio's opening time table.
-->

<template>
    <div class="grid-x grid-margin-x grid-padding-x grid-margin-y">
        <div
            v-for="(weekDay, dayIx) in weekDays"
            class="cell small-12 medium-6 large-4">

            <div class="grid-x grid-margin-x">
                <div class="cell small-12">
                    <input
                        :id="'opening-times-' + weekDay.toLowerCase() + '-is-open'"
                        :name="fieldName(weekDay.toLowerCase() + '.is-open')"
                        type="checkbox"
                        value="true"
                        v-model="schedule[dayIx].isOpen">

                    <label :for="'opening-times-' + weekDay.toLowerCase() + '-is-open'">
                        <h5>{{ weekDay }}</h5>

                        <span
                            v-if="fieldHasError(weekDay.toLowerCase())"
                            class="error">
                            {{ fieldError(weekDay.toLowerCase()) }}
                        </span>

                        <span
                            v-if="fieldHasError(weekDay.toLowerCase() + '.is-open')"
                            class="error">
                            {{ fieldError(weekDay.toLowerCase() + '.is-open') }}
                        </span>
                    </label>
                </div>

                <div class="cell small-6">
                    <label>
                        Opens at

                        <input type="time"
                            :name="fieldName(weekDay.toLowerCase() + '.opens-at')"
                            v-model="schedule[dayIx].opensAt"
                            :disabled="!schedule[dayIx].isOpen"
                            :required="schedule[dayIx].isOpen"
                            pattern="[0-9]{2}:[0-9]{2}">

                        <span
                            v-if="fieldHasError(weekDay.toLowerCase() + '.opens-at')"
                            class="error">
                            {{ fieldError(weekDay.toLowerCase() + '.opens-at') }}
                        </span>
                    </label>
                </div>

                <div class="cell small-6">
                    <label>
                        Closes at

                        <input type="time"
                            :name="fieldName(weekDay.toLowerCase() + '.closes-at')"
                            v-model="schedule[dayIx].closesAt"
                            :disabled="!schedule[dayIx].isOpen"
                            :required="schedule[dayIx].isOpen"
                            pattern="[0-9]{2}:[0-9]{2}">

                        <span
                            v-if="fieldHasError(weekDay.toLowerCase() + '.closes-at')"
                            class="error">
                            {{ fieldError(weekDay.toLowerCase() + '.closes-at') }}
                        </span>
                    </label>
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import Vue from "vue";

import VueInput from '../../widgets/VueInput';

export default Vue.extend({
    mixins: [VueInput],
    props: {
        value: { type: Object, default() { return {}; } }
    },
    data() {
        let weekDays = [
            'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'
        ];

        return {
            weekDays: weekDays,

            schedule: weekDays.map((day) => {
                let lcDay = day.toLowerCase();

                if (lcDay in this.value) {
                    let dayValue = this.value[lcDay];
                    return {
                        isOpen: dayValue["is-open"] == "true",
                        opensAt: dayValue["opens-at"],
                        closesAt:  dayValue["closes-at"],
                    };
                } else {
                    return {
                        isOpen: false,
                        opensAt: '08:00',
                        closesAt: '20:00',
                    };
                }
            }),
        };
    },
});
</script>

<style>
</style>
