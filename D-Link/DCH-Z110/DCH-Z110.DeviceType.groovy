/**
 *  Copyright (c) 2016 Tibor Jakab-Barthi
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
 *  mydlink™ Home Door/Window Sensor DCH-Z110
 *
 *  Author: Tibor Jakab-Barthi
 */

metadata {
	definition (name: "mydlink™ Home Door/Window Sensor", namespace: "jbt", author: "Tibor Jakab-Barthi") {
		capability "Battery"
		capability "Configuration"
		capability "Contact Sensor"
		capability "Illuminance Measurement"
		capability "Sensor"
		capability "Tamper Alert"
		capability "Temperature Measurement"

		fingerprint deviceId: "0x0701", inClusters: "0x59,0x5A,0x5E,0x72,0x73,0x7A,0x86,0x8F,0x98", outClusters:"0x20", manufacturer: "0108", model: "000E"
		
		attribute "sensorStateChangedDate", "string"
	}

	tiles(scale: 2) {
		multiAttributeTile(name: "contact", type: "generic", width: 6, height: 4) {
			tileAttribute("device.contact", key: "PRIMARY_CONTROL") {
				attributeState("open", label: '${name}', icon: "st.contact.contact.open", backgroundColor: "#ffa81e")
				attributeState("closed", label: '${name}', icon: "st.contact.contact.closed", backgroundColor: "#79b821")
			}
			tileAttribute("device.sensorStateChangedDate", key: "SECONDARY_CONTROL") {
				attributeState("default", label:'At ${currentValue}')
			}
		}
		valueTile("temperature", "device.temperature", width: 3, height: 2) {
			state("temperature", label: '${currentValue}°',
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
		valueTile("illuminance", "device.illuminance", width: 3, height: 2) {
			state("illuminance", label:'${currentValue}%',
				backgroundColors:[
							[value: 0, color: "#000000"],
							[value: 10, color: "#444400"],
							[value: 20, color: "#777700"],
							[value: 30, color: "#999900"],
							[value: 40, color: "#AAAA00"],
							[value: 50, color: "#BBBB00"],
							[value: 60, color: "#CCCC00"],
							[value: 70, color: "#DDDD00"],
							[value: 80, color: "#EEEE00"],
							[value: 90, color: "#FFFF00"]
					]
			)
		}
		valueTile("temperatureMain", "device.temperature", width: 1, height: 1) {
			state("temperature", label: '${currentValue}°', unit: "C", icon: "st.Weather.weather2", backgroundColor:"#EC6E05")
		}
		valueTile("battery", "device.battery", width: 6, height: 1) {
			state("battery", label:'${currentValue}% battery', icon: "st.Transportation.transportation6",
				backgroundColors:[
					[value: 10, color: "#FF0000"],
					[value: 15, color: "#FF7F00"],
					[value: 50, color: "#FF7F00"],
					[value: 55, color: "#79B821"],
					[value: 90, color: "#79B821"]
				]
			)
		}

		main("contact")
		details(["contact", "temperature", "illuminance", "battery"])
	}
}

def parse(String description) {
	log.debug "parse raw '$description'"

	def result = []
	if (description.startsWith("Err 106")) {
		if (state.sec) {
			log.debug description
		} else {
			result << createEvent(
				descriptionText: "This sensor failed to complete the network security key exchange. If you are unable to control it via SmartThings, you must remove it from your network and add it again.",
				eventType: "ALERT",
				name: "secureInclusion",
				value: "failed",
				isStateChange: true,
			)
		}
	} else if (description != "updated") {
		def cmd = zwave.parse(description)

		log.debug "zwave.parse returned '$cmd'"

		if (cmd instanceof physicalgraph.zwave.commands.multicmdv1.MultiCmdEncap) {
			// HACK: Until SmartThings implements MultiCmdEncap.
			log.debug "parse found MultiCmdEncap"
			result += parseMultiCmdEncapString(description)
		} else if (cmd) {
			result += zwaveEvent(cmd)
		}
	}

	log.debug "parsed '$description' to ${result.inspect()}"

	return result
}

def parseMultiCmdEncapString(description) {
	log.debug "parseMultiCmdEncapString($description)"

	def payload = []

	def payloadStart = description.indexOf("payload: ") + 9

	description.substring(payloadStart).tokenize(" ").eachWithIndex { it, index ->
		if (index > 2) {
			payload << Integer.parseInt("$it", 16)
		}
	}

	def events = parseMultiCmdEncapBytes(payload)

	return events
}

def parseMultiCmdEncapBytes(bytes) {
	log.debug "parseMultiCmdEncapBytes($bytes)"

	def results = []

	def zwDeviceId = "MultiCmdEncap" // Irrelevant for parse so can be anything.
	def bytesLastIndex = bytes.size() - 1
	def offset = 0

	def commandCount = getMessagePayloadByte(bytes, offset)

	offset++

	for (int commandIndex = 0; commandIndex < commandCount; commandIndex++) {
		def messageLength = getMessagePayloadByte(bytes, offset)

		def commandClassCode = getMessagePayloadByteAsHexString(bytes, offset + 1)
		def commandTypeCode = getMessagePayloadByteAsHexString(bytes, offset + 2)

        def payloadStartIndex = offset + 3
		def payloadEndIndex = offset + messageLength < bytes.size() - 1 ? offset + messageLength : bytes.size() - 1
		def payload = ""

		if (payloadStartIndex <= bytesLastIndex && payloadEndIndex <= bytesLastIndex) {
			for (int payloadIndex = payloadStartIndex; payloadIndex <= payloadEndIndex; payloadIndex++) {
				payload += getMessagePayloadByteAsHexString(bytes, payloadIndex) + " "
			}
		}

		def singleCommandDescription = "zw device: $zwDeviceId, command: $commandClassCode$commandTypeCode, payload: $payload"

		results += parse(singleCommandDescription)

		offset += messageLength + 1
	}

	return results
}

String getMessagePayloadByteAsHexString(payload, index) {
	return getMessagePayloadByte(payload, index).encodeAsHex().toUpperCase()
}

int getMessagePayloadByte(payload, index) {
	return payload[index] & 0xFF;
}

def updated() {
	def cmds = []
	if (!state.MSR) {
		cmds = [
			zwave.manufacturerSpecificV2.manufacturerSpecificGet().format(),
			"delay 1200",
			zwave.wakeUpV1.wakeUpNoMoreInformation().format()
		]
	} else if (!state.lastbat) {
		cmds = []
	} else {
		cmds = [zwave.wakeUpV1.wakeUpNoMoreInformation().format()]
	}

	cmds << configure()
	
	response(cmds)
}

def configure() {
	log.debug "configure()"
	delayBetween([
		zwave.manufacturerSpecificV2.manufacturerSpecificGet().format(),
		batteryGetCommand()
	], 6000)
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
	log.debug "zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport $cmd)"

	def results = []

	def map = [ name: "battery", unit: "%" ]
	if (cmd.batteryLevel == 0xFF) {
		map.value = 1
		map.descriptionText = "${device.displayName} has a low battery"
		map.isStateChange = true
	} else {
		map.value = cmd.batteryLevel
	}

	state.lastbat = now()

	results << createEvent(map)

	results << response(zwave.wakeUpV1.wakeUpNoMoreInformation())

	return results
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
	log.debug "zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport $cmd)"

	def results = []

	def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
	log.debug "msr: $msr"
	updateDataValue("MSR", msr)

	retypeBasedOnMSR()

	results << createEvent(descriptionText: "$device.displayName MSR: $msr", isStateChange: false)

	results << response(batteryGetCommand())

	return results
}

def zwaveEvent(physicalgraph.zwave.commands.multicmdv1.MultiCmdEncap cmd) {
	log.debug "zwaveEvent(physicalgraph.zwave.commands.multicmdv1.MultiCmdEncap $cmd)"
}

def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd) {
	log.debug "zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport $cmd)"

	def events = []
	def openCloseChanged = false

	if (cmd.notificationType == 0x06) {
		def currentValue = device.currentValue("contact")

		if (cmd.event == 0x16) {
			log.debug "open"
			events << createEvent(name: "contact", value: "open", descriptionText: "$device.displayName is open.", translatable: true)
			openCloseChanged = currentValue != "open"
		} else if (cmd.event == 0x17) {
			log.debug "closed"
			events << createEvent(name: "contact", value: "closed", descriptionText: "$device.displayName is closed.", translatable: true)
			openCloseChanged = currentValue != "closed"
		} else {
			log.debug "Unknown contact event '${cmd.event}'."
		}

		if (openCloseChanged) {
			def dateTime = new Date()
			def sensorStateChangedDate = dateTime.format("yyyy-MM-dd HH:mm:ss", location.timeZone)
			events << createEvent(name: "sensorStateChangedDate", value: sensorStateChangedDate, descriptionText: "$device.displayName open/close state changed at $sensorStateChangedDate.", translatable: true)
		}
	} else if (cmd.notificationType == 0x07) {
		if (cmd.event == 0x03) {
			log.debug "tamper"
			events << createEvent(name: "tamper", value: "detected", descriptionText: "$device.displayName tampered.", translatable: true)
		} else {
			log.debug "Unknown tamper event '${cmd.event}'."
		}
	} else {
		log.debug "Unknown notification type '${cmd.notificationType}'."
	}

	return events
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	log.debug "zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation $cmd)"

	def events = []

	def encapsulatedCommand = cmd.encapsulatedCommand()

	if (encapsulatedCommand) {
		log.debug "encapsulatedCommand $encapsulatedCommand"

		state.sec = 1

		if (encapsulatedCommand instanceof physicalgraph.zwave.commands.multicmdv1.MultiCmdEncap) {
			events += parseMultiCmdEncapBytes(cmd.commandByte)
		} else {
			events += zwaveEvent(encapsulatedCommand)
		}
	}

	return events
}

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd) {
	log.debug "zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport $cmd)"

	def events = []

	if (cmd.sensorType == 0x08) {
		if (cmd.sensorValue == 0xFF) {
			log.debug "tamper"
			events << createEvent(name: "tamper", value: "detected", descriptionText: "$device.displayName tampered.", translatable: true)
		} else {
			log.debug "Unknown tamper event '${cmd.sensorValue}'."
		}
	}
	else if (cmd.sensorType == 0x0A) {
		if (cmd.sensorValue == 0xFF) {
			log.debug "open"
			events << createEvent(name: "contact", value: "open", descriptionText: "$device.displayName is open.", translatable: true)
		} else if (cmd.sensorValue == 0x00) {
			log.debug "close"
			events << createEvent(name: "contact", value: "closed", descriptionText: "$device.displayName is closed.", translatable: true)
		} else {
			log.debug "Unknown contact event '${cmd.sensorValue}'."
		}
	} else {
		log.debug "Unknown sensor type '${cmd.sensorType}'."
	}

	return events
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd) {
	log.debug "zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport $cmd)"

	def events = []

	if (cmd.sensorType == 0x01) {
		def reportedTemperatureValue = cmd.scaledSensorValue
		def reportedTemperatureUnit = cmd.scale == 1 ? "F" : "C"

		def convertedTemperatureValue = convertTemperatureIfNeeded(reportedTemperatureValue, reportedTemperatureUnit, 2)

		def descriptionText = "$device.displayName temperature was $convertedTemperatureValue°" + getTemperatureScale() + "."

		events << createEvent(name: "temperature", value: convertedTemperatureValue, descriptionText: descriptionText, translatable: true)
	}
	else if (cmd.sensorType == 0x03) {
		def illuminanceValue = cmd.scaledSensorValue

		events << createEvent(name: "illuminance", value: illuminanceValue, descriptionText: "$device.displayName illuminance is $illuminanceValue.", translatable: true)
	}

	return events
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpNotification cmd) {
	log.debug "zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpNotification $cmd)"

	def results = []

	results << createEvent(descriptionText: "${device.displayName} woke up", isStateChange: false)

	if (!state.MSR) {
		results << zwave.wakeUpV2.wakeUpIntervalSet(seconds: 4 * 3600, nodeid: zwaveHubNodeId).format()
		results << zwave.manufacturerSpecificV2.manufacturerSpecificGet().format()
		results << "delay 1200"
	}

	if (!state.lastbat || now() - state.lastbat > 53 * 60 * 60 * 1000) {
		results << batteryGetCommand()
	} else {
		results << zwave.wakeUpV2.wakeUpNoMoreInformation().format()
	}

	return results
}

def batteryGetCommand() {
	def cmd = zwave.batteryV1.batteryGet()
	if (state.sec) {
		cmd = zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd)
	}
	cmd.format()
}

def retypeBasedOnMSR() {
	switch (state.MSR) {
		case "0108-0002-000E":
			log.debug "Changing device type to mydlink™ Home Door/Window Sensor"
			setDeviceType("mydlink™ Home Door/Window Sensor")
			break
	}
}
