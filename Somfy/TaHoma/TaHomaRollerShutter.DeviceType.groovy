/**
 *  Copyright (c) 2016 Tibor Jakab-Barthi
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
	definition (name: "TaHoma Roller Shutter", namespace: "jbt", author: "Tibor Jakab-Barthi") {
		capability "Window Shade"

		attribute "lastUpdateDate", "string"

		command "identify"
		command "stop"
	}

	tiles(scale: 2) {
		standardTile("windowShade", "device.windowShade", width: 4, height: 6) {
			state("open", label: 'Open', icon:"https://tahomabysomfyapi.azurewebsites.net/img/RollerShutterUp.png", backgroundColor:"#ffffff")
			state("partially open", label: 'My', icon:"https://tahomabysomfyapi.azurewebsites.net/img/RollerShutterMy.png", backgroundColor:"#ffffff")
			state("closed", label: 'Closed', icon:"https://tahomabysomfyapi.azurewebsites.net/img/RollerShutterDown.png", backgroundColor:"#ffffff")
			state("unknown", defaultState: true, label: 'Unknown', icon:"https://tahomabysomfyapi.azurewebsites.net/img/RollerShutterUnknown.png", backgroundColor:"#ffffff")
		}

		standardTile("open", "device.windowShade", width: 2, height: 2, decoration: "flat") {
    		state("any", icon: "https://tahomabysomfyapi.azurewebsites.net/img/ButtonOpen.png", action: "open")
		}

		standardTile("my", "device.windowShade", width: 2, height: 2, decoration: "flat") {
    		state("any", icon: "https://tahomabysomfyapi.azurewebsites.net/img/ButtonMy.png", action: "presetPosition")
		}

		standardTile("close", "device.windowShade", width: 2, height: 2, decoration: "flat") {
    		state("any", icon: "https://tahomabysomfyapi.azurewebsites.net/img/ButtonClose.png", action: "close")
		}

		standardTile("identify", "device.windowShade", width: 3, height: 2, decoration: "flat") {
    		state("any", icon: "https://tahomabysomfyapi.azurewebsites.net/img/ButtonIdentify.png", action: "identify")
		}

		standardTile("stop", "device.windowShade", width: 3, height: 2, decoration: "flat") {
    		state("any", icon: "https://tahomabysomfyapi.azurewebsites.net/img/ButtonStop.png", action: "stop")
		}

		main ("windowShade")
		details(["windowShade", "open", "my", "close", "identify", "stop"])
	}
}

def close() {
	log.debug "DT: close()"

	parent.stop(state.executionId)
	state.executionId = parent.close(device.name)

	sendEvent(name: "windowShade", value: "closed")

	log.debug "close executionId ${state.executionId}"
}

def identify() {
	log.debug "DT: identify()"

	parent.stop(state.executionId)
	state.executionId = parent.identify(device.name)

	sendEvent(name: "windowShade", value: "closed")

	log.debug "identify executionId ${state.executionId}"
}

def open() {
	log.debug "DT: open()"

	parent.stop(state.executionId)
	state.executionId = parent.open(device.name)

	sendEvent(name: "windowShade", value: "open")

	log.debug "open executionId ${state.executionId}"
}

def presetPosition() {
	log.debug "DT: presetPosition()"

	parent.stop(state.executionId)
	state.executionId = parent.presetPosition(device.name)

	sendEvent(name: "windowShade", value: "partially open")

	log.debug "presetPosition executionId ${state.executionId}"
}

def stop() {
	log.debug "DT: stop()"

	def result = parent.stop(state.executionId)

	sendEvent(name: "windowShade", value: "partially open")

	log.debug "stop executionId ${state.executionId} $result"
}

void poll() {
	log.debug "poll"

	parent.poll()
}

def generateEvent(Map eventData){
	log.debug "generateEvent(${eventData})"

    eventData.each { name, value ->
		log.debug "sendEvent(name: $name, value: $value)"

        sendEvent(name: name, value: value)
    }
}
