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
definition(
		name: "${cloudConnectedDeviceName}",
		namespace: "jbt",
		author: "Tibor Jakab-Barthi",
		description: "Connect your ${cloudConnectedDeviceName} to SmartThings.",
		category: "My Apps",
		iconUrl: "https://wirelesssensortags.azurewebsites.net/img/WirelessSensorTags/WirelessSensorTags.square310.png",
		iconX2Url: "https://wirelesssensortags.azurewebsites.net/img/WirelessSensorTags/WirelessSensorTags.square310.png",
		singleInstance: true
)

preferences {
	page(
		name: "auth",
		title: "${cloudConnectedDeviceName}",
		nextPage: "",
		content: "authPage",
		uninstall: true,
		install: true
	)
}

mappings {
	path("/oauth/initialize") { 
		action: [
			GET: "oauthInitUrl"
		]
	}
	path("/oauth/callback") { 
		action: [
			GET: "callback"
		]
	}
}

// Cloud-Connected Device properties
def getCloudConnectedDeviceName()	{ "Wireless Sensor Tags" }
def getCloudApiEndpoint()			{ "https://www.mytaglist.com/ethClient.asmx/" }
def getCloudOauthAuthorizeUrl()		{ "https://www.mytaglist.com/oauth2/authorize.aspx" }
def getCloudOauthTokenUrl()			{ "https://www.mytaglist.com/oauth2/access_token.aspx" }
def getOauthClientId()				{ "TODO: Insert your OAuth Client ID" }
def getOauthClientSecret()			{ "TODO: Insert your OAuth Client Secret" }

// Service Manager properties
def getSmartThingsApiEndpoint()		{ return "https://graph.api.smartthings.com" }
def getSmartThingsApiServerUrl()	{ return getApiServerUrl() }
def getSmartThingsCallbackUrl()		{ "https://graph.api.smartthings.com/oauth/callback" }
def getSmartThingsRedirectUrl()		{ "${smartThingsApiEndpoint}/oauth/initialize?appId=${app.id}&access_token=${atomicState.accessToken}&apiServerUrl=${smartThingsApiServerUrl}" }

// Device specific methods
def getDeviceId(device) { 
	return device.slaveId;
}

def getDeviceName(device) { 
	return device.name;
}

// Utilities
def toQueryString(Map m) {
	return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

def authPage() {
	log.debug("WirelessSensorTags.SmartApp.authPage()")

	if (!atomicState.accessToken) {
		atomicState.accessToken = createAccessToken()
	}

	def description
	def uninstallAllowed = false
	def oauthTokenProvided = false

	if (atomicState.authToken) {
		description = "You are connected to your ${cloudConnectedDeviceName} account."
		uninstallAllowed = true
		oauthTokenProvided = true
	} else {
		description = "Tap to enter ${cloudConnectedDeviceName} credentials"
	}

	def redirectUrl = smartThingsRedirectUrl
	log.debug("WirelessSensorTags.SmartApp.RedirectUrl = ${redirectUrl}")

	// Get rid of next button until the user is actually authorised
	if (!oauthTokenProvided) {
		return dynamicPage(name: "auth", title: "Login", nextPage: "", uninstall: uninstallAllowed) {
			section(){
				paragraph "Tap below to log in to the ${cloudConnectedDeviceName} service and authorize SmartThings access."
				href url:redirectUrl, style:"embedded", required:true, title:"${cloudConnectedDeviceName}", description:description
			}
		}
	} else {
		def allDeviceNames = getDevices()
		log.debug("All device names: ${allDeviceNames}")

		return dynamicPage(name: "auth", title: "Select your Tag", uninstall: true) {
			section(""){
				paragraph "Tap below to see the list of tags available in your account and select the ones you want to connect to SmartThings."
				input(name: "selectedDeviceNames", title:"", type: "enum", required: true, multiple: true, description: "Tap to choose", metadata: [values: allDeviceNames])
			}
		}
	}
}

def oauthInitUrl() {
	log.debug("WirelessSensorTags.SmartApp.oauthInitUrl with callback: ${smartThingsCallbackUrl} ${oauthClientId}")

	atomicState.oauthInitState = UUID.randomUUID().toString()

	def oauthParams = [
			client_id: oauthClientId,
            state: atomicState.oauthInitState,
			redirect_uri: smartThingsCallbackUrl
	]

	redirect(location: "${cloudOauthAuthorizeUrl}?${toQueryString(oauthParams)}")
}

def callback() {
	log.debug("WirelessSensorTags.SmartApp.callback()>> params: $params, params.code ${params.code}")

	def code = params.code
	def oauthState = params.state

	if (oauthState == atomicState.oauthInitState){

		def tokenParams = [
			code: code,
			client_id: oauthClientId,
			client_secret: oauthClientSecret
		]

		def tokenUrl = "${cloudOauthTokenUrl}?${toQueryString(tokenParams)}"

		httpPost(uri: tokenUrl) { resp ->
			atomicState.refreshToken = resp.data.refresh_token
			atomicState.authToken = resp.data.access_token
			log.debug("swapped token: $resp.data")
			log.debug("atomicState.refreshToken: ${atomicState.refreshToken}")
			log.debug("atomicState.authToken: ${atomicState.authToken}")
		}

		if (atomicState.authToken) {
			success()
		} else {
			fail()
		}

	} else {
		log.error("callback() failed oauthState != atomicState.oauthInitState")
	}
}

def success() {
	def message = """
    <p>Your ${cloudConnectedDeviceName} Account is now connected to SmartThings!</p>
    <p>Tap 'Done' to finish setup.</p>
    """
	connectionStatus(message)
}

def fail() {
	def message = """
        <p>The connection could not be established!</p>
        <p>Tap 'Done' to return to the menu.</p>
    """
	connectionStatus(message)
}

def connectionStatus(message, redirectUrl = null) {
	def redirectHtml = ""
	if (redirectUrl) {
		redirectHtml = """
			<meta http-equiv="refresh" content="3; url=${redirectUrl}" />
		"""
	}

	def html = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=640">
<title>${cloudConnectedDeviceName} & SmartThings connection</title>
<style type="text/css">
        @font-face {
                font-family: 'Swiss 721 W01 Thin';
                src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot');
                src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot?#iefix') format('embedded-opentype'),
                         url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.woff') format('woff'),
                         url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.ttf') format('truetype'),
                         url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.svg#swis721_th_btthin') format('svg');
                font-weight: normal;
                font-style: normal;
        }
        @font-face {
                font-family: 'Swiss 721 W01 Light';
                src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot');
                src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot?#iefix') format('embedded-opentype'),
                         url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.woff') format('woff'),
                         url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.ttf') format('truetype'),
                         url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.svg#swis721_lt_btlight') format('svg');
                font-weight: normal;
                font-style: normal;
        }
        .container {
                width: 90%;
                padding: 4%;
                /*background: #eee;*/
                text-align: center;
        }
        img {
                vertical-align: middle;
        }
        p {
                font-size: 2.2em;
                font-family: 'Swiss 721 W01 Thin';
                text-align: center;
                color: #666666;
                padding: 0 40px;
                margin-bottom: 0;
        }
        span {
                font-family: 'Swiss 721 W01 Light';
        }
</style>
</head>
<body>
        <div class="container">
                <img src="https://wirelesssensortags.azurewebsites.net/img/WirelessSensorTags/WirelessSensorTags.square155.png" alt="cloud service icon" />
                <img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/connected-device-icn%402x.png" alt="connected device icon" />
                <img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/st-logo%402x.png" alt="SmartThings logo" />
                ${message}
        </div>
</body>
</html>
"""

	render contentType: 'text/html', data: html
}

def getDevices() {
	log.debug("WirelessSensorTags.SmartApp.getDevices(): Getting device list '${atomicState}'.")

	def allDevices = [:]
	def deviceNames = [:]

	try {
		def deviceListParams = [
				uri: cloudApiEndpoint,
				path: "GetTagList",
				headers: [
                	"Authorization": "Bearer ${atomicState.authToken}",
                    "Content-Type": "application/json"
				]
		]

		log.debug("HTTP request ${deviceListParams}")

		httpPost(deviceListParams) { resp ->
			if (resp.status == 200) {
				log.debug("resp.data ${resp.data}")

				resp.data.d.each { device ->
					log.debug("resp.data.device ${device}")

					if (device.alive == true) {
						def dni = [app.id, getDeviceId(device)].join('.')
						allDevices[dni] = device
						log.debug("allDevices ${dni} ${allDevices}")

						deviceNames[dni] = getDeviceName(device)
                    }
				}
			} else {
				log.debug("http status: ${resp.status}")
			}
		}
	} catch (groovyx.net.http.HttpResponseException e) {
        log.error("Exception polling devices: ${e}" + e.response.data.status)

        if (e.response && e.response.data && e.response.data.status && e.response.data.status.code == 14) {
            atomicState.action = "getDevices"
            log.debug("Refreshing your auth_token.")
            refreshAuthToken()
        }
    }

	atomicState.allDevices = allDevices
	atomicState.allDeviceNames = deviceNames
	log.debug("getDevices atomicState.allDevices ${dni} ${atomicState.allDevices}")

	return atomicState.allDeviceNames
}

private refreshAuthToken() {
	log.debug("WirelessSensorTags.SmartApp.refreshAuthToken()")

	if (!atomicState.refreshToken) {
		log.warn("Can not refresh OAuth token since there is no refreshToken stored.")
	} else {
		def refreshParams = [
				method: 'POST',
				uri: cloudOauthTokenUrl,
				query: [grant_type: 'refresh_token', code: "${atomicState.refreshToken}", client_id: oauthClientId],
		]

		log.debug(refreshParams)

		try {
			httpPost(refreshParams) { resp ->

				if (resp.status == 200) {
					log.debug("Token refreshed. Calling saved RestAction now.")

					log.debug(resp)

					if (resp.data) {
						log.debug("Response = ${resp.data}")

						atomicState.refreshToken = resp?.data?.refresh_token
						atomicState.authToken = resp?.data?.access_token

						log.debug("Refresh Token = ${atomicState.refreshToken}")
						log.debug("OAUTH Token = ${atomicState.authToken}")

						if (atomicState.action && atomicState.action != "") {
							log.debug("Executing next action: ${atomicState.action}")

							"${atomicState.action}"()

							atomicState.action = ""
						}
					}

					atomicState.action = ""
				}
			}
		} catch (groovyx.net.http.HttpResponseException e) {
			log.error("refreshAuthToken() >> Error: ${e}")

			def reAttemptPeriod = 300 // in sec
			if (e.statusCode != 401) { // this issue might come from exceed 20sec app execution, connectivity issue etc.
				runIn(reAttemptPeriod, "refreshAuthToken")
			} else if (e.statusCode == 401) { // unauthorized
				atomicState.reAttempt = atomicState.reAttempt + 1

				log.warn("reAttempt refreshAuthToken to try = ${atomicState.reAttempt}")

				if (atomicState.reAttempt <= 3) {
					runIn(reAttemptPeriod, "refreshAuthToken")
				} else {
					atomicState.reAttempt = 0
				}
			}
		}
	}
}

def installed() {
	log.debug("WirelessSensorTags.SmartApp.installed() settings: ${settings}")

	initialize()
}

def uninstalled() {
	log.debug("WirelessSensorTags.SmartApp.uninstalled()")

    removeChildDevices(getChildDevices())
}

private removeChildDevices(childDevices) {
	log.debug("WirelessSensorTags.SmartApp.removeChildDevices(${childDevices})")

    childDevices.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

def updated() {
	log.debug("WirelessSensorTags.SmartApp.updated() settings: ${settings}")

	unsubscribe()

	initialize()
}

def initialize() {
	log.debug("WirelessSensorTags.SmartApp.initialize()")

	def devices = selectedDeviceNames.collect { dni ->
		def remoteDevice = atomicState.allDevices[dni]

		if (!remoteDevice) {
			log.debug("initialize ${atomicState.allDevices}")

			def errorMessage = "Remote device '$dni' not found."
			log.error (errorMessage)

			throw new GroovyRuntimeException(errorMessage)
		}

		log.debug("remoteDevice $remoteDevice")

		def deviceName = getDeviceName(remoteDevice)
		def deviceId = getDeviceId(remoteDevice)

		def virtualDevice = getChildDevice(dni)

		if (!virtualDevice) {
			def deviceTypeName = "Wireless Sensor Tag"

			log.debug("Creating new '$deviceTypeName' device '$deviceName' with id '$dni'.")

			virtualDevice = addChildDevice(app.namespace, deviceTypeName, dni, null, ["name": deviceId, "label": deviceName, "completedSetup": true])

			log.debug("virtualDevice ${virtualDevice}")

			log.debug("Created '$deviceName' with network id '$dni'.")
		} else {
			log.debug("Found ${virtualDevice.name} with network id '$dni' already exists.")
		}

		return virtualDevice
	}

	log.debug("User selected ${devices.size()} devices.")

	def delete = getAllChildDevices()

	if (selectedDeviceNames) {
		log.debug("Delete devices not selected by user.")

		delete = getChildDevices().findAll { !selectedDeviceNames.contains(it.deviceNetworkId) }
	}

	log.warn("Delete: ${delete}, deleting ${delete.size()} devices.")

	delete.each { deleteChildDevice(it.deviceNetworkId) }

	atomicState.timeSendPush = null
	atomicState.reAttempt = 0

	pollHandler()

	// Automatically update devices status every 5 mins.
	runEvery5Minutes("poll")
}

def pollHandler() {
	log.debug("WirelessSensorTags.SmartApp.pollHandler()")
}

void poll() {
	try {
		log.debug("WirelessSensorTags.SmartApp.poll()")

		def devices = getChildDevices()

		log.debug("poll devices: ${devices}")

		devices.each { 
			try {
				pollChild(it)
			} catch (Exception e) {
				log.error("Exception polling device ${it.name}: ${e}")
			}
		}
	} catch (Exception e) {
        log.error("Exception polling devices: ${e}")
    }

	//runIn(30, poll)
}

void pollChild(childDevice) {
	log.debug("WirelessSensorTags.SmartApp.pollChild(${childDevice}; name: ${childDevice.name}; label: ${childDevice.label})")

	def deviceUpdateParams = [
		uri: cloudApiEndpoint,
		path: "PingTag",
		headers: [
			"Authorization": "Bearer ${atomicState.authToken}",
			"Content-Type": "application/json"
		],
		body: "{id:${childDevice.name}}"
	]

	log.debug("HTTP request ${deviceUpdateParams}")

	try {
		httpPost(deviceUpdateParams) { resp ->
			if (resp.status == 200) {
				log.debug("resp.data ${resp.data}")

				def deviceData = resp.data.d

				log.debug("resp.data.deviceData ${deviceData}")

				def data = [
					battery: deviceData.batteryRemaining,
					humidity: deviceData.cap,
					temperature: deviceData.temperature
				]

				log.debug("event ${data}")

				childDevice.generateEvent(data)

				return data;
			} else {
				log.debug("http status: ${resp.status}")
			}
		}
	} catch (groovyx.net.http.HttpResponseException e) {
        log.error("Exception polling device: ${e}")
        if (e.response && e.response.data && e.response.data.status && e.response.data.status.code == 14) {
            atomicState.action = "pollChild"
            log.debug("Refreshing your auth_token!")
            refreshAuthToken()
        }
    }
}
