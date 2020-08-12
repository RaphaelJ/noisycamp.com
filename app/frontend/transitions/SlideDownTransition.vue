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
    <transition
        name="slide-down"
        @enter="enter">

        <slot></slot>
    </transition>
</template>

<script lang="ts">
import * as $ from 'jquery'
import Vue from "vue";

export default Vue.extend({
    props: {
        maxHeight: { type: Number, required: false },
    },
    computed: {
        duration() {
            // Duration is 50ms per 100px.
            return this.maxHeight / 100 * 50;
        }
    },
    methods: {
        enter(el, done) {
            let oldStyle = Object.assign({}, el.style);

            el.style.maxHeight = 0;
            el.style.overflow = 'hidden';

            $(el).animate({
                maxHeight: this.maxHeight
            }, this.duration, function () {
                el.style = oldStyle;
                done();
            });
        }
    }
});
</script>

<style>
</style>
