/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2021  Raphael Javaux <raphael@noisycamp.com>
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

export interface EventTimes {
    beginsAt: moment.Moment
    endsAt?: moment.Moment
    duration?: moment.Duration
}

export interface Event {
    title?: string
    href?: string
    classes: Set<string>
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
export function eventsSort(events: Event[]): Event[] {
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
export function eventsMerge(events: Event[]): Event[] {
    return eventsSort(events).
        reduce((acc: Event[], currEvent: Event) => {
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

                    prevEvent.classes = new Set([...prevEvent.classes, ...currEvent.classes]);
                } else {
                    acc.push(currEvent);
                }

                return acc;
            }
        }, []);
}