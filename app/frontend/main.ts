/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2019  Raphael Javaux <raphaeljavaux@gmail.com>
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

import AddressInput from './components/widgets/AddressInput.vue';
import EquipmentList from './components/studios/create/EquipmentList.vue';
import OpenningTimesInput from './components/studios/create/OpenningTimesInput.vue';
import PayoutInput from './components/studios/create/PayoutInput.vue';
import PictureList from './components/studios/create/PictureList.vue';

import StudiosIndex from './components/studios/Index.vue';

var ncApp = new Vue({
    el: "#nc-app",
    components: {
        AddressInput, EquipmentList, OpenningTimesInput, PayoutInput, PictureList, StudiosIndex,
    },
});
