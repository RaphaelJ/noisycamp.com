/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2019-2021 Raphael Javaux <raphael@noisycamp.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import Vue from 'vue';

import Calendar from './components/calendar/Calendar.vue';

import Arrow from './components/widgets/Arrow.vue';
import BurgerButton from './components/widgets/BurgerButton.vue';
import LocationMap from './components/widgets/LocationMap.vue';
import FixedBottomBox from './components/widgets/FixedBottomBox.vue';
import PictureCarousel from './components/widgets/PictureCarousel.vue';
import ReactivePicture from './components/widgets/ReactivePicture.vue';
import StripeCheckoutRedirect from './components/widgets/StripeCheckoutRedirect.vue';

import SearchForm from './components/index/SearchForm.vue';

import StudiosIndex from './components/studios/index/StudioIndex.vue';

import BookingForm from './components/studios/booking/BookingForm.vue';
import BookingPricingCalculator from './components/studios/booking/BookingPricingCalculator.vue';
import BookingRepeatInput from './components/account/studios/bookings/BookingRepeatInput.vue';
import CustomerEmailInput from './components/account/studios/bookings/CustomerEmailInput.vue';
import BookingReviewForm from './components/studios/booking/BookingReviewForm.vue';
import BookingTimesInput from './components/studios/booking/BookingTimesInput.vue';

import StudioCreate from './components/account/studios/create/StudioCreate.vue';
import StudioForm from './components/account/studios/create/StudioForm.vue';

import SlideDownTransition from "./transitions/SlideDownTransition.vue";

new Vue({
    el: "#nc-app",
    components: {
        Calendar,
        Arrow, BurgerButton, LocationMap, FixedBottomBox, PictureCarousel, ReactivePicture,
        StripeCheckoutRedirect,
        SearchForm,
        StudiosIndex,
        BookingForm, BookingPricingCalculator, BookingRepeatInput, CustomerEmailInput,
        BookingReviewForm, BookingTimesInput,
        StudioCreate, StudioForm,
    },
    transitions: { SlideDownTransition, },
});
