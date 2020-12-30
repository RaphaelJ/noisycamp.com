<!--
  Noisycamp is a platform for booking music studios.
  Copyright (C) 2020  Raphael Javaux <raphael@noisycamp.com>

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
-->

<template>
    <div class="booking-equipment-input">
        <div
            v-for="equip in pricedEquipments"
            :key="equip.id">
            <div class="grid-x grid-margin-x">
                <div class="cell auto checkbox-group text-overflow-ellipsis">
                    <input 
                        :id="'equipment-' + equip.id"
                        name="equipments"
                        :value="equip.id"
                        type="checkbox"
                        v-model="equipmentToggle[equip.id]">

                    <label :for="'equipment-' + equip.id">
                        <img
                            class="equipment-icon"
                            alt="Equipment icon"
                            :src="equipmentCategoryIcon(equip)">
                        &nbsp;

                        <span
                            v-if="equip.category"
                            :title="equipmentCategoryName(equip)">
                            {{ equipmentCategoryName(equip) }}
                            <small
                                v-if="equip.details"
                                :title="equip.details">
                                {{ equip.details }}
                            </small>
                        </span>
                        <span 
                            v-else-if="equip.details"
                            :title="equip.details">
                            {{ equip.details }}
                        </span>
                    </label>
                </div>

                <div class="cell small-3 text-right">
                    <money-amount :value="equipmentPrice(equip)"></money-amount>
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import Vue, { PropOptions } from "vue";

import { fromCDN } from '../../../misc/CDN';
import { asCurrency } from '../../../misc/MoneyUtils';

import MoneyAmount from '../../widgets/MoneyAmount.vue'
import VueInput from '../../widgets/VueInput';

declare var NC_CONFIG: any;
declare var NC_ROUTES: any;

export default Vue.extend({
    mixins: [VueInput],
    props: {
        equipments: <PropOptions<Object[]>>{ type: Array, required: true },

        // The duration of the session, in seconds.
        sessionDuration: { type: Number, required: true },

        value: <PropOptions<Object[]>>{ type: Array, required: true },
    },
    data() {  
        let pricedEquipments = this.equipments.filter(e => e['price']);

        let equipmentToggle = {};
        for (var e in pricedEquipments) {
            equipmentToggle[e['id']] = this.value.some(e2 => e2['id'] == e['id']);
        }

        return {
            pricedEquipments: pricedEquipments,
            equipmentToggle: equipmentToggle,
        };
    },
    computed: {
        
    },
    methods: {
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
                return null;
            }
        },

        equipmentPrice(equipment) {
            var price = asCurrency(equipment['price']['value']);

            if (equipment['price']['price-type'] == 'per-hour') {
                price = price.multiply(this.sessionDuration).divide(3600);
            }

            return price;
        },
    },
    watch: {
        'equipmentToggle': {
            deep: true,
            handler(newValue) {
                this.$emit('input', this.equipments.filter(e => newValue[e.id]));
            },
        },
    },
    components: { MoneyAmount }
});
</script>

<style>
.booking-equipment-input {
    overflow-y: auto;
    overflow-x: hidden;
    max-height: 150px;
}

.booking-equipment-input .equipment-icon {
    height: 1rem;
    vertical-align: text-top;
}

.booking-equipment-input .checkbox-group {
    margin-bottom: 0.5rem;
}
</style>
