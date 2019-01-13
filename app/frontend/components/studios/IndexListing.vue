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
    <div>
        <p class="text-right">
            <small>{{ studios.length }} studio(s) matching</small>
        </p>

        <ul class="grid-x grid-margin-x grid-margin-y studios">
            <li
                class="cell small-12 medium-6 large-4 studio"
                v-for="studio in studios">
                <ul class="pictures">
                    <!-- <li v-for="picture in studio.pictures">
                        <img :src="picture" :alt="studio.name">
                    </li> -->
                    <li>
                        <img :src="studio.pictures[0]" :alt="studio.name">
                    </li>
                </ul>

                <div
                    v-if="studio.instant_booking"
                    class="instant-booking"
                    title="Instant booking availaible for that place">
                    <i class="fi-target"></i>
                </div>

                <div class="details">
                    <span class="name">{{ studio.name }}</span>
                    <span class="price">
                        <strong>{{ renderCurrency(studio.price) }}</strong>
                        per hour
                    </span>
                </div>
            </li>
        </ul>
    </div>
</template>

<script lang="ts">
import Vue from "vue";

export default Vue.extend({
    props: {
        studios: { type: Array, required: true }
    },
    data() {
        return {
            location: null
        }
    },
    methods: {
        renderCurrency(value) {
            return new Intl.NumberFormat(
                navigator.language, { style: 'currency', currency: 'EUR' }
            ).format(value / 100);
        }

    },
    components: { },
});
</script>

<style>
ul.studios, ul.studios li.studio ul.pictures {
    list-style-type: none;
}

ul.studios li.studio {
    position: relative;
    padding: 0;
    height: 75vh;

    box-shadow: 0 0 3px rgba(0, 0, 0, 0.5);
}

ul.studios li.studio,
ul.studios li.studio ul.pictures,
ul.studios li.studio ul.pictures img {
    border-radius: 3px;
}

ul.studios li.studio ul.pictures {
    margin: 0;
}

ul.studios li.studio ul.pictures,
ul.studios li.studio ul.pictures li,
ul.studios li.studio ul.pictures li img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

ul.studios li.studio ul.pictures li img {
    display: block;
}

ul.studios li.studio .instant-booking {
    position: absolute;
    top: 0;
    right: 0;
    padding: 5px 15px;
    margin: 0;

    color: white;
    text-shadow: 0 0 3px rgba(0, 0, 0, 0.8);
    font-size: 1.5rem;

    cursor: help;
}

ul.studios li.studio .details {
    position: absolute;
    top: calc(100% - 45px);
    left: 0;
    height: 45px;
    width: 100%;
    box-sizing: border-box;
    padding-left: 1rem;
    padding-right: 1rem;

    border-top: 2px solid #b37216;
    border-bottom-left-radius: 3px;
    border-bottom-right-radius: 3px;

    background-color: rgba(0, 0, 0, 0.75);
    color: white;
    line-height: 45px;
    font-size: 1.15rem;
}

ul.studios li.studio .details .name {
    float: left;
}

ul.studios li.studio .details .price {
    float: right;
}
</style>
