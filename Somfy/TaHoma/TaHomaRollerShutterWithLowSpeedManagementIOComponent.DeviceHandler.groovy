/**
 *  Copyright (c) 2018 Tibor Jakab-Barthi
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
	definition (name: "TaHoma Roller Shutter With Low Speed Management IO Component", namespace: "jbt", author: "Tibor Jakab-Barthi") {
		capability "Configuration"
		capability "Refresh"
		capability "Switch"
		capability "Switch Level"
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
    		state("any", icon: "https://tahomabysomfyapi.azurewebsites.net/img/ButtonOpen2-Small.png", action: "open")
		}

		standardTile("my", "device.windowShade", width: 2, height: 2, decoration: "flat") {
    		state("any", icon: "https://tahomabysomfyapi.azurewebsites.net/img/ButtonMy2-Small.png", action: "presetPosition")
		}

		standardTile("close", "device.windowShade", width: 2, height: 2, decoration: "flat") {
    		state("any", icon: "https://tahomabysomfyapi.azurewebsites.net/img/ButtonClose2-Small.png", action: "close")
		}

		standardTile("identify", "device.windowShade", width: 2, height: 2, decoration: "flat") {
    		state("any", icon: "https://tahomabysomfyapi.azurewebsites.net/img/ButtonIdentify2-Small.png", action: "identify")
		}

		standardTile("stop", "device.windowShade", width: 2, height: 2, decoration: "flat") {
    		state("any", icon: "https://tahomabysomfyapi.azurewebsites.net/img/ButtonStop2-Small.png", action: "stop")
		}

		standardTile("refresh", "device.refresh", inactiveLabel: false,  width: 2, height: 2, decoration: "flat") {
			state "default", action:"refresh", icon:"st.secondary.refresh"
		}

		valueTile("levelMain", "device.level", width: 1, height: 1, canChangeIcon: true) {
			state("level", label: '${currentValue}%', unit: "%")
		}

		controlTile("levelSliderControl", "device.level", "slider", width: 6, height: 1) {
			state "level", action:"setLevel", label: '${currentValue}%', unit: "%"
		}

		main ("levelMain")
		details(["windowShade", "levelSliderControl", "open", "my", "close", "identify", "stop", "refresh"])
	}
}

preferences {
	section {
		input ("disableOpen", "bool", title: "Disable open", defaultValue: false, displayDuringSetup: false)
		input ("disableClose", "bool", title: "Disable close", defaultValue: false, displayDuringSetup: false)
	}
}

def getDeviceTypeVersion() {
	"1.0.20180604" 
}

def debug(message) {
	if (parent.settings.debugMode) {
		log.debug("DH $deviceTypeVersion: $message")
	}
}

def close() {
	debug("close()")

	if (settings.disableClose) {
		debug("END: close: Close is not enabled.")
	} else {
		stop()

		def percent = 100
    	def silent = true
		state.executionId = setLevelInternal(percent, silent)

		debug("END: close executionId: ${state.executionId}")
	}
}

def generateEvent(Map eventData){
	debug("generateEvent(${eventData})")

    eventData.each { name, value ->
		debug("sendEvent(name: '$name', value: '$value')")

		if (name == "level") {
			sendAllEvents(value)
		}

        sendEvent(name: name, value: value)
    }
}

def identify() {
	debug("identify()")

	if (settings.disableClose) {
		debug("END: identify: Close is not enabled.")
	} else {
		stop()

		state.executionId = parent.executeRollerShutterCommand("identify", device.name, "Identify ${device.label}")

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
		stop()

		def percent = 0
    	def silent = true
		state.executionId = setLevelInternal(percent, silent)

		debug("END: open executionId: ${state.executionId}")
	}
}

void poll() {
	debug("poll()")

	parent.poll()

	debug("END: poll()")
}

def presetPosition() {
	debug("presetPosition()")

	if (settings.disableClose) {
		debug("END: presetPosition: Close is not enabled.")
	} else {
		stop()

		state.executionId = parent.executeRollerShutterCommand("my", device.name, "My position ${device.label}")

		sendEvent(name: "windowShade", value: "partially open")
		sendEvent(name: "switch", value: "on")

		debug("END: presetPosition executionId: ${state.executionId}")
	}
}

def refresh() {
	debug("refresh()")

	poll()

	debug("END: refresh()")
}

def sendAllEvents(percent) {
	if (percent == 0) {
		sendEvent(name: "windowShade", value: "open")
		sendEvent(name: "switch", value: "off")
	} else if (percent == 100) {
		sendEvent(name: "windowShade", value: "closed")
		sendEvent(name: "switch", value: "on")
	}
	else {
		sendEvent(name: "windowShade", value: "partially open")
		sendEvent(name: "switch", value: "on")
	}

	sendEvent(name: "level", value: percent)
}

def setLevel(percent) {
	debug("setLevel($percent)")
    
    def silent = true
	state.executionId = setLevelInternal(percent, silent)

	debug("END: setLevel executionId: ${state.executionId}")
}

def setLevelInternal(percent, silent = true) {
	debug("setLevelInternal($percent, $silent)")

	def command = ""
	def parameters = ""

	if (silent) {
		command = "setClosureAndLinearSpeed"
		parameters = "$percent,\"lowspeed\""
	} else {
		command = "setClosure"
		parameters = "$percent"
	}

	sendAllEvents()

	def rollerShutterId = device.name
	def label = device.label

	def executionId = parent.executeRollerShutterCommand(command, rollerShutterId, "Set level $label", parameters)

	debug("END: setLevelInternal executionId: ${executionId}")

	return executionId
}

def stop() {
	debug("stop()")

	def result = parent.stopExecution(state.executionId, "Stop ${device.label}")

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
