/*
Name:		Esp8266BarcodeReader.ino
Created:	2018/03/06 8:42:33
Author:	min
*/

//#define DP3_ESP32

#include "Config.h"
#include "NetworkManager.h"
#include "TableViewManager.h"
#include "NormalOperationManager.h"
#include "CommonController.h"

#include <FS.h>

#include "HIDSelector.h"
#include "TxManager.h"
#include "FTDIAsync.h"
#include <cdcftdi.h>
#include <usbhub.h>
#include "LcdManager.h"

#ifdef dobogusinclude
#include <spi4teensy3.h>
#endif
#include <SPI.h>


NetworkManager net = NetworkManager();

StaticJsonBuffer<JSON_BUFFER_SIZE> JSONBuffer;



USB			Usb;
USBHub		Hub(&Usb);

// USB Hub and keyboard
USBHub     Hub2(&Usb);
HIDSelector    hid1(&Usb);
HIDSelector    hid2(&Usb);
HIDSelector    hid3(&Usb);
Max_LCD lcd(&Usb);
LcdManager scr(&lcd, BEEP_PIN);
HIDBoot<USB_HID_PROTOCOL_KEYBOARD>    HidKeyboard(&Usb);
CommonController *cc = NULL;
NormalOperationManager normOpMgr = NormalOperationManager();
TableViewWrapper * pTableOpMgr = NULL;

void updateConfig() {
	unsigned long timeout = millis() + 200;
	char* chBuf = (char*)malloc(JSON_POST_BUFFER_SIZE);
	char* chBuf2 = (char*)malloc(JSON_POST_BUFFER_SIZE);
	memset(chBuf, 0, JSON_POST_BUFFER_SIZE);
	memset(chBuf2, 0, JSON_POST_BUFFER_SIZE);
	int size = Serial.readBytesUntil(0x0d, chBuf, JSON_POST_BUFFER_SIZE);
	bool isReboot = false;
	if (size > 10) {
		memcpy(chBuf2, chBuf, JSON_POST_BUFFER_SIZE);
		JSONBuffer.clear();
		Serial.print("RECV:"); Serial.println(chBuf);
		JsonObject& parsed = JSONBuffer.parseObject(chBuf);
		if (parsed.success()) {
			if (parsed.containsKey("cmd")) {
				if (strcmp("format", parsed["cmd"].asString()) == 0) {
					Serial.println("Start SPIFFS format");
					SPIFFS.begin();
					SPIFFS.format();
					SPIFFS.end();
					Serial.println("format done.");
					Serial.println(F("{\"status\": 200, \"msg\": \"format done\"}"));
				}
				if (strcmp("ping", parsed["cmd"].asString()) == 0) {
					Serial.println(F("{\"status\": 200, \"msg\": \"pong\"}"));
				}
				if (strcmp("w", parsed["cmd"].asString()) == 0) {
					SPIFFS.begin();
					File file = SPIFFS.open(CONFIG_FILE, "w");
					if (file == NULL) {
						Serial.print("SPIFFS format");
						SPIFFS.format();
						Serial.println(" done.");
						file = SPIFFS.open(CONFIG_FILE, "w");
						if (file == NULL) {
							Serial.println(F("{\"status\": 500, \"msg\": \"Cannot format SPIFFS\"}"));
							return;
						}
					}
					file.print(chBuf2);
					file.close();

					Serial.println(F("{\"status\": 200, \"msg\": \"write done\"}"));
					isReboot = true;
				}
			}
		}
	}
	free(chBuf);
	free(chBuf2);
	if (isReboot) {
		delay(1000);
		ESP.reset();
	}
}


// the setup function runs once when you press reset or power the board
void setup() {
	Serial.begin(38400);
	Serial.setTimeout(200);
	Serial.println(F("Start"));


	if (Usb.Init() == -1)
		Serial.println(F("OSC did not start."));

	net.setupWiFi(&lcd);


	UsbDEBUGlvl = 0xff;
	Serial.println(F("USB host start"));
	hid1.init(); hid2.init(); hid3.init();

#ifndef DP3_ESP32
	//事前にロードしなくては音がでない場合がある為
	pinMode(BEEP_PWM_PIN, OUTPUT);
	analogWriteFreq(4000);
	analogWrite(BEEP_PWM_PIN, 128);
	delay(50);
	analogWrite(BEEP_PWM_PIN, 0);
	pinMode(BEEP_PWM_PIN, INPUT);
#endif


	//*/
	//入力フックの設定
	HIDSelector::setCommonController(&normOpMgr);

	lcd.clear();
	lcd.print("Connect USB ...");
	char chWait[] = { '-','\\', '|', '/' ,'-','\\','|','/' };
	while (1) {
		Usb.Task();
		if (Usb.getUsbTaskState() == USB_STATE_RUNNING) {
			lcd.clear();
			lcd.print("USB detected.");
			unsigned long limitMs = millis() + 3000;
			lcd.setCursor(0, 1);
			lcd.print("wait 3 sec.");
			for (int i = 0; i < 30; i++) {
				if (millis() > limitMs) { break; }
				delay(100);
				lcd.setCursor(12, 1);
				lcd.print(chWait[i % sizeof(chWait)]);
				Usb.Task();
			}
			break;
		}
		else {
			delay(500);
		}
	}

	//初期アクセス
	normOpMgr.post();
}



// the loop function runs over and over again until power down or reset
void loop() {
	Usb.Task();

	if (Serial.available() > 0) {
		updateConfig();
	}

	if (Usb.getUsbTaskState() != USB_STATE_RUNNING) { return; }

	scr.Task();
}
