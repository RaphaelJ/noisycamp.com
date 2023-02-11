<!--
  Noisycamp is a platform for booking music studios.
  Copyright (C) 2023  Raphael Javaux <raphael@noisycamp.com>

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
        v-if="type == 'link'"
        :class="classes"
        :href="target"
        @click="doAction">
        <slot></slot>
    </a>
    <button
        v-else
        :class="classes"
        @click="doAction">
        <slot></slot>
    </button>
</template>

<script lang="ts">
import Vue from "vue";

import * as $ from "jquery";

export default Vue.extend({
    props: {
        action: { type: String, required: true }, // One of [scroll-to, click]
        target: { type: String, required: true },
        type: { default: "link" }, // Either button or link
        classes: { type: String, default: "" },
    },
    methods: {
        doAction(event) {
            let actions = {
                "click": this.click,
                "scroll-to": this.scrollTo,
            }

            actions[this.action]();

            event.preventDefault();
        },

        click() {
            $(this.target).trigger("click");
        },

        scrollTo() {
            $('html, body').animate({ scrollTop: $(this.target).offset().top }, 500);
        },

    }
});
</script>

<style>
</style>
