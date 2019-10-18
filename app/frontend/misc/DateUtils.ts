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

import * as moment from 'moment';

// Renders a Moment duration as a human readable way.
//
// @param precision is one of 'hours', 'minutes', 'seconds'.
export function renderDuration(duration, precision) {
    let components = [];

    function addComponent(value, singular, plural) {
        if (value == 1) {
            components.push(`1 ${singular}`);
        } else if (value > 1) {
            components.push(`${value} ${plural}`);
        }
    }

    addComponent(duration.hours(), 'hour', 'hours');

    if (precision == 'minutes' || precision == 'seconds') {
        addComponent(duration.minutes(), 'minute', 'minutes');
    }

    if (precision == 'seconds') {
        addComponent(duration.seconds(), 'second', 'seconds');
    }

    return components.join(' ');
}

// Returns a new Moment date with the time component changed.
//
// Does not modify the given Moment object.
export function withTimeComponent(date, time) {
    return moment(date.format('YYYY-MM-DD') + 'T' + time);
}
