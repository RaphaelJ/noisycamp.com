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

import * as currency from 'currency.js';

declare var NC_CONFIG: any;

// Returns a new currency.js object from a JSON-described amount of money.
export function asCurrency(value): currency {
    let currInfo = NC_CONFIG.currencies[value['currency']];

    return currency(value['value'], {
        precision: currInfo.decimals,
        symbol: currInfo.symbol
    });
}

// Returns a new JSON-described amount of money from a currency.js object.
export function asMoney(value, currencyCode) {
    return {
        'currency': currencyCode,
        'value': value.value,
    };
}

// Returns the least expensive price for the given studio and the given day.
//
// Also returns a boolean set to true if the studio also has a more expensive pricing.
export function startingPrice(pricingPolicy, isWeekend: boolean): [currency, boolean] {
    if (isWeekend && pricingPolicy['weekend'] != null) {
        return [asCurrency(pricingPolicy['weekend']['price-per-hour']), false];
    } else {
        let standardPrice = asCurrency(pricingPolicy['price-per-hour']);

        if (pricingPolicy['evening'] != null) {
            let eveningPrice = asCurrency(pricingPolicy['evening']['price-per-hour']);

            if (eveningPrice.value < standardPrice.value) {
                return [eveningPrice, true];
            } else {
                return [standardPrice, eveningPrice.value != standardPrice.value];
            }
        } else {
            return [standardPrice, false];
        }
    }
}