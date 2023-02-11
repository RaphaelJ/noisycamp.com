<!--
  Noisycamp is a platform for booking music studios.
  Copyright (C) 2022  Raphael Javaux <raphaeljavaux@gmail.com>

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

  Provides a burger button that set the target component class "open" when clicked.
-->

<template>
    <button
        :class="buttonClasses"
        ref="button"
        @click="click()"
        aria-label="Open/Close menu">
        <div class="burger-button-bar"></div>
        <div class="burger-button-bar"></div>
        <div class="burger-button-bar"></div>
    </button>
</template>

<script lang="ts">
import Vue from "vue";
import * as $ from 'jquery';

export default Vue.extend({
    props: {
        target: { type: String },
    },
    data() {
        return {
            isOpen: false,
        }
    },
    mounted() {
        this.setTargetClass();
    },
    computed: {
        buttonClasses() {
            let statusClass = this.isOpen ? 'open' : 'closed';
            return ['burger-button', statusClass];
        }
    },
    methods: {
        click() {
            this.isOpen = !this.isOpen;

            this.setTargetClass();
        },

        setTargetClass() {
            if (this.isOpen) {
                $(this.target).removeClass("closed");
                $(this.target).addClass("open");
            } else {
                $(this.target).removeClass("open");
                $(this.target).addClass("closed");
            }
        },
    }
});
</script>

<style>
.burger-button {
    --button-button-width: 1.25rem;
    --burger-button-padding: 0.9rem;

    --burger-button-margin: calc(
        (
            var(--top-bar-line-height)
            - var(--burger-button-padding) * 2
            - var(--burger-button-border-size) * 3
        ) / 2
    );

    --burger-button-height: calc(var(--burger-button-margin) * 2);

    --burger-button-border-size: 1px;

    padding: var(--burger-button-padding);
}

.burger-button .burger-button-bar {
    border-bottom: 1px solid rgba(255, 255, 255, 1);
    width: var(--button-button-width);

    transition: opacity 0.2s ease-in-out, transform 0.2s ease-in-out;
}

.burger-button .burger-button-bar:not(:first-child) {
    margin-top: var(--burger-button-margin);
}

.burger-button.open .burger-button-bar:not(:first-child):not(:last-child) {
    opacity: 0;
}

.burger-button.open .burger-button-bar:first-child {
    transform:
        translateY(calc(var(--burger-button-height) / 2 + var(--burger-button-border-size)))
        rotate(45deg);
}

.burger-button.open .burger-button-bar:last-child {
    transform:
        translateY(calc(var(--burger-button-height) / -2 - var(--burger-button-border-size)))
        rotate(-45deg);

}
</style>
