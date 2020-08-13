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

  Displays a SVG arrow.
-->

<template>
    <svg
        xmlns="http://www.w3.org/2000/svg"
        width="24" height="24"
        viewBox="0 0 24 24"
        :style="style">

        <path d="M24 12c0-6.627-5.373-12-12-12s-12 5.373-12 12 5.373 12 12 12 12-5.373 12-12zm-18.005-1.568l1.415-1.414 4.59 4.574 4.579-4.574 1.416 1.414-5.995 5.988-6.005-5.988z">
        </path>
    </svg>
</template>

<script lang="ts">
import Vue from "vue";

export default Vue.extend({
    props: {
        direction: { type: String, default: 'bottom' },
    },
    computed: {
        style() {
            switch (this.direction) {
                case 'bottom':
                    return {};
                case 'up':
                    return { transform: 'rotate(180deg)' };
                case 'right':
                    return { transform: 'rotate(-90deg)' };
                case 'left':
                    return { transform: 'rotate(90deg)' };
            }
        }
    }
});
</script>

<style>
</style>
