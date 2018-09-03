// NetworkManager.h

#ifndef _NETWORKMANAGER_h
#define _NETWORKMANAGER_h

#if defined(ARDUINO) && ARDUINO >= 100
	#include "arduino.h"
#else
	#include "WProgram.h"
#endif

#include <FS.h>
#include "Config.h"

#define CONFIG_FILE	"/config.jsn"
#define CONFIG_ERASE_NUMPAD_CMD "/--0"


class NetworkManager {
public:
	NetworkManager();
	const char* getBaseURL();
	const char* getStartUri();
	void clearStartUri();
	void setupWiFi(Max_LCD* pLcd);

	char session_key[12];
	char session_name[32];
	char session_uri[32];

private:
	void parseIpAdr(const char* ipChar, uint* ipAry);
	char *pSSID;
	char *pPASSWORD;
	int IS_DHCP = 1;
	char BASE_URL[128];
	char START_URI[32];
	uint IP_ADR[4];
	uint SUBNET[4];
	uint GATEWAY[4];
	uint DNSIP[4];
};

#endif

