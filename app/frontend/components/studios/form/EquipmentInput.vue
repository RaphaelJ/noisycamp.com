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
        <div class="cell callout alert" v-if="fieldHasError()">
            {{ fieldError() }}
        </div>

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

                    <!-- <input
                        type="hidden"
                        :name="fieldName('pricePerHour', index)"
                        :value="equipment.pricePerHour"> -->

                    <button
                        class="close-button equipment-remove"
                        type="button"
                        @click="equipmentRemove(index)">
                        <span aria-hidden="true">×</span>
                    </button>

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
                                        class="text-sans-serif text-overflow-ellipsis"
                                        :title="equipmentCategoryName(equipment)">
                                        {{ equipmentCategoryName(equipment) }}
                                    </h6>
                                </div>

                                <!-- <div class="cell small-4 text-right">
                                    <small v-if="equipment.pricePerHour">
                                        €{{ equipment.pricePerHour }}/hour
                                    </small>
                                    <small v-else>
                                        Included
                                    </small>
                                </div> -->

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
                            :label="family.name">

                            <option
                                v-for="equipment in family.equipments"
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

                <!-- <div class="cell small-12">
                    <input
                        id="new-equipment-has-extra-fee"
                        type="checkbox"
                        v-model="newEquipment.hasExtraFee">

                    <label for="new-equipment-has-extra-fee">
                        This equipment requires an extra rental fee
                    </label>
                </div>

                <slide-down-transition :max-height="85">
                    <div
                        class="cell small-12 medium-4 medium-offset-1 large-3"
                        v-if="newEquipment.hasExtraFee">

                        <label>
                            Price per hour

                            <money-input
                                :currency="currency"
                                v-model="newEquipment.pricePerHour"
                                :required="newEquipment.hasExtraFee">
                            </money-input>
                        </label>
                    </div>
                </slide-down-transition> -->

                <div class="cell small-12">
                    <button
                        type="button" class="button primary"
                        @click="equipmentAdd()"
                        :disabled="!newEquipment.category">
                        Add equipment
                    </button>
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import Vue from "vue";

import { fromCDN } from '../../../misc/CDN';

import VueInput from '../../widgets/VueInput';
import MoneyInput from '../../widgets/MoneyInput.vue';
import SlideDownTransition from '../../../transitions/SlideDownTransition.vue';

declare var NC_CONFIG: any;
declare var NC_ROUTES: any;

export default Vue.extend({
    mixins: [VueInput],
    props: {
        value: { type: Object, default() { return {}; } },

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
                    fee: null,
                });
            });
        }

        return {
            newEquipment: { category: null, details: null, hasExtraFee: false, pricePerHour: null },

            equipments: equipments,
        }
    },
    computed: {
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
        }
    },
    mounted() {
    },
    methods: {
        equipmentAdd() {
            if (!this.newEquipment.category) {
                return;
            }

            if (this.newEquipment.category == '__other') {
                this.newEquipment.category = null;
            }

            var fee = null;
            if (this.newEquipment.hasExtraFee && this.newEquipment.extraFee) {
                fee = this.newEquipment.extraFee;
            }

            this.equipments.push({
                category: this.newEquipment.category,
                details: this.newEquipment.details,
                fee: fee,
            });

            this.newEquipment.category = null;
            this.newEquipment.details = null;
            this.newEquipment.hasExtraFee = false;
            this.newEquipment.extraFee = null;
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
        }
    },
    watch: {
    },
    components: { MoneyInput, SlideDownTransition },
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
