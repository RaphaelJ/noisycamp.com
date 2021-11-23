/**
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
*/

// Provides a mixin with common methods and interface shared by components that provide an
// <input>-like behavior.
export default {
    props: {
        // The prefix that will be used for <input> sub-fields. Can be empty (i.e. no prefix).
        name: { type: String, required: false, default: '' },

        // The input data object or value. If the input if a composite field, use an object to
        // describe the values of the object (see `FormUtils.dataAsJson`).
        value: { type: [ Object, String ], required: false },

        // The form errors, indexed by the field's name (see `FormUtils.errorsAsJson`).
        errors: {
            type: [ Object, String ], required: false,
            default: function () { return {}; }
        },
    },
    computed: {
        globalError(): string {
            if (typeof(this.errors) == 'string') {
                return this.errors;
            } else if (this.errors && typeof(this.errors) == 'object' && "" in this.errors) {
                return this.errors[""];
            } else {
                return null;
            }
        }
    },
    methods: {
        // Returns a prefixed sub-field name, including the parent form's
        // prefix if there is one.
        //
        // If `index` is provided, will add a `[index]` subscript prefix.
        fieldName(subFieldName?: string, index?: number): string {
            var prefix = this.name;

            if (index != undefined) {
                prefix += '[' + index + ']';
            }

            if (subFieldName != undefined) {
                if (prefix != '') {
                    return prefix + '.' + subFieldName;
                } else {
                    return subFieldName;
                }
            } else {
                return prefix;
            }
        },

        fieldValue(fieldName: string, defaultVal = undefined) {
            if (this.value && fieldName in this.value) {
                return this.value[fieldName];
            } else {
                return defaultVal;
            }
        },
    }
};
