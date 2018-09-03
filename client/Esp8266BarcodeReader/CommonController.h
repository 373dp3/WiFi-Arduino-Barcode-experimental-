// CommonController.h

#ifndef _COMMONCONTROLLER_h
#define _COMMONCONTROLLER_h

#if defined(ARDUINO) && ARDUINO >= 100
	#include "arduino.h"
#else
	#include "WProgram.h"
#endif
#include "Config.h"

class CommonController {
public:
	virtual void onChange() = 0;
	virtual void onBarcodeEnter() = 0;
	virtual void onTenkeyEnter() = 0;
	virtual void resume() = 0;

protected:
	void beepCheck(JsonObject* pParsed);
};

#endif

