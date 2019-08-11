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

  Provides a mixin with common methods and interface shared by components that
  provide an <input>-like behavior.
*/

export default {
    props: {
        // The prefix that will be used for <input> sub-fields.
        name: { type: String, required: true },

        // The form errors, indexed by the field's name (see
        // `play.api.data.Form.errorsAsJson`)
        errors: {
            type: Object, required: false,
            default: function () { return {}; }
        },
    },
    methods: {
        // Returns the prefixed field name.
        fieldName(fieldName) {
            return this.name + '.' + fieldName;
        },

        fieldHasError(fieldName) {
            return this.fieldName(fieldName) in this.errors;
        },

        fieldErrors(fieldName) {
            return this.errors[this.fieldName(fieldName)];
        },

        fieldError(fieldName) {
            return this.fieldErrors(fieldName)[0];
        },
    }
};
