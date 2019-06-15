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

  Provides an input field with an address autocompletion.
-->

<template>
    <div>
        <label :for="'picture_input_' + id" class="button">
            <span v-if="!isUploading">Upload pictures</span>
            <span v-else>Uploading pictures ...</span>
        </label>
        <input type="file" multiple
            :id="'picture_input_' + id"
            :disable="isUploading"
            @change="upload()"
            ref="uploadInput" class="show-for-sr"
            accept="image/*"
            >
    </div>
</template>

<script lang="ts">
import axios from "axios";
import Vue from "vue";

declare var NC_CONFIG: any;
declare var NC_ROUTES: any;

export default Vue.extend({
    props: {
    },
    data() {
        return {
            isUploading: false
        }
    },
    mounted () {
        this.id = this._uid; // Component ID
    },
    methods: {
        upload() {
            let formData = new FormData();
            formData.append('picture', this.$refs.uploadInput.files[0]);

            this.isUploading = true;
            axios.post(
                NC_ROUTES.controllers.PictureController.upload().url,
                formData, {
                    headers: {
                        'Content-Type': 'multipart/form-data',
                        'Csrf-Token': NC_CONFIG.csrfToken,
                    }
                }
            ).then(function (response) {
                console.log(response);
            }).catch(function (error) {
                console.log(error);
            }).then(() => { this.isUploading = false; });
        },
    },
});
</script>

<style>
</style>
