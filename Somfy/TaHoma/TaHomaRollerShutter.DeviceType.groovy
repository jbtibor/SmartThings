/**
 *  Copyright (c) 2016-2017 Tibor Jakab-Barthi
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
		capability "Configuration"
		capability "Switch"
		capability "Window Shade"

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

preferences {
	section {
		input ("disableOpen", "bool", title: "Disable open", defaultValue: false, displayDuringSetup: false)
		input ("disableClose", "bool", title: "Disable close", defaultValue: false, displayDuringSetup: false)
	}
}

def getDeviceTypeVersion() {
	"1.2.20171222" 
}

def debug(message) {
	if (parent.settings.debugMode) {
		log.debug("DT $deviceTypeVersion: $message")
	}
}

def close() {
	debug("close()")

	if (settings.disableClose) {
		debug("END: close: Close is not enabled.")
	} else {
		parent.stop(state.executionId, device.label)
		state.executionId = parent.close(device.name, device.label)

		sendEvent(name: "windowShade", value: "closed")
		sendEvent(name: "switch", value: "off")

		debug("END: close executionId: ${state.executionId}")
	}
}

def identify() {
	debug("identify()")

	if (settings.disableClose) {
		debug("END: identify: Close is not enabled.")
	} else {
		parent.stop(state.executionId, device.label)
		state.executionId = parent.identify(device.name, device.label)

		sendEvent(name: "windowShade", value: "closed")
		sendEvent(name: "switch", value: "off")

		debug("END: identify executionId: ${state.executionId}")
	}
}

def open() {
	debug("open()")

	if (settings.disableOpen) {
		debug("END: open: Open is not enabled.")
	} else {
		parent.stop(state.executionId, device.label)
		state.executionId = parent.open(device.name, device.label)

		sendEvent(name: "windowShade", value: "open")
		sendEvent(name: "switch", value: "on")

		debug("END: open executionId: ${state.executionId}")
	}
}

def presetPosition() {
	debug("presetPosition()")

	if (settings.disableClose) {
		debug("END: presetPosition: Close is not enabled.")
	} else {
		parent.stop(state.executionId, device.label)
		state.executionId = parent.presetPosition(device.name, device.label)

		sendEvent(name: "windowShade", value: "partially open")
		sendEvent(name: "switch", value: "on")

		debug("END: presetPosition executionId: ${state.executionId}")
	}
}

def stop() {
	debug("stop()")

	def result = parent.stop(state.executionId, device.label)

	sendEvent(name: "windowShade", value: "partially open")
	sendEvent(name: "switch", value: "on")

	debug("END: stop executionId: ${state.executionId} $result")
}

// Switch
def on() {
	debug("on()")

	close()
}

def off() {
	debug("off()")

	open()
}
