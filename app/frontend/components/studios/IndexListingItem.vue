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
    <a
        :href="studio.url"
        target="_blank"
        class="studio panel-section"
        :class="{ 'highlighted': highlighted, }"
        @mouseover="$emit('mouseover')"
        @mouseout="$emit('mouseout')">

        <div class="grid-x grid-padding-x">
            <div class="cell small-12 medium-4 pictures">
                <reactive-picture
                    :alt="studio.name + ' picture'"
                    :picture-id="studio.picture"
                    :width="450"
                    :height="300"
                    classes="border-radius">
                </reactive-picture>
            </div>

            <div class="cell small-12 medium-8">
                <div class="grid-y info">
                    <h5 class="cell shrink text-overflow-ellipsis title">
                        {{ studio.name }}
                    </h5>

                    <div class="cell medium-auto details">
                        <span class="location text-overflow-ellipsis">{{ location }}</span>

                        <span
                            class="schedule"
                            title="Opening schedule">

                            <i class="fi-clock"></i>

                            <span
                                v-for="(day, index) in studio['opening-schedule']"
                                :key="index">

                                <span :class="{ 'is-open': day['is-open'], 'is-closed': !day['is-open'] }">
                                    {{ weekDays[index] }}
                                </span>
                                <span v-if="index < 6">&nbsp;•&nbsp;</span>
                            </span>
                        </span>

                        <span
                            class="equipments text-overflow-ellipsis"
                            title="Equipments and instruments"
                            v-if="equipmentsNames.length > 0">

                            <i class="fi-microphone"></i>

                            <span
                                v-for="(equipmentName, index) in equipmentsNames"
                                :key="index">

                                {{ equipmentName }}
                                <span v-if="index < (equipmentsNames.length - 1)">
                                    &nbsp;•&nbsp;
                                </span>
                            </span>
                        </span>
                    </div>

                    <h5 class="cell shrink pricing">
                        <small v-if="pricing[1]">Starting at </small>
                        <money-amount :value="pricing[0]">
                        </money-amount>
                        <small>per hour</small>
                    </h5>
                </div>
            </div>
        </div>
    </a>
</template>

<script lang="ts">
import Vue, { PropOptions } from "vue";

import * as moment from 'moment';

import MoneyAmount from '../widgets/MoneyAmount.vue'
import ReactivePicture from '../widgets/ReactivePicture.vue';

import { startingPrice } from '../../misc/MoneyUtils';
import { isWeekend } from "../../misc/DateUtils";

declare var NC_CONFIG: any;

export default Vue.extend({
    props: {
        studio: { type: Object, required: true },

        bookingDate: { type: String, required: false, default: null },

        highlighted: { type: Boolean, required: false, default: false },
    },
    computed: {
        weekDays(): string[] {
            return ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
        },

        location(): string {
            let city = this.studio.location.address.city;
            let countryCode = this.studio.location.address['country-code'];
            let country = NC_CONFIG.countries[countryCode].name;
            return `${city}, ${country}`
        },

        // Returns the unique and defined category names of the studio's equipments.
        equipmentsNames(): string[] {
            let nonUniques: string[] = this.studio['equipments']
                .map(e => e.category)
                .filter(e => e)
                .map(e => NC_CONFIG['equipments'][e].name);

            let uniques: string[] = Array.from(new Set(nonUniques));

            return uniques;
        },

        pricing(): [currency, boolean] {
            return startingPrice(this.studio['pricing-policy'], isWeekend(this.bookingDate));
        },
    },
    components: { MoneyAmount, ReactivePicture },
});
</script>

<style scoped>
.studio {
    display: block;
    margin-top: 1rem;

    border: 2px solid transparent;
    transition: border 0.2s ease-out;
}

.studio.highlighted {
    border: 2px solid #b3721675;
}

.studio .title {
    font-weight: bold;
}

.studio .details {
    color: #777;
    font-size: 0.87em;
}

.studio .details span.location {
    margin-top: -0.5rem;
    margin-bottom: 0.75rem;
}

.studio .details span.location,
.studio .details span.schedule,
.studio .details span.equipments {
    display: block;

    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.studio .details span.location .fi-marker,
.studio .details span.schedule .fi-clock,
.studio .details span.equipments .fi-microphone {
    display: inline-block;
    width: 15px;
    text-align: center;
}

.studio .details span.schedule .is-open {
    font-weight: bold;
}

.studio .details span.schedule .is-closed {
    color: #999;
}

@media screen and (min-width: 40em) {
    /* On medium and large screens, the text info is on the right of the picture. */

    .studio .info {
        height: 100%;
        min-height: 135px;
    }

    .studio .pricing {
        text-align: right;
    }
}

@media screen and (max-width: 39.9375em) {
    /* On small screens, the text info is on the bellow the picture. */

    .studio .pictures img {
        width: 100%;
    }

    .studio .title {
        margin-top: 0.5rem;
    }

    .studio .pricing {
        margin-top: 1rem;
        text-align: center;
    }
}
</style>
