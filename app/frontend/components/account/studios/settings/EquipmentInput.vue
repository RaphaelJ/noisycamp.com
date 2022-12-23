<!--
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

  Provides an input field with an address autocompletion.
-->

<template>
    <div class="grid-x grid-margin-y">
        <div class="cell small-12" v-if="equipments.length > 0">
            <ul
                class="equipment-list grid-x grid-margin-x grid-margin-y grid-padding-x grid-padding-y">
                <li
                    class="cell small-12 medium-6 large-4 equipment"
                    v-for="(equipment, index) in equipments">

                    <input
                        type="hidden"
                        :name="fieldName('id', index)"
                        :value="equipment.id">

                    <input
                        type="hidden"
                        :name="fieldName('category', index)"
                        :value="equipment.category">

                    <input
                        type="hidden"
                        :name="fieldName('details', index)"
                        :value="equipment.details">

                    <input
                        type="hidden"
                        v-if="equipment.price"
                        :name="fieldName('price.type', index)"
                        :value="equipment.price.type">

                    <input
                        type="hidden"
                        v-if="equipment.price"
                        :name="fieldName('price.value', index)"
                        :value="equipment.price.value">

                    <button
                        class="close-button equipment-remove"
                        type="button"
                        @click="equipmentRemove(index)">
                        <span aria-hidden="true">×</span>
                    </button>

                    <div
                        v-if="equipment.price"
                        class="equipment-price">
                        <money-amount :value="equipmentPriceValue(equipment)"></money-amount>
                        per
                        <span v-if="equipment.price.type == 'per-hour'">hour</span>
                        <span v-else-if="equipment.price.type == 'per-session'">session</span>
                    </div>

                    <div class="grid-x grid-margin-x">
                        <div class="cell small-3 medium-4 text-center">
                            <img
                                class="equipment-icon"
                                :alt="equipmentCategoryName(equipment)"
                                :src="equipmentCategoryIcon(equipment)">
                        </div>

                        <div class="cell small-9 medium-8">
                            <div class="grid-x">
                                <div class="cell small-12 equipment-name">
                                    <h6
                                        class="text-overflow-ellipsis"
                                        :title="equipmentCategoryName(equipment)">
                                        {{ equipmentCategoryName(equipment) }}
                                    </h6>
                                </div>

                                <div
                                    class="cell small-12 equipment-details text-overflow-ellipsis"
                                    :title="equipment.details">
                                    <small>{{ equipment.details }}</small>
                                </div>
                            </div>
                        </div>
                    </div>
                </li>
            </ul>
        </div>

        <div class="cell small-12">
            <h5>Add new equipment</h5>

            <div class="grid-x grid-margin-x">
                <label class="cell small-12 medium-4">
                    Category

                    <select
                        v-model="newEquipment.category">
                        <option disabled>Please select an equipment category</option>
                        <optgroup
                            v-for="family in equipmentByFamily"
                            :key="family.name"
                            :label="family.name">

                            <option
                                v-for="equipment in family.equipments"
                                :key="equipment.code"
                                :value="equipment.code">
                                {{ equipment.name }}
                            </option>
                        </optgroup>
                        <option value="__other">Other</option>
                    </select>
                </label>

                <label class="cell small-12 medium-8 large-5">
                    Details

                    <input
                        type="text"
                        v-model="newEquipment.details"
                        placeholder="i.e. brand, model">
                </label>

                <div class="cell small-12">
                    <div class="checkbox-group">
                        <input
                            id="new-equipment-has-fee"
                            type="checkbox"
                            v-model="newEquipment.hasFee"
                            :disabled="!canAddFee">

                        <label for="new-equipment-has-fee">
                            The use of this equipment requires an extra rental fee
                        </label>
                    </div>

                    <p
                        v-if="!canAddFee"
                        class="help-text">
                        <a
                            :href="planUpgradeUrl"
                            target="_blank">
                            Upgrade your NoisyCamp account
                        </a>
                        to charge users for the use of additional equipments and gears.
                    </p>
                </div>

                <slide-down-transition :max-height="120">
                    <div
                        class="cell small-12"
                        v-if="canAddFee && newEquipment.hasFee">

                        <div class="grid-x grid-margin-x">
                            <div class="cell small-12 medium-offset-1 medium-shrink radio-group">
                                <input
                                    type="radio"
                                    value="per-hour"
                                    v-model="newEquipment.priceType"
                                    id="new-equipment-price-type-per-hour">
                                <label for="new-equipment-price-type-per-hour">
                                    Charge per hour
                                </label>
                            </div>

                            <div class="cell small-12 medium-shrink radio-group">
                                <input
                                    type="radio"
                                    value="per-session"
                                    v-model="newEquipment.priceType"
                                    id="new-equipment-price-type-per-session">
                                <label for="new-equipment-price-type-per-session">
                                    Charge per session
                                </label>
                            </div>
                        </div>

                        <div class="grid-x grid-margin-x">
                            <label class="cell small-12 medium-4 medium-offset-1 large-3">
                                <span v-if="newEquipment.priceType == 'per-hour'">
                                    Price per hour
                                </span>
                                <span v-else-if="newEquipment.priceType == 'per-session'">
                                    Price per session
                                </span>

                                <money-input
                                    :currency="currency"
                                    :name="null"
                                    v-model="newEquipment.priceValue">
                                </money-input>
                            </label>
                        </div>
                    </div>
                </slide-down-transition>

                <div class="cell small-12">
                    <button
                        type="button" class="button primary"
                        @click="equipmentAdd()"
                        :disabled="!canAddEquipment">
                        Add equipment
                    </button>
                </div>

                <div
                    class="cell"
                    v-if="globalError">
                    <label>
                        <span class="error">{{ globalError }}</span>
                    </label>
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import Vue from "vue";

import { fromCDN } from '../../../../misc/URL';

import VueInput from '../../../widgets/VueInput';
import MoneyAmount from '../../../widgets/MoneyAmount.vue'
import MoneyInput from '../../../widgets/MoneyInput.vue';
import SlideDownTransition from '../../../../transitions/SlideDownTransition.vue';

declare var NC_CONFIG: any;
declare var NC_ROUTES: any;

export default Vue.extend({
    mixins: [VueInput],
    props: {
        value: { type: Object, default() { return {}; } },

        canAddFee: { type: Boolean, default: false },

        currency: { type: String, required: false },
    },
    data() {
        let equipments = [];

        if (this.value) {
            Object.keys(this.value).sort().forEach(idx => {
                let equipment = this.value[idx];
                equipments.push({
                    id: equipment.id,
                    category: equipment.category,
                    details: equipment.details,
                    price: equipment.price,
                });
            });
        }

        return {
            newEquipment: {
                category: null, details: null,
                hasFee: false, priceType: "per-hour", priceValue: null,
            },

            equipments: equipments,
        }
    },
    computed: {
        planUpgradeUrl() {
            return NC_ROUTES.controllers.account.PlansController.index().url;
        },

        // Returns an object of equipment families, each with a `equipment` field with the family's
        // equipments.
        equipmentByFamily() {
            let families = {};

            for (let code in NC_CONFIG.equipments) {
                let equip = NC_CONFIG.equipments[code];
                if (!(equip.family in families)) {
                    let family = NC_CONFIG.equipmentFamilies[equip.family];
                    families[equip.family] = {
                        code: family.code,
                        name: family.name,
                        equipments: {}
                    }
                }

                families[equip.family].equipments[code] = equip;
            }

            return families;
        },

        canAddEquipment() {
            let value = this.newEquipment;

            return value.category
                && (value.category != '__other' || value.details)
                && (!value.hasPrice || (value.priceType && value.priceValue))
        }
    },
    mounted() {
    },
    methods: {
        equipmentAdd() {
            if (!this.canAddEquipment) {
                return;
            }

            let value = this.newEquipment;

            if (value.category == '__other') {
                value.category = null;
            }

            var price = null;
            if (value.hasFee) {
                price = {
                    type: value.priceType,
                    value: value.priceValue,
                };
            }

            this.equipments.push({
                category: value.category,
                details: value.details,
                price: price,
            });

            value.category = null;
            value.details = null;
            value.hasFee = false;
            value.priceValue = null;
        },

        equipmentRemove(index: number) {
            this.equipments.splice(index, 1);
        },

        equipmentCategoryIcon(equipment) {
            var iconName;
            if (equipment.category) {
                iconName = "images/vendor/music-icons/" + equipment.category + ".svg";
            } else {
                iconName = "images/vendor/music-icons/other.svg";
            }

            return fromCDN(NC_ROUTES.controllers.Assets.versioned(iconName));
        },

        equipmentCategoryName(equipment) {
            if (equipment.category) {
                return NC_CONFIG.equipments[equipment.category].name;
            } else {
                return "Other";
            }
        },

        equipmentPriceValue(equipment) {
            return {
                currency: this.currency,
                value: equipment.price.value,
            };
        }
    },
    components: { MoneyAmount, MoneyInput, SlideDownTransition },
});
</script>

<style>
.equipment-list .equipment .equipment-remove {
    position: absolute;
    top: 0rem;
    right: 0.35rem;

    opacity: 0.5;
}

.equipment-list .equipment .equipment-remove:hover {
    opacity: 1;
}
</style>
