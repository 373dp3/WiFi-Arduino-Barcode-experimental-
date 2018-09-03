// NormalOperationManager.h

#ifndef _NORMALOPERATIONMANAGER_h
#define _NORMALOPERATIONMANAGER_h

#if defined(ARDUINO) && ARDUINO >= 100
	#include "arduino.h"
#else
	#include "WProgram.h"
#endif
#include "Config.h"
#include "CommonController.h"
#include "LcdManager.h"
#include "HIDSelector.h"
#include "NetworkManager.h"
#include "TableViewManager.h"

extern LcdManager scr;
extern NetworkManager net;
extern Max_LCD lcd;

class NormalOperationManager : public CommonController {
public:
	NormalOperationManager() {
		memset(net.session_key, 0, sizeof(net.session_key));
		memset(net.session_name, 0, sizeof(net.session_name));
		memset(net.session_uri, 0, sizeof(net.session_uri));
	};
	void onChange() { this->valueChange(); };
	void onBarcodeEnter() { this->post(); };
	void onTenkeyEnter() { this->post(); };
	void resume();
	void post();
	const char* getSessionKey() { return net.session_key; }

private:
	void valueChange();
	bool checkBarcodeCommand();


	CommonController * pSubController = NULL;

};

#endif

