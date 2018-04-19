/**
 *  Copyright (c) 2016-2018 Tibor Jakab-Barthi
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

		section("General") {
			input(name: "debugMode", type: "bool", title: "Enable debug logging", defaultValue: false, displayDuringSetup: true)
		}
	}

	page(name: "settingsPage")
}

def settingsPage() {
	debug("settingsPage()")

	setDefaultValues()

	updateDevices()

	def interiorRollerBlindNames = getInteriorRollerBlindNames()
	def rollerShutterNames = getRollerShutterNames()

	dynamicPage(name: "settingsPage", title: "", install: true, uninstall: true) {
		section ("TaHoma® devices to control", hideWhenEmpty: true) {
			input(name: "selectedInteriorRollerBlindNames", type: "enum", title: "Interior Roller Blinds", description: "Tap to choose", required: false, multiple: true, metadata: [values: interiorRollerBlindNames], displayDuringSetup: true)
			input(name: "selectedRollerShutterNames", type: "enum", title: "Roller Shutters", description: "Tap to choose", required: false, multiple: true, metadata: [values: rollerShutterNames], displayDuringSetup: true)
			input(name: "selectedSwitchNames", type: "enum", title: "Switches", description: "Tap to choose", required: false, multiple: true, metadata: [values: switchNames], displayDuringSetup: true)
		}
	}
}

// Cloud-Connected Device properties
def getCloudApiEndpoint() {
	"https://www.tahomalink.com/enduser-mobile-web/enduserAPI/" 
}

def getSmartAppVersion() {
	"1.2.20180419" 
}

// Device specific methods
def getDeviceId(device) { 
	return device.deviceURL;
}

def getDeviceName(device) { 
	return device.label;
}

def installed() {
	debug("installed(): ${settings}")

	atomicState.installedAt = now()

	updateDevices()

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

	updateDevices()

	initialize()
}

def debug(message) {
	if (settings.debugMode) {
		log.debug("SA $smartAppVersion: $message")
	}
}

def setDefaultValues() {
	debug("setDefaultValues()")

	def minute = 60 * 1000 // seconds * milliseconds

	atomicState.authorizationFailed = false
	atomicState.authorizationHeaderValue = ""
	atomicState.authorizationRetries = 1
	atomicState.authorizationRetriesRemaining = atomicState.authorizationRetries
	atomicState.authorizationTimeoutInMilliseconds = 1 * minute
	atomicState.authorizedAt = 0

	debug("setDefaultValues: atomicState: $atomicState")
}

def initialize() {
	debug("initialize()")

	setDefaultValues();

	def selectedDeviceCount = processSelectedInteriorRollerBlinds()
	selectedDeviceCount += processSelectedRollerShutters()
	selectedDeviceCount += processSelectedSwitches()

	if (selectedDeviceCount == 0){
		log.error("No devices were selected.")
	}

	deleteUnselectedDevices()
}

def processSelectedInteriorRollerBlinds() {
	debug("processSelectedInteriorRollerBlinds()")

	if (!settings.selectedInteriorRollerBlindNames){
		settings.selectedInteriorRollerBlindNames = []
	}

	def selectedInteriorRollerBlinds = settings.selectedInteriorRollerBlindNames.each { dni ->
		def interiorRollerBlind = atomicState.interiorRollerBlinds[dni]

		if (!interiorRollerBlind) {
			debug("initialize: interiorRollerBlinds: ${atomicState.interiorRollerBlinds}")

			def errorMessage = "Interior Roller Blind '$dni' not found."
			log.error(errorMessage)

			throw new org.json.JSONException(errorMessage)
		}

		debug("interiorRollerBlind $interiorRollerBlind")

		def deviceName = getDeviceName(interiorRollerBlind)
		def deviceId = getDeviceId(interiorRollerBlind)

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

	debug("User selected ${selectedInteriorRollerBlinds.size()} Interior Roller Blinds.")

	return selectedInteriorRollerBlinds.size()
}

def processSelectedRollerShutters() {
	debug("processSelectedRollerShutters()")

	if (!settings.selectedRollerShutterNames){
		settings.selectedRollerShutterNames = []
	}

	def selectedRollerShutters = selectedRollerShutterNames.each { dni ->
		def rollerShutter = atomicState.rollerShutters[dni]

		if (!rollerShutter) {
			debug("initialize: rollerShutters: ${atomicState.rollerShutters}")

			def errorMessage = "Roller Shutter '$dni' not found."
			log.error(errorMessage)

			throw new org.json.JSONException(errorMessage)
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

	return selectedRollerShutters.size()
}

def processSelectedSwitches() {
	debug("processSelectedSwitches()")

	if (!settings.selectedSwitchNames){
		settings.selectedSwitchNames = []
	}

	def selectedSwitches = selectedSwitchNames.each { dni ->
		def currentSwitch = atomicState.switches[dni]

		if (!currentSwitch) {
			debug("initialize: switches: ${atomicState.switches}")

			def errorMessage = "Switch '$dni' not found."
			log.error(errorMessage)

			throw new org.json.JSONException(errorMessage)
		}

		debug("switch $currentSwitch")

		def deviceName = getDeviceName(currentSwitch)
		def deviceId = getDeviceId(currentSwitch)

		def virtualDevice = getChildDevice(dni)

		if (virtualDevice) {
			debug("Found ${virtualDevice.name} with network id '$dni' already exists.")
		} else {
			def deviceTypeName = "TaHoma Switch"

			debug("Creating new '$deviceTypeName' device '$deviceName' with id '$dni'.")

			virtualDevice = addChildDevice(app.namespace, deviceTypeName, dni, null, ["name": deviceId, "label": deviceName, "completedSetup": true])

			debug("virtualDevice ${virtualDevice}")

			debug("Created '$deviceName' with network id '$dni'.")
		}

		return virtualDevice
	}

	debug("User selected ${selectedSwitches.size()} switches.")

	return selectedSwitches.size()
}

def deleteUnselectedDevices() {
	debug("deleteUnselectedDevices()")

	def delete

	if ((settings.selectedInteriorRollerBlindNames && settings.selectedInteriorRollerBlindNames.size() > 0) || 
			(settings.selectedRollerShutterNames && settings.selectedRollerShutterNames.size() > 0) || 
			(settings.selectedSwitchNames && settings.selectedSwitchNames.size() > 0)) {
		debug("Delete devices not selected by user.")

		delete = getChildDevices().findAll { 
			!settings.selectedInteriorRollerBlindNames.contains(it.deviceNetworkId) && 
			!settings.selectedRollerShutterNames.contains(it.deviceNetworkId) && 
			!settings.selectedSwitchNames.contains(it.deviceNetworkId) 
		}
	} else {
		delete = getAllChildDevices()
	}

	log.warn("Delete: ${delete}, deleting ${delete.size()} devices.")

	delete.each { deleteChildDevice(it.deviceNetworkId) }
}

def getAuthorizationHeaderValue() {
	debug("getAuthorizationHeaderValue()")

	if (atomicState.authorizationHeaderValue == "" || atomicState.authorizedAt < now() - atomicState.authorizationTimeoutInMilliseconds) {
		debug("getAuthorizationHeaderValue: Authorizing with server.")

		atomicState.authorizationHeaderValue = getAuthorizationHeaderValueCore()
	} else {
		debug("getAuthorizationHeaderValue: Using cached token.")
	}

	return atomicState.authorizationHeaderValue
}

def getAuthorizationHeaderValueCore() {
	debug("getAuthorizationHeaderValueCore()")

	atomicState.authorizationFailed = false
	atomicState.authorizationHeaderValue = ""

	def requestParams = [
		method: 'POST',
		uri: cloudApiEndpoint,
		path: 'login',
		headers: [
			'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
		]
	]

	debug("$requestParams")

	requestParams.body = [
		'userId':	settings.username,
		'userPassword':	settings.password
	]

	try {
		httpPost(requestParams) { resp ->
			if (resp.status == 200) {
				debug("getAuthorizationHeaderValueCore(): resp.data ${resp.data}")

				if (resp.data && resp.data.success == true) {
					def cookieHeader = resp.headers.'Set-Cookie'

					if (cookieHeader.contains('JSESSIONID')) {
						def cookieParts = cookieHeader.split(';')
						if (cookieParts.length > 0) {
							atomicState.authorizationHeaderValue = cookieParts[0]

							debug("getAuthorizationHeaderValueCore(): authorizationHeaderValue ${atomicState.authorizationHeaderValue}")

							atomicState.authorizationFailed = false
							atomicState.authorizationRetriesRemaining = atomicState.authorizationRetries
							atomicState.authorizedAt = now()
						}
					}
				}
			}
			else {
				log.error("getAuthorizationHeaderValueCore(): Failed: ${resp.status}")
			}
		}
	} catch (groovyx.net.http.HttpResponseException e) {
		log.error("getAuthorizationHeaderValueCore(): Error: ${e}")

		atomicState.authorizationHeaderValue = ""

		if (e.statusCode == 401) {
			if (atomicState.authorizationRetriesRemaining > 0) {
				debug("Retry authorization $atomicState.authorizationRetriesRemaining time(s).")

				atomicState.authorizationRetriesRemaining = atomicState.authorizationRetriesRemaining - 1

				atomicState.authorizationHeaderValue = getAuthorizationHeaderValueCore()
			} else {
				atomicState.authorizationFailed = true
			}
		}
	}

	return atomicState.authorizationHeaderValue
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

def getInteriorRollerBlindNames() {
	debug("getInteriorRollerBlindNames()")

	def interiorRollerBlindNames = [:]

	atomicState.interiorRollerBlinds.each { dni, interiorRollerBlind ->
		interiorRollerBlindNames[dni] = getDeviceName(interiorRollerBlind)
	}

	debug("interiorRollerBlindNames $interiorRollerBlindNames")

	return interiorRollerBlindNames
}

def getRollerShutterNames() {
	debug("getRollerShutterNames()")

	def rollerShutterNames = [:]

	atomicState.rollerShutters.each { dni, rollerShutter ->
		rollerShutterNames[dni] = getDeviceName(rollerShutter)
	}

	debug("rollerShutterNames $rollerShutterNames")

	return rollerShutterNames
}

def getSwitchNames() {
	debug("getSwitchNames()")

	def switchNames = [:]

	atomicState.switches.each { dni, currentSwitch ->
		switchNames[dni] = getDeviceName(currentSwitch)
	}

	debug("switchNames $switchNames")

	return switchNames
}

def updateDevices() {
	debug("updateDevices()")

	def requestParams = [
		method: 'GET',
		uri: cloudApiEndpoint,
		path: 'setup',
		headers: [
			'Cookie': getAuthorizationHeaderValue()
		]
	]

	debug("$requestParams")

	try {
		httpGet(requestParams) { resp ->
			debug("updateDevices(): resp.data ${resp.data}")

			if (resp.status == 200 && resp.data) {
				def rollerShutters = [:]
				def interiorRollerBlinds = [:]
				def switches = [:]

				if (resp.data.devices) {
					resp.data.devices.each { device ->
						if (device.controllableName == 'rts:BlindRTSComponent') {
							def dni = [app.id, getDeviceId(device)].join('.')

							interiorRollerBlinds[dni] = device
						} else if (device.controllableName == 'rts:RollerShutterRTSComponent') {
							def dni = [app.id, getDeviceId(device)].join('.')

							rollerShutters[dni] = device
						} else if (device.controllableName == 'rts:OnOffRTSComponent') {
							def dni = [app.id, getDeviceId(device)].join('.')

							switches[dni] = device
						}
					}
				}

				debug("updateDevices(): interiorRollerBlinds ${interiorRollerBlinds}")
				debug("updateDevices(): rollerShutters ${rollerShutters}")
				debug("updateDevices(): switches ${switches}")

				atomicState.interiorRollerBlinds = interiorRollerBlinds
				atomicState.rollerShutters = rollerShutters
				atomicState.switches = switches
				atomicState.devicesUpdatedAt = now()
			}
			else {
				log.error("updateDevices(): Failed: ${resp.status}")
			}
		}
	} catch (groovyx.net.http.HttpResponseException e) {
		log.error("updateDevices(): Error: ${e}")

		if (e.statusCode == 401) {
			atomicState.authorizationFailed = true
		}
	}
}

def executeRollerShutterCommand(commandId, rollerShutterId, label) {
	debug("executeRollerShutterCommand($commandId, $rollerShutterId, $label)")

	label = "$label ($rollerShutterId; SmartThings $smartAppVersion)";

	def body = "{\"label\":\"$label\",\"actions\":[{\"deviceURL\":\"$rollerShutterId\",\"commands\":[{\"name\":\"$commandId\",\"parameters\":[]}]}]}"

	def requestParams = [
		method: 'POST',
		uri: cloudApiEndpoint,
		path: 'exec/apply',
		headers: [
			'Cookie': getAuthorizationHeaderValue()
		],
		body: body
	]

	debug("requestParams: $requestParams")

	def executionId

	try {
		httpPostJson(requestParams) { resp ->
			if (resp.status == 200 && resp.data) {
				debug("executeRollerShutterCommand(): resp.data ${resp.data}")

				executionId = resp.data.execId
			}
			else {
				log.error("executeRollerShutterCommand(): Failed: ${resp.status}")
			}
		}
	} catch (groovyx.net.http.HttpResponseException e) {
		log.error("executeRollerShutterCommand(): Error: ${e}")

		if (e.statusCode == 401) {
			atomicState.authorizationFailed = true
		}
	}

	debug("executeRollerShutterCommand: $commandId done: executionId: $executionId")

	return executionId
}

def executeSwitchCommand(commandId, switchId, label) {
	debug("executeSwitchCommand($commandId, $switchId, $label)")

	label = "$label ($switchId; SmartThings $smartAppVersion)";

	def body = "{\"label\":\"$label\",\"actions\":[{\"deviceURL\":\"$switchId\",\"commands\":[{\"name\":\"$commandId\",\"parameters\":[]}]}]}"

	def requestParams = [
		method: 'POST',
		uri: cloudApiEndpoint,
		path: 'exec/apply',
		headers: [
			'Cookie': getAuthorizationHeaderValue()
		],
		body: body
	]

	debug("requestParams: $requestParams")

	def executionId

	try {
		httpPostJson(requestParams) { resp ->
			if (resp.status == 200 && resp.data) {
				debug("executeSwitchCommand(): resp.data ${resp.data}")

				executionId = resp.data.execId
			}
			else {
				log.error("executeSwitchCommand(): Failed: ${resp.status}")
			}
		}
	} catch (groovyx.net.http.HttpResponseException e) {
		log.error("executeSwitchCommand(): Error: ${e}")

		if (e.statusCode == 401) {
			atomicState.authorizationFailed = true
		}
	}

	debug("executeSwitchCommand: $commandId done: executionId: $executionId")

	return executionId
}

def stopExecution(executionId, label) {
	debug("stopExecution($executionId, $label)")

	def requestParams = [
		method: 'DELETE',
		uri: cloudApiEndpoint,
		path: "exec/current/setup/$executionId",
		headers: [
			'Cookie': getAuthorizationHeaderValue()
		],
	]

	debug("$requestParams")

	def result

	try {
		httpDelete(requestParams) { resp ->
			result = resp.status
		}
	} catch (groovyx.net.http.HttpResponseException e) {
		log.error("stopExecution(): Error: ${e}")

		result = e

		if (e.statusCode == 401) {
			atomicState.authorizationFailed = true
		}
	}

	return result
}

def close(rollerShutterId, label) {
	debug("close($rollerShutterId, $label)")

	def executionId = executeRollerShutterCommand("close", rollerShutterId, "Close $label")

	return executionId
}

def identify(rollerShutterId, label) {
	debug("identify($rollerShutterId, $label)")

	def executionId = executeRollerShutterCommand("identify", rollerShutterId, "Identify $label")

	return executionId
}

def open(rollerShutterId, label) {
	debug("open($rollerShutterId, $label)")

	def executionId = executeRollerShutterCommand("open", rollerShutterId, "Open $label")

	return executionId
}

def presetPosition(rollerShutterId, label) {
	debug("presetPosition($rollerShutterId, $label)")

	def executionId = executeRollerShutterCommand("my", rollerShutterId, "My position $label")

	return executionId
}

def stop(executionId, label) {
	debug("stop($executionId, $label)")

	stopExecution(executionId, "Stop $label")
}

def switchOff(switchId, label) {
	debug("switchOff($switchId, $label)")

	def executionId = executeSwitchCommand("off", switchId, "$label off")

	return executionId
}

def switchOn(switchId, label) {
	debug("switchOn($switchId, $label)")

	def executionId = executeSwitchCommand("on", switchId, "$label on")

	return executionId
}
