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
 */
definition(
		name: "TaHoma® by Somfy",
		namespace: "jbt",
		author: "Tibor Jakab-Barthi",
		description: "Connect your TaHoma® system to SmartThings.",
		category: "My Apps",
		iconUrl: "http://tahomabysomfyapi.azurewebsites.net/img/TaHomaBySomfy.square150.png",
		iconX2Url: "http://tahomabysomfyapi.azurewebsites.net/img/TaHomaBySomfy.square300.png",
		singleInstance: true
)

preferences {
	page(name: "loginPage", title: "Log in with your TaHoma® credentials", nextPage: "settingsPage", uninstall: true) {
		section  {
			input(name: "username", type: "text", title: "Username", required: true, displayDuringSetup: true)
			input(name: "password", type: "password", title: "Password", required: true, displayDuringSetup: true)
		}
	}

	page(name: "settingsPage")
}

def settingsPage() {
	debug("settingsPage()")

	def rollerShutterNames = getRollerShutterNames()

	dynamicPage(name: "settingsPage", title: "", install: true, uninstall: true) {
		section ("TaHoma® devices to control") {
			input(name: "selectedRollerShutterNames", type: "enum", title: "Roller Shutters", description: "Tap to choose", required: true, multiple: true, metadata: [values: rollerShutterNames], displayDuringSetup: true)
		}

		section("General") {
			input "debugMode", "bool", title: "Enable debug logging", defaultValue: false, displayDuringSetup: true
		}
	}
}

// Cloud-Connected Device properties
def getAuthorizationHeaderValue() {
	def authorizationToken = "${settings.username}:${settings.password}".bytes.encodeBase64()

	def authorizationHeaderValue = "Basic $authorizationToken"

	return authorizationHeaderValue
}

def getCloudApiEndpoint() {
	"https://tahomabysomfyapi.azurewebsites.net/api/v1/" 
}

// Device specific methods
def getDeviceId(device) { 
	return device.Id;
}

def getDeviceName(device) { 
	return device.Name;
}

def installed() {
	debug("installed(): ${settings}")

	atomicState.installedAt = now()

	updateRollerShutters()
	initialize()
}

def uninstalled() {
	debug("uninstalled()")

	if (getChildDevices()) {
		removeChildDevices(getChildDevices())
	}
}

def updated() {
	debug("updated(): ${settings}")

	unsubscribe()

	updateRollerShutters()
	initialize()
}

def debug(message) {
	if (settings.debugMode) {
		log.debug "SA: $message"
	}
}

def initialize() {
	debug("initialize()")

	def selectedRollerShutters = selectedRollerShutterNames.each { dni ->
		def rollerShutter = atomicState.rollerShutters[dni]

		if (!rollerShutter) {
			debug("initialize: rollerShutters: ${atomicState.rollerShutters}")

			def errorMessage = "Roller Shutter '$dni' not found."
			log.error errorMessage

			throw new GroovyRuntimeException(errorMessage)
		}

		debug("rollerShutter $rollerShutter")

		def deviceName = getDeviceName(rollerShutter)
		def deviceId = getDeviceId(rollerShutter)

		def virtualDevice = getChildDevice(dni)

		if (virtualDevice) {
			debug("Found ${virtualDevice.name} with network id '$dni' already exists.")
		} else {
			def deviceTypeName = "TaHoma Roller Shutter"

			debug("Creating new '$deviceTypeName' device '$deviceName' with id '$dni'.")

			virtualDevice = addChildDevice(app.namespace, deviceTypeName, dni, null, ["name": deviceId, "label": deviceName, "completedSetup": true])

			debug("virtualDevice ${virtualDevice}")

			debug("Created '$deviceName' with network id '$dni'.")
		}

		return virtualDevice
	}

	debug("User selected ${selectedRollerShutters.size()} Roller Shutters.")

	def delete

	if (selectedRollerShutterNames) {
		debug("Delete Roller Shutters not selected by user.")

		delete = getChildDevices().findAll { !selectedRollerShutterNames.contains(it.deviceNetworkId) }
	} else {
		delete = getAllChildDevices()
	}

	log.warn "Delete: ${delete}, deleting ${delete.size()} Roller Shutters."

	delete.each { deleteChildDevice(it.deviceNetworkId) }

	//pollHandler()

	// Automatically update devices status every 5 mins.
	//runEvery5Minutes("poll")
	//runIn(30, poll)
}

def removeChildDevices(childDevices) {
	debug("removeChildDevices($childDevices)")

    childDevices.each {
		try {
			deleteChildDevice(it.deviceNetworkId)
		} catch (Exception e) {
			debug("removeChildDevices ${it.deviceNetworkId} $e")
		}
    }
}

def getRollerShutterNames() {
	debug("getRollerShutterNames()")

	updateRollerShutters()

	def rollerShutterNames = [:]

	atomicState.rollerShutters.each { dni, rollerShutter ->
		rollerShutterNames[dni] = getDeviceName(rollerShutter)
	}

	debug("rollerShutterNames $rollerShutterNames")

	return rollerShutterNames
}

def updateRollerShutters() {
	debug("updateRollerShutters()")

	def requestParams = [
		method: 'GET',
		uri: cloudApiEndpoint,
		path: 'RollerShutters/List',
		headers: [
			'Authorization': getAuthorizationHeaderValue()
		]
	]

	debug("$requestParams")

	try {
		httpGet(requestParams) { resp ->
			if (resp.status == 200 && resp.data) {
				debug "updateRollerShutters(): resp.data ${resp.data}"

				def rollerShutters = [:]

				resp.data.each { rollerShutter ->
					def dni = [app.id, getDeviceId(rollerShutter)].join('.')

					rollerShutters[dni] = rollerShutter
				}

				atomicState.authorizationFailed = false
				atomicState.rollerShutters = rollerShutters
				atomicState.rollerShuttersUpdatedAt = now()
			}
			else {
				log.error "updateRollerShutters(): Failed: ${resp.status}"
			}
		}
	} catch (groovyx.net.http.HttpResponseException e) {
		log.error "updateRollerShutters(): Error: ${e}"

		if (e.statusCode == 401) {
			atomicState.authorizationFailed = true
		}
	}
}

def executeRollerShutterCommand(commandId, rollerShutterId) {
	debug("executeRollerShutterCommand($commandId, $rollerShutterId)")

	def requestParams = [
		method: 'POST',
		uri: cloudApiEndpoint,
		path: "RollerShutters/$commandId",
		headers: [
			'Authorization': getAuthorizationHeaderValue()
		],
		body: [
			'RollerShutterId': rollerShutterId,
		]
	]

	debug("$requestParams")

	def executionId

	try {
		httpPostJson(requestParams) { resp ->
			if (resp.status == 200 && resp.data) {
				debug "executeRollerShutterCommand(): resp.data ${resp.data}"

				executionId = resp.data.ExecutionId

				atomicState.authorizationFailed = false
			}
			else {
				log.error "executeRollerShutterCommand(): Failed: ${resp.status}"
			}
		}
	} catch (groovyx.net.http.HttpResponseException e) {
		log.error "executeRollerShutterCommand(): Error: ${e}"

		if (e.statusCode == 401) {
			atomicState.authorizationFailed = true
		}
	}

	return executionId
}

def stopExecution(executionId) {
	debug("stopExecution($executionId)")

	def requestParams = [
		method: 'DELETE',
		uri: cloudApiEndpoint,
		path: "RollerShutters/Stop/$executionId",
		headers: [
			'Authorization': getAuthorizationHeaderValue()
		],
	]

	debug("$requestParams")

	def result

	try {
		httpDelete(requestParams) { resp ->
			result = resp.status

			atomicState.authorizationFailed = false
		}
	} catch (groovyx.net.http.HttpResponseException e) {
		log.error "stopExecution(): Error: ${e}"

		result = e

		if (e.statusCode == 401) {
			atomicState.authorizationFailed = true
		}
	}

	return result
}

def close(rollerShutterId) {
	debug("close($rollerShutterId)")

	def executionId = executeRollerShutterCommand("Close", rollerShutterId)

	return executionId
}

def identify(rollerShutterId) {
	debug("identify($rollerShutterId)")

	def executionId = executeRollerShutterCommand("Identify", rollerShutterId)

	return executionId
}

def open(rollerShutterId) {
	debug("open($rollerShutterId)")

	def executionId = executeRollerShutterCommand("Open", rollerShutterId)

	return executionId
}

def presetPosition(rollerShutterId) {
	debug("presetPosition($rollerShutterId)")

	def executionId = executeRollerShutterCommand("My", rollerShutterId)

	return executionId
}

def stop(executionId) {
	debug("stop($executionId)")

	stopExecution(executionId)
}
