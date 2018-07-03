/**
 *  Copyright (c) 2017 Tibor Jakab-Barthi
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *  See the License for the specific language governing permissions and limitations under the License.
 *
 *  Author: Tibor Jakab-Barthi
 */
metadata {
	definition (name: "TaHoma Light Sensor IO", namespace: "jbt", author: "Tibor Jakab-Barthi") {
		capability "Illuminance Measurement"
		capability "Refresh"
		capability "Sensor"
	}

	tiles(scale: 2) {
		valueTile("illuminance", "device.illuminance", width: 6, height: 6, canChangeIcon: true) {
			state("illuminance", label:'${currentValue} lux', icon: "st.Weather.weather11",
				backgroundColors:[
					[value: 0, color: "#000000"],
					[value: 10000, color: "#444400"],
					[value: 20000, color: "#777700"],
					[value: 30000, color: "#999900"],
					[value: 40000, color: "#AAAA00"],
					[value: 50000, color: "#BBBB00"],
					[value: 60000, color: "#CCCC00"],
					[value: 70000, color: "#DDDD00"],
					[value: 80000, color: "#EEEE00"],
					[value: 90000, color: "#FFFF00"]
				]
			)
		}

		standardTile("refresh", "device.refresh", inactiveLabel: false,  width: 2, height: 2, decoration: "flat") {
			state "default", action:"refresh", icon:"st.secondary.refresh"
		}

		main ("illuminance")
		details(["illuminance", "refresh"])
	}
}

def getDeviceTypeVersion() {
	"1.1.20180616" 
}

def debug(message) {
	if (parent.settings.debugMode) {
		log.debug("DT $deviceTypeVersion: $message")
	}
}

def generateEvents(Map eventData) {
	debug("generateEvents(${eventData})")

    eventData.each { name, value ->
		if (name == "illuminance") {
        	int intValue = value as Integer

			debug("sendEvent(name: '$name', value: '$intValue')")

	        sendEvent(name: name, value: intValue, unit: "lux")
		}
    }
}

void poll() {
	debug("poll()")

	parent.poll()

	debug("END: poll()")
}

def refresh() {
	debug("refresh()")

	poll()

	debug("END: refresh()")
}

def setCapabilities(newCapabilities) {
	debug("setCapabilities($newCapabilities)")
}
