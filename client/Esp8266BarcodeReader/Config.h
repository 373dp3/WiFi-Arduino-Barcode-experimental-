// Config.h

#ifndef _CONFIG_h
#define _CONFIG_h

#if defined(ARDUINO) && ARDUINO >= 100
	#include "arduino.h"
#else
	#include "WProgram.h"
#endif

#ifdef DP3_ESP32
#include <WiFi.h>
#include <esp32-hal-ledc.h>
#else
//ESP8266だと仮定する。
#include <ESP8266WiFi.h>
#include <ESP8266HTTPClient.h>
//SS P5, INT P2
#endif

#include <ArduinoJson.h>
#include <max_LCD.h>


#ifdef DP3_ESP32
#define LCD_LED_PIN	(2)
#else
#define LCD_LED_PIN	(16)
#endif

//BEEP_PINは未接続の為、使用していないピンをダミーで
#define BEEP_PIN	(33)
#define CONTLAST_ANALOG_WRITE_PIN	(12)
#define LCD_LED_K_PIN	(14)

//ESP02 BEEP
#define BEEP_PWM_PIN	(4)
#define BEEP_PWM_FREQ	(4000)

#define JSON_BUFFER_SIZE	(5120)
extern StaticJsonBuffer<JSON_BUFFER_SIZE> JSONBuffer;

#define JSON_POST_BUFFER_SIZE	(512)

#endif

