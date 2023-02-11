<!--
  Noisycamp is a platform for booking music studios.
  Copyright (C) 2020  Raphael Javaux <raphaeljavaux@gmail.com>

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

  Provides a box with a fixed position at the bottom of the page. The box can be used to
  show a set of links to other sections of the page.

  If provided a jQuery DOM selector to the `hide-if-visible` prop, the box will be hidden
  when that component is visible on the user screen.
-->

<template>
    <div
        class="fixed-bottom-box"
        :class="allClasses">
        <slot></slot>
    </div>
</template>

<script lang="ts">
import Vue from "vue";
import * as $ from 'jquery';

export default Vue.extend({
    props: {
        classes: { default() { return [] } },
        hideIfVisible: { type: String },
    },
    data() {
        return {
            isVisible: false,
        }
    },
    computed: {
        hideIfElementVisible() {
            if (this.hideIfVisible) {
                return $(this.hideIfVisible);
            } else {
                return null;
            }
        },

        allClasses() {
            let values = { 'hidden': !this.isVisible };

            if (this.classes) {
                Object.assign(values, this.classes);
            }

            return values;
        },
    },
    mounted() {
        this.isVisible = this.checkIfIsVisible();

        $(window).scroll(() => {
            this.isVisible = this.checkIfIsVisible()
        });
    },
    methods: {
        checkIfIsVisible() {
            if (this.hideIfElementVisible) {
                let el = this.hideIfElementVisible;

                let screenTop = window.pageYOffset;
                var screenBottom = screenTop + window.innerHeight;

                let elTop = el.offset().top;
                let elBottom = elTop + el.height();

                let elIsVisible = screenBottom > elTop && screenTop < elBottom;

                return !elIsVisible;
            } else {
                return true;
            }
        }
    }
});
</script>

<style>
.fixed-bottom-box {
    width: 100%;
    padding: 1rem;

    position: fixed;
    bottom: 0;
    left: 0;
    z-index: 100; /* Required to be on top of some Mapbox layers. */

    background-color: rgba(255, 255, 255, 1);

    border-top: 1px solid rgba(0, 0, 0, 0.06);
    box-shadow: 0 0 12px rgba(0, 0, 0, 0.2);

    transition: transform 0.25s, box-shadow 0.25s;
}

.fixed-bottom-box.hidden {
    /* Makes the box disappear. bellow the screen. */
    transform: translateY(100%);
    box-shadow: unset;
}
</style>
