// 
// 
// 

#include "NetworkManager.h"

NetworkManager::NetworkManager()
{
	pSSID = NULL;
	pPASSWORD = NULL;
	memset(START_URI, 0, sizeof(START_URI));
}

const char * NetworkManager::getBaseURL()
{
	return BASE_URL;
}

const char * NetworkManager::getStartUri()
{
	return START_URI;
}

void NetworkManager::clearStartUri()
{
	memset(START_URI, 0, sizeof(START_URI));
}

void NetworkManager::setupWiFi(Max_LCD* pLcd)
{
	int isFileMatch = 0;
	SPIFFS.begin();
	Dir dir = SPIFFS.openDir("/");
	while (dir.next()) {
		Serial.print(dir.fileName());
		if (strcmp(dir.fileName().c_str(), CONFIG_FILE) == 0) {
			isFileMatch = 1;
		}
		File f = dir.openFile("r");
		Serial.println(String(" ") + f.size());
	}
	if (isFileMatch == 0) {
		Serial.print(F("SPIFFS format start"));
		SPIFFS.format();
		Serial.println(F(" done."));
		/*
		Serial.println(F("config file not found."));
		File file = SPIFFS.open(CONFIG_FILE, "w");
		if (file == NULL) {
		Serial.println(F("Cannot create file"));
		}
		else {
		JSONBuffer.clear();

		char* JsonCharBuffer = (char*)malloc(JSON_POST_BUFFER_SIZE);
		memset(JsonCharBuffer, 0, JSON_POST_BUFFER_SIZE);

		Serial.println(F("json encoder"));
		JsonObject& JSONencoder = JSONBuffer.createObject();
		JSONencoder["dhcp"] = 1;
		JSONencoder["ip"] = "192.168.100.101";
		JSONencoder["mask"] = "255.255.255.0";
		JSONencoder["gw"] = "192.167.100.1";
		JSONencoder.printTo(JsonCharBuffer, JSON_POST_BUFFER_SIZE);
		JSONencoder.end();
		file.print(JsonCharBuffer);
		file.close();
		free(JsonCharBuffer);

		}
		//*/
	}
	else {
		File file = SPIFFS.open(CONFIG_FILE, "r");
		if (file.size() < JSON_POST_BUFFER_SIZE) {
			char* JsonCharBuffer = (char*)malloc(JSON_POST_BUFFER_SIZE);
			memset(JsonCharBuffer, 0, JSON_POST_BUFFER_SIZE);
			file.readBytes(JsonCharBuffer, file.size());
			Serial.println(JsonCharBuffer);
			JSONBuffer.clear();
			JsonObject& parsed = JSONBuffer.parseObject(JsonCharBuffer);
			if (parsed.success()) {
				if (parsed.containsKey(F("ssid"))) {
					String ssid = parsed[F("ssid")].asString();
					pSSID = (char *)malloc(ssid.length() + 1);//開放はH/W RESETのみ
					strcpy(pSSID, ssid.c_str());
					Serial.print(F("pSSID: ")); Serial.println(pSSID);
				}
				if (parsed.containsKey(F("pwd"))) {
					String tmp = parsed[F("pwd")].asString();
					pPASSWORD = (char *)malloc(tmp.length() + 1);//開放はH/W RESETのみ
					strcpy(pPASSWORD, tmp.c_str());
					Serial.print(F("pPASSWORD: ")); Serial.println(pPASSWORD);
				}
				if (parsed.containsKey(F("server"))) {
					String tmp = parsed[F("server")].asString();
					strcpy(BASE_URL, tmp.c_str());
					Serial.print(F("BASE_URL: ")); Serial.println(BASE_URL);
				}
				if (parsed.containsKey(F("start"))) {
					String tmp = parsed[F("start")].asString();
					strcpy(START_URI, tmp.c_str());
					Serial.print(F("START_URI: ")); Serial.println(START_URI);
				}
				if (parsed.containsKey(F("dhcp"))) {
					String tmp = parsed[F("dhcp")].asString();
					if (tmp.charAt(0) == '1') {
						IS_DHCP = 1;
					}
					else {
						IS_DHCP = 0;
					}
					Serial.print(F("DHCP: ")); Serial.println(IS_DHCP);
				}
				if (parsed.containsKey(F("ip"))) {
					String tmp = parsed[F("ip")].asString();
					parseIpAdr(tmp.c_str(), IP_ADR);
				}
				if (parsed.containsKey(F("gw"))) {
					String tmp = parsed[F("gw")].asString();
					parseIpAdr(tmp.c_str(), GATEWAY);
				}
				if (parsed.containsKey(F("mast"))) {
					String tmp = parsed[F("mast")].asString();
					parseIpAdr(tmp.c_str(), SUBNET);
				}
				if (parsed.containsKey(F("dns"))) {
					String tmp = parsed[F("dns")].asString();
					parseIpAdr(tmp.c_str(), DNSIP);
				}
				//parseIpAdr
			}
			free(JsonCharBuffer);
		}
		else {
			Serial.println(F("Config json file size over."));
		}
		file.close();

	}

#ifdef DP3_ESP32
	//ledcSetup(0/* LED ctrler ch */, 20000/*freq*/, 8 /*8bit精度*/);
	//ledcAttachPin(CONTLAST_ANALOG_WRITE_PIN, 0/*led ch*/);
	//ledcWrite(0, 80);//出力を80/256に設定
	//pinMode(CONTLAST_ANALOG_WRITE_PIN, OUTPUT);

	//LCD LEDのカソード制御用NchMOS GATE
	pinMode(LCD_LED_K_PIN, OUTPUT);
	digitalWrite(LCD_LED_K_PIN, HIGH);
#else
	//LCDのLEDバックライト制御
	pinMode(LCD_LED_PIN, OUTPUT);
	digitalWrite(LCD_LED_PIN, HIGH);
#endif


	// LCDのサイズを指定
	pLcd->begin(16, 2);

	if ((pSSID == NULL) || (pPASSWORD == NULL)) {
		pLcd->setCursor(0, 0);
		pLcd->print("No config found.");
		pLcd->setCursor(0, 1);
		pLcd->print("Please setup.");
		return;
	}

	WiFi.mode(WIFI_STA);
	//固定IP時
	WiFi.begin(pSSID, pPASSWORD);
	if (IS_DHCP == 0) {
		//IPアドレスの設定
		WiFi.config(
			IPAddress(IP_ADR[0], IP_ADR[1], IP_ADR[2], IP_ADR[3]), //local
			IPAddress(GATEWAY[0], GATEWAY[1], GATEWAY[2], GATEWAY[3]), //gateway
			IPAddress(SUBNET[0], SUBNET[1], SUBNET[2], SUBNET[3]),//subnet
			IPAddress(DNSIP[0], DNSIP[1], DNSIP[2], DNSIP[3]));//dns
	}
	Serial.print(F("WiFi connecting"));
	pLcd->clear();
	pLcd->setCursor(0, 0);
	pLcd->print(F("WiFi connecting "));
	int isOn = HIGH;

	char chWait[] = { '-','\\', '|', '/' ,'-','\\','|','/' };
	int cnt = 0;

	long timeoutms = millis() + 15000L;
	for (int i = 1; i < 20; i++) {
		if (WiFi.status() == WL_CONNECTED) { break; }
		for (int j = 0; j < 200; j++) {
			if (WiFi.status() == WL_CONNECTED) { break; }
			if (Serial.available() > 10) { return; }//設定変更指示の為、中断
			Serial.print(".");
			delay(100);
			pLcd->setCursor(0, 1);
			pLcd->print("Try ");
			pLcd->print(i);
			pLcd->setCursor(10, 1);
			pLcd->print(chWait[cnt % sizeof(chWait)]);
			cnt++;
		}
		//タイムアウトの場合は、リトライ
		if (WiFi.status() != WL_CONNECTED) {
			Serial.print("Retry ");
			WiFi.disconnect();
			delay(500);
			WiFi.begin(pSSID, pPASSWORD);
			//IPアドレスの設定
			WiFi.config(
				IPAddress(IP_ADR[0], IP_ADR[1], IP_ADR[2], IP_ADR[3]), //local
				IPAddress(GATEWAY[0], GATEWAY[1], GATEWAY[2], GATEWAY[3]), //gateway
				IPAddress(SUBNET[0], SUBNET[1], SUBNET[2], SUBNET[3]),//subnet
				IPAddress(DNSIP[0], DNSIP[1], DNSIP[2], DNSIP[3]));//dns
			Serial.println(i);
		}
	}

	Serial.println(F(" connected"));
	Serial.println(WiFi.localIP());
	Serial.println(WiFi.subnetMask());
	Serial.println(WiFi.gatewayIP());
}

void NetworkManager::parseIpAdr(const char * ipChar, uint * ipAry)
{
	if (ipChar[0] == 0x00) { return; }
	char *tmp = (char*)malloc(strlen(ipChar) + 1);
	strcpy(tmp, ipChar);
	char *tp;
	int pos = 0;

	/* スペース.を区切りに文字列を抽出 */
	tp = strtok(tmp, ".");
	ipAry[pos] = strtol(tp, 0, 10);
	pos++;
	while (tp != NULL) {
		tp = strtok(NULL, ".");
		if (tp == NULL) {
			break;
		}
		ipAry[pos] = strtol(tp, 0, 10);
		pos++;
		if (pos >= 4) { break; }
	}
	free(tmp);
	Serial.print(F("ip parse: "));
	Serial.print(ipChar);
	Serial.print(" ");
	for (int i = 0; i < 4; i++) {
		Serial.print(ipAry[i]);
		Serial.print(" ");
	}
	Serial.println("");

}
