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


  Provides an input widget to display and add pictures.
-->

<template>
    <div class="grid-x grid-margin-x grid-margin-y">
        <div
            class="cell small-12 callout alert"
            v-if="error">

            <h6>An error occured when uploading the picture</h6>

            <p>{{ error.message }}</p>
        </div>

        <div
            class="cell large-2 medium-3 small-4 picture"
            v-for="(picId, index) in pictures">

            <reactive-picture
                :picture-id="picId" alt="Uploaded picture"
                :width="200" :height="200">
            </reactive-picture>

            <button
                class="close-button picture-remove"
                type="button"
                @click="pictureRemove(index)">
                <span aria-hidden="true">×</span>
            </button>
        </div>

        <div class="cell large-2 medium-3 small-4 picture-add">
            <label :for="'picture_input_' + _uid">
                <p>
                    <i v-if="!isUploading" class="fi-photo"></i>
                    <i v-else class="fi-upload"></i>

                    <br>

                    <span v-if="!isUploading">Add a picture</span>
                    <span v-else>
                        Uploading {{ isUploading }} picture<span v-if="isUploading > 1">s</span>
                        ...
                    </span>
                </p>
            </label>

            <input type="file"
                :id="'picture_input_' + _uid"
                :disable="isUploading >= 1"
                @change="upload()"
                ref="uploadInput" class="show-for-sr"
                accept="image/*"
                multiple>
        </div>
    </div>
</template>

<script lang="ts">
import axios from "axios";
import Vue from "vue";

import ReactivePicture from '../../widgets/ReactivePicture.vue'

declare var NC_CONFIG: any;
declare var NC_ROUTES: any;

export default Vue.extend({
    props: {
    },
    data() {
        return {
            pictures: [],

            error: null,

            // If uploading, contains the number of images currently being uploaded.
            isUploading: false,
        }
    },
    methods: {
        pictureRemove(index: number) {
            this.pictures.splice(index, 1);
        },

        // Uploads all the files in the `uploadInput`.
        upload() {
            this.error = null;

            let pictures = Array.from(this.$refs.uploadInput.files);

            this.uploadPictures(pictures);
        },

        uploadPictures(pictures: File[]) {
            this.isUploading = pictures.length;

            if (pictures.length > 0) {
                let formData = new FormData();
                formData.append('picture', pictures[0]);

                axios.post(
                    NC_ROUTES.controllers.PictureController.upload().url,
                    formData, {
                        headers: {
                            'Content-Type': 'multipart/form-data',
                            'Csrf-Token': NC_CONFIG.csrfToken,
                        }
                    }
                ).then(response => {
                    this.pictures.push(response.data);

                    if (pictures.length > 1) {
                        this.uploadPictures(pictures.slice(1));
                    } else {
                        this.isUploading = false;
                    }
                }).catch(error => {
                    this.error = error;
                    this.isUploading = false
                });
            }
        },
    },
    components: { ReactivePicture }
});
</script>

<style>
.picture img, .picture-add label {
    width: 100%;

    border: 1px solid rgba(0,0,0,0.1);
    border-radius: 2px;
    padding: 4px;
}

.picture {
    position: relative;
}

.picture .picture-remove {
    position: absolute;
    top: 4px;
    right: calc(4px + 0.35rem);
    color: black;

    /* Adds a white border around the button for contrast. */
    text-shadow: -1px 0 rgba(255, 255, 255, 0.5),
                 0    1px rgba(255, 255, 255, 0.5),
                 1px  0 rgba(255, 255, 255, 0.5),
                 0    -1px rgba(255, 255, 255, 0.5);

    opacity: 0.5;
}

.picture .picture-remove:hover {
    opacity: 1;
}

.picture-add {
    position: relative;
}

/* Uses a CSS trick to make the `picture-add` the same height as its width. */
.picture-add:before{
	content: "";
	display: block;
	padding-top: 100%;
}

.picture-add label {
    position: absolute;

	top: 0;
	left: 0;
	bottom: 0;
	right: 0;

    opacity: 0.65;
    cursor: pointer;

    display: flex; /* required to center the icon and text vertucally. */
}

.picture-add label:hover {
    opacity: 1;
}

.picture-add label p {
    margin: auto;

    text-align: center;
    font-size: 1rem;
}

.picture-add label p i {
    font-size: 44px;
}
</style>
