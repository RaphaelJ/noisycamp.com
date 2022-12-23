/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2022 Raphael Javaux <raphael@noisycamp.com>
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

import * as NoisyCamp from '/entry_points/main.ts'
export * from '/entry_points/main.ts'

import BookingForm from '/components/studios/booking/BookingForm.vue';
import Calendar from '/components/widgets/Calendar.vue';

Object.assign(NoisyCamp.components,{BookingForm, Calendar});
