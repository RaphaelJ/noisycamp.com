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
                    v-model="nameValue"
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

        <fieldset class="cell">
            <legend>The studio is suitable for:</legend>

            <div class="grid-x grid-margin-x grid-padding-x">
                <div class="cell small-12 medium-6 large-4">
                    <div class="checkbox-group">
                        <input
                            type="checkbox"
                            :id="fieldName('use-practice')"
                            :name="fieldName('use-practice')"
                            v-model="usePractice"
                            value="true">

                        <label :for="fieldName('use-practice')">Rehearsal/Practice</label>
                    </div>
                </div>

                <div class="cell small-12 medium-6 large-4">
                    <div class="checkbox-group">
                        <input
                            type="checkbox"
                            :id="fieldName('use-recording')"
                            :name="fieldName('use-recording')"
                            v-model="useRecording"
                            value="true">

                        <label :for="fieldName('use-recording')">Recording and/or mastering</label>
                    </div>
                </div>

                <div class="cell small-12 medium-6 large-4">
                    <div class="checkbox-group">
                        <input
                            type="checkbox"
                            :id="fieldName('use-live')"
                            :name="fieldName('use-live')"
                            v-model="useLive"
                            value="true">

                        <label :for="fieldName('use-live')">Live music</label>
                    </div>
                </div>

                <div class="cell small-12 medium-6 large-4">
                    <div class="checkbox-group">
                        <input
                            type="checkbox"
                            :id="fieldName('use-lessons')"
                            :name="fieldName('use-lessons')"
                            v-model="useLessons"
                            value="true">

                        <label :for="fieldName('use-lessons')">Lessons/Learning</label>
                    </div>
                </div>
            </div>
        </fieldset>

        <div class="cell">
            <label>
                Short description

                <textarea
                    :name="fieldName('description')"
                    v-model="description"
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
    props: {
        value: { type: Object, default() { return {}; } }
    },
    data() {
        let data = {
            nameValue: this.value.name,
            description: this.value.description,

            usePractice: false,
            useRecording: false,
            useLive: false,
            useLessons: false,
        };

        if ('use-practice' in this.value) {
            data.usePractice = this.value['use-practice'] == 'true';
        }

        if ('use-recording' in this.value) {
            data.useRecording = this.value['use-recording'] == 'true';
        }

        if ('use-live' in this.value) {
            data.useLive = this.value['use-live'] == 'true';
        }

        if ('use-lessons' in this.value) {
            data.useLessons = this.value['use-lessons'] == 'true';
        }

        return data;
    },
});
</script>

<style>
</style>
