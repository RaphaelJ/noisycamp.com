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

  Redirects the browser to a Stripe Checkout page.

  It's mandatory to load the Stripe library before-hand:

      <script src="https://js.stripe.com/v3/"></script>
-->

<template>
    <div>
        <p
            v-if="!error"
            class="text-center">
            Redirecting to payment provider...
        </p>

        <p
            v-if="error"
            class="callout error">

            An error occured when redirecting to our payment provider:

            <pre>{{ error.message }}</pre>
        </p>
    </div>
</template>

<script lang="ts">
import Vue from "vue";

declare var NC_CONFIG: any;
declare var Stripe: any;

export default Vue.extend({
    props: {
        sessionId: { type: String, required: true },
    },
    data() {
        return {
            error: null
        }
    },
    mounted() {
        let stripe = Stripe(NC_CONFIG.stripePublicKey);

        stripe.redirectToCheckout({
            sessionId: this.sessionId
        }).then(function (result) {
            console.log(result.error);
            this.error = result.error;
        });
    }
});
</script>

<style>
</style>
