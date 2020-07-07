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

  Provides an set of input widgets for the studio's general information
  (name and description).
-->

<template>
    <div class="grid-x grid-margin-x">
        <div class="cell">
            <label>
                Studio name

                <input
                    type="text"
                    :name="fieldName('name')"
                    :value="fieldValue('name')"
                    required>

                <span v-if="fieldHasError('name')" class="error">
                    {{ fieldError('name') }}
                </span>
            </label>

            <p class="help-text">
                Try to make it as descriptive and short as possible. This is how your studio will
                appear on listings and search engines.
            </p>
        </div>

        <div class="cell">
            <label>
                Short description

                <textarea
                    :name="fieldName('description')"
                    :value="fieldValue('description')"
                    rows="10" required>
                </textarea>

                <span v-if="fieldHasError('description')" class="error">
                    {{ fieldError('description') }}
                </span>
            </label>

            <p class="help-text">
                What makes your studio special. A good description will give you a better visibility
                on search engines like Google.
            </p>
        </div>
    </div>
</template>

<script lang="ts">
import Vue from "vue";

import VueInput from '../../widgets/VueInput';

export default Vue.extend({
    mixins: [VueInput],
});
</script>

<style>
</style>
