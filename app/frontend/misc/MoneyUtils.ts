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
export function asCurrency(value) {
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
