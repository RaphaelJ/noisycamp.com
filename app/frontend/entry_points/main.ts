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

import ActionButton from '/components/widgets/ActionButton.vue';
import Arrow from '/components/widgets/Arrow.vue';
import BurgerButton from '/components/widgets/BurgerButton.vue';
import FixedBottomBox from '/components/widgets/FixedBottomBox.vue';

export const components = {ActionButton, Arrow, BurgerButton, FixedBottomBox}
export const transitions = {}

export function createApp() {
    return new Vue({
        el: "#nc-app",
        components: components,
        transitions: transitions,
    })
}
