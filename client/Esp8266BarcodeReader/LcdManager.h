// LcdManager.h

#ifndef _LCDMANAGER_h
#define _LCDMANAGER_h

#if defined(ARDUINO) && ARDUINO >= 100
	#include "Arduino.h"
#else
	#include "WProgram.h"
#endif
#include <max_LCD.h>
#include "HIDSelector.h"

/*
#define BEEP_SHORT 	pinMode(beepPin, OUTPUT);\
	analogWrite(beepPin, 40);\
	delay(100);\
	digitalWrite(beepPin, LOW);\
	pinMode(beepPin, INPUT);\

#define BEEP_LONG 	pinMode(beepPin, OUTPUT);\
	analogWrite(beepPin, 40);\
	delay(2000);\
	digitalWrite(beepPin, LOW);\
	pinMode(beepPin, INPUT);\
/*/
//ESP32Ç≈ÇÕBEEPÇÕñ¢é¿ëïÇÃà◊ÅAãÛÇ…ÅB
#define BEEP_SHORT
#define BEEP_LONG
//*/

struct ScreenParam {
	char stuff_code[BARCODE_BUFFER_SIZE];
	char place_code[BARCODE_BUFFER_SIZE];
	char barcode[BARCODE_BUFFER_SIZE];
	char op_code[BARCODE_BUFFER_SIZE];
	char qty[TENKEY_BUFFER_SIZE];
	uint32_t millis;
};

class LcdManager {
public:
	enum Status {
		BOOT, 
		REQ_STUFF_CODE,
		REQ_LOCATION_CODE,
		REQ_OPERATION_CODE,
		WAIT_BARCODE,
		WAIT_QUANTITY_INPUT,
		SENDING,
		SENDOK,
		ERR_TX_FAIL,
		ERR_NO_MEMORY,
		ERR_OTHER,
		NOOP
	};
	LcdManager(Max_LCD* max_lcd, uint8_t beepPinCode);
	void update(Status stat, uint32_t timeout_ms = 0, Status nextStatus = LcdManager::NOOP);
	void Task();
	void valueFixed(char* value);
	void onChangeTenkey(char* tenkey);

	static Status status;
	static ScreenParam param;
private:
	Max_LCD* lcd;
	Status _pre_status = LcdManager::NOOP;
	uint32_t limit_ms = 0;
	uint8_t beepPin = 0;
};

#endif

