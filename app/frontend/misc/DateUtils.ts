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

    addComponent(Math.floor(duration.asHours()), 'hour', 'hours');

    if (precision == 'minutes' || precision == 'seconds') {
        addComponent(duration.minutes(), 'minute', 'minutes');
    }

    if (precision == 'seconds') {
        addComponent(duration.seconds(), 'second', 'seconds');
    }

    return components.join(' ');
}

// Returns the date component of a moment datetime object as a ISO 8601 time
// string.
export function dateComponent(datetime) {
    return datetime.format('YYYY-MM-DD');
}

// Returns the time component of a moment datetime object as a ISO 8601 time
// string.
export function timeComponent(datetime, precision: string = 'millisecs') {
    var format: string;

    switch (precision) {
    case 'millisecs':
        format = 'HH:mm:ss.SSS';
        break;
    case 'seconds':
        format = 'HH:mm:ss';
        break;
    case 'minutes':
    default:
        format = 'HH:mm';
        break;
    }

    return datetime.format(format);
}

// Returns a new Moment date with the time component changed.
//
// Does not modify the given Moment object.
export function withTimeComponent(date, time) {
    return moment(date.format('YYYY-MM-DD') + 'T' + time);
}

// Returns true if the given date is a Saturday or a Sunday.
export function isWeekend(date): boolean {
    if (date != null) {
        let weekday: number = moment(date).isoWeekday();
        return weekday == 6 || weekday == 7;
    } else {
        return null;
    }
}

export interface EventTimes {
    beginsAt: moment.Moment
    endsAt?: moment.Moment
    duration?: moment.Duration
}

export interface Event {
    title?: string
    href?: string
    classes: Array<string>
    times: EventTimes
}

// Returns true if there is an overlap between the two events defined by their [begin; end[ MomentJS
// intervals.
export function eventsOverlap(
    beginsAt1: moment.Moment, endsAt1: moment.Moment,
    beginsAt2: moment.Moment, endsAt2: moment.Moment): boolean {
    return endsAt1.isAfter(beginsAt2) && beginsAt1.isBefore(endsAt2);
}

// Sorts events by their `begins-at` value.
export function eventsSort(events: Array<Event>): Array<Event> {
    return events.
        slice().
        sort((e1: Event, e2: Event) => {
            if (e1.times.beginsAt.isSame(e2.times.beginsAt)) {
                return 0;
            } else if (e1.times.beginsAt.isBefore(e2.times.beginsAt)) {
                return -1;
            } else {
                return 1;
            }
        });
}

// Combines overlapping events as single events. Merge event classes.
export function eventsMerge(events: Array<Event>): Array<Event> {
    return eventsSort(events).
        reduce((acc: Array<Event>, currEvent: Event) => {
            if (acc.length < 1) {
                acc.push(currEvent);
                return acc;
            } else {
                let prevEvent: Event = acc[acc.length - 1];

                if (prevEvent.times.endsAt.isAfter(currEvent.times.beginsAt)) {
                    // Merges the 2 overlapping events.
                    prevEvent.times.endsAt = moment.max(
                        prevEvent.times.endsAt, currEvent.times.endsAt);
                    prevEvent.times.duration = moment.duration(
                        prevEvent.times.endsAt.diff(prevEvent.times.beginsAt));
                } else {
                    acc.push(currEvent);
                }

                return acc;
            }
        }, []);
}