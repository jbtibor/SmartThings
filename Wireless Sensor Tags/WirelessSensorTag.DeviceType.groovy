/**
 *  Copyright (c) 2017 Tibor Jakab-Barthi
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
	definition (name: "Wireless Sensor Tag", namespace: "jbt", author: "Tibor Jakab-Barthi") {
		capability "Battery"
		capability "Refresh"
		capability "Relative Humidity Measurement"
		capability "Sensor"
		capability "Temperature Measurement"
	}

	tiles {
		valueTile("temperature", "device.temperature", width: 3, height: 2) {
			state("temperature", label: '${currentValue}°', unit: "C",
				backgroundColors:[
					// Celsius
					[value: 0, color: "#153591"],
					[value: 7, color: "#1e9cbb"],
					[value: 15, color: "#90d2a7"],
					[value: 23, color: "#44b621"],
					[value: 28, color: "#f1d801"],
					[value: 35, color: "#d04e00"],
					[value: 37, color: "#bc2323"],
					// Fahrenheit
					[value: 40, color: "#153591"],
					[value: 44, color: "#1e9cbb"],
					[value: 59, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 84, color: "#f1d801"],
					[value: 95, color: "#d04e00"],
					[value: 96, color: "#bc2323"]
				]
			)
		}

		valueTile("temperatureMain", "device.temperature", width: 1, height: 1, canChangeIcon: true) {
			state("temperature", label: '${currentValue}°', unit: "C", icon: "st.Weather.weather2")
		}

		valueTile("humidity", "device.humidity", width: 1, height: 1) {
			state("humidity", label:'Humidity ${currentValue}%',
				backgroundColors:[
					[value: 10, color: "#FF0000"],
					[value: 35, color: "#FF9900"],
					[value: 40, color: "#00CC00"],
					[value: 60, color: "#00CC00"],
					[value: 65, color: "#FF9900"],
					[value: 90, color: "#FF0000"],
				]
            )
		}

		valueTile("battery", "device.battery", width: 1, height: 1) {
			state("battery", label:'Battery ${currentValue}%')
		}

		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat") {
			state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		main (["temperatureMain"])
		details(["temperature", "humidity", "battery", "refresh"])
	}
}

def refresh() {
	log.debug("WirelessSensorTag.DeviceType.refresh()")

	poll()
}

void poll() {
	log.debug("WirelessSensorTag.DeviceType.poll()")

	parent.poll()
}

def generateEvent(Map eventData){
	log.debug("WirelessSensorTag.DeviceType.generateEvent(${eventData})")

    eventData.each { name, value ->
		if (name == "temperature") {
			value = convertTemperatureIfNeeded(value.toDouble(), "C", 1)
			def numberBigDecimal = new BigDecimal(value)
 
			value = numberBigDecimal.setScale(1, BigDecimal.ROUND_HALF_UP)
		} else if (name == "humidity") {
			value = Math.round(value.toDouble())
		} else if (name == "battery") {
			value = Math.round(100 * value.toDouble())
		}

		log.debug("sendEvent(name: '$name', value: '$value')")

        sendEvent(name: name, value: value)
    }
}
