// 
// 
// 

#include "CommonController.h"


void CommonController::beepCheck(JsonObject* pParsed) {
	if (pParsed == NULL) { return; }
	if (pParsed->containsKey("beep") == false) { return; }

	//ˆÈ‰ºABEEP‚ÉŠÖ‚·‚éî•ñ‚ª‘¶İ‚·‚éê‡‚Ìˆ—
	JsonObject& obj = (*pParsed)["beep"].asObject();

	int hz = 399;
	int value = 511;
	if (obj.containsKey("hz")) {
		hz = strtol(obj["hz"].asString(), 0, 10);
	}
	if (obj.containsKey("val")) {
		value = strtol(obj["val"].asString(), 0, 10);
	}
	char buf[512];
	obj.printTo(buf);
	Serial.print("beep: ");
	Serial.println(buf);

	//—áŠO’l‘Î‰
	if (value < 0) { value = 0; }
	if (value > 1024) { value = 1024; }
	if (hz < 100) { hz = 100; }
	if (hz > 10000) { hz = 10000; }

	//ÀÛ‚Ì‹ì“®ˆ—
#ifndef DP3_ESP32
	pinMode(BEEP_PWM_PIN, OUTPUT);
	analogWriteFreq(hz);
	analogWrite(BEEP_PWM_PIN, 0);

	JsonArray& ary = obj["ptn"].asArray();
	for (int i = 0; i < ary.size(); i++) {
		if ((i % 2) == 0) {
			analogWrite(BEEP_PWM_PIN, value);
		}
		else {
			analogWrite(BEEP_PWM_PIN, 0);
		}
		int sleepms = ary.get<int>(i);
		if (sleepms < 0) {
			sleepms = 0;
		}
		if (sleepms > 10000) {
			sleepms = 10000;
		}
		delay(sleepms);
	}
	analogWrite(BEEP_PWM_PIN, 0);
	pinMode(BEEP_PWM_PIN, INPUT);
#endif

}