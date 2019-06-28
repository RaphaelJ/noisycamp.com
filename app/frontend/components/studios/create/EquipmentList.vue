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
        <div class="cell small-12">
            <div v-if="equipments.length < 1" class="text-center">
                <em>No equipment specified.</em>
            </div>

            <ul class="equipment-list grid-x grid-margin-x grid-margin-y grid-padding-x grid-padding-y">
                <li
                    class="equipment cell small-12 medium-6 large-4"
                    v-for="(equipment, index) in equipments">

                    <button
                        class="close-button equipment-remove"
                        type="button"
                        @click="equipmentRemove(index)">
                        <span aria-hidden="true">×</span>
                    </button>

                    <div class="grid-x grid-margin-x">
                        <div class="cell small-3 text-center">
                            <img
                                class="equipment-icon"
                                :alt="equipmentTypeName(equipment)"
                                :src="equipmentTypeIcon(equipment)">
                        </div>

                        <div class="cell small-9">
                            <div class="grid-x">
                                <div class="cell small-7">
                                    <h6>{{ equipmentTypeName(equipment) }}</h6>
                                </div>

                                <div class="cell small-4 text-right">
                                    <small v-if="equipment.fee">
                                        €{{ equipment.fee }}/hour
                                    </small>
                                    <small v-else>
                                        Included
                                    </small>
                                </div>

                                <div class="cell small-12">
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
                <label class="cell small-6 medium-3">
                    Type

                    <select
                        ref="new-type"
                        v-model="newEquipment.type">
                        <option disabled>Please select an equipment type</option>
                        <optgroup
                            v-for="family in equipmentByFamily"
                            :label="family.name">

                            <option
                                v-for="equipment in family.equipments"
                                :value="equipment.code">
                                {{ equipment.name }}
                            </option>
                        </optgroup>
                        <option value="">Other</option>
                    </select>
                </label>

                <label class="cell small-6 medium-5">
                    Details

                    <input
                        type="text"
                        v-model="newEquipment.details"
                        placeholder="i.e. brand, model"
                        required>
                </label>

                <div class="cell small-12">
                    <input
                        id="new-equipment-has-extra-fee"
                        type="checkbox"
                        v-model="newEquipment.hasExtraFee">

                    <label for="new-equipment-has-extra-fee">
                        This equipment requires an extra rental fee
                    </label>
                </div>

                <label
                    class="cell small-12 medium-2 medium-offset-1"
                    v-if="newEquipment.hasExtraFee">

                    Price per hour

                    <div class="input-group">
                        <span class="input-group-label">$</span>
                        <input
                            class="input-group-field"
                            type="number"
                            v-model="newEquipment.extraFee">
                    </div>
                </label>

                <label class="cell small-12">
                    <button
                        type="button" class="button"
                        @click="equipmentAdd()">
                        Add equipment
                    </button>
                </label>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import Vue from "vue";

declare var NC_CONFIG: any;
declare var NC_ROUTES: any;

export default Vue.extend({
    props: {
    },
    data() {
        return {
            newEquipment: { type: null, details: null, hasExtraFee: false, extraFee: null },

            // equipments: [
            //     {
            //         id: 1234,
            //         type: 'drum-kit',
            //         details: "Tama Superstar w/ Zildjian SBT cymbals",
            //         fee: 5.0,
            //     }, {
            //         id: 1235,
            //         type: 'electric-guitar',
            //         details: "Fender Stratocaster",
            //         fee: null,
            //     },
            // ],
            equipments: []
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
            var fee = null;
            if (this.newEquipment.hasExtraFee && this.newEquipment.extraFee) {
                fee = this.newEquipment.extraFee;
            }

            this.equipments.push({
                type: this.newEquipment.type,
                details: this.newEquipment.details,
                fee: fee,
            });

            this.newEquipment.type = null;
            this.newEquipment.details = null;
            this.newEquipment.hasExtraFee = false;
            this.newEquipment.extraFee = null;
        },

        equipmentRemove(index: number) {
            this.equipments.splice(index, 1);
        },

        equipmentTypeIcon(equipment) {
            var icon_name;
            if (equipment.type) {
                icon_name = "images/vendor/music-icons/" + equipment.type + ".svg";
            } else {
                icon_name = "images/vendor/music-icons/other.svg";
            }

            return NC_ROUTES.controllers.Assets.versioned(icon_name).url;
        },

        equipmentTypeName(equipment) {
            if (equipment.type) {
                return NC_CONFIG.equipments[equipment.type].name;
            } else {
                return "Other";
            }
        }
    },
    watch: {
    },
});
</script>

<style>
.equipment-list {
    list-style: none;
}

.equipment-list li {
    position: relative;
    display: inline-block;

    border: 1px solid rgba(0,0,0,0.1);
    border-radius: 2px;
}

.equipment-list li .equipment-remove {
    position: absolute;
    top: 0rem;
    right: 0.35rem;

    opacity: 0.5;
}

.equipment-list li .equipment-remove:hover {
    opacity: 1;
}

.equipment-list li .equipment-icon {
    width: 100%;
}
</style>
