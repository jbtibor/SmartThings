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
	definition (name: "TaHoma Switch", namespace: "jbt", author: "Tibor Jakab-Barthi") {
		capability "Configuration"
		capability "Switch"
	}

	tiles(scale: 2) {
		multiAttributeTile(name: "switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
			tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: 'On', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821", nextState: "turningOff"
				attributeState "turningOff", label:'...', icon: "st.switches.switch.off", backgroundColor:"#ffffff", nextState: "turningOff"
				attributeState "off", label: 'Off', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
				attributeState "turningOn", label:'...', icon: "st.switches.switch.on", backgroundColor:"#79b821", nextState: "turningOn"
			}
		}

		main ("switch")
		details(["switch"])
	}
}

preferences {
	section {
		icon(title: "Update Icon")
	}
}

def getDeviceTypeVersion() {
	"1.1.20171222" 
}

def debug(message) {
	if (parent.settings.debugMode) {
		log.debug("DT $deviceTypeVersion: $message")
	}
}

// Switch
def on() {
	debug("on()")

	state.executionId = parent.switchOn(device.name, device.label)

	sendEvent(name: "switch", value: "on")
}

def off() {
	debug("off()")

	state.executionId = parent.switchOff(device.name, device.label)

	sendEvent(name: "switch", value: "off")
}
