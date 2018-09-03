// 
// 
// 

#include "NormalOperationManager.h"

void NormalOperationManager::valueChange() {
	scr.onChangeTenkey(HIDSelector::TENKEY);
}

bool NormalOperationManager::checkBarcodeCommand()
{
	if (strcmp(HIDSelector::BARCODE, "RB$%") == 0) {
#ifdef DP3_ESP32
#else
		ESP.reset();
#endif
		return true;
	}

	if (strcmp(HIDSelector::BARCODE, "HO$%") == 0) {
		memset(net.session_key, 0, sizeof(net.session_key));
		memset(net.session_name, 0, sizeof(net.session_name));
		memset(net.session_uri, 0, sizeof(net.session_uri));
	}

	//ES$% (Edit Start)
	if (strcmp(HIDSelector::BARCODE, "ES$%") == 0) {
		Serial.println("INTO Edit Start");
		/*/
		char order[] = "{\"d\":[ {\"id\": 0, \"hhmm\": \"00:00\", \"cols\":[ \"AAAA\",\"10\",\"0\" ] } , {\"id\": 1, \"hhmm\": \"00:11\", \"cols\":[ \"BBBB-BB\",\"30\",\"2\" ] }, {\"id\": 1, \"hhmm\": \"00:22\", \"cols\":[ \"CCCC-CC\",\"40\",\"3\" ] }, {\"id\": 1, \"hhmm\": \"00:33\", \"cols\":[ \"DDDD\",\"50\",\"40\" ] } ]}";
		pSubController = new TableViewWrapper(order, this);
		/*/
		pSubController = new TableViewWrapper(this);
		//*/
		HIDSelector::setCommonController(pSubController);
		return true;
	}
	return false;
}


void NormalOperationManager::resume()
{
	Serial.print("NormalOperationManager::resume");
	if (pSubController == NULL) {
		Serial.println(F("pSubController is NULL"));
		return;
	}
	delete pSubController;
	pSubController = NULL;

	memset(HIDSelector::BARCODE, 0, sizeof(HIDSelector::BARCODE));
	memset(net.session_name, 0, sizeof(net.session_name));
	//memset(session_uri, 0, sizeof(session_uri));
	this->post();
}

void NormalOperationManager::post() {
	Serial.println(F("into post"));

	//�Z�b�V���������͎��̓��͂̓��[�h�ݒ�Ɣ��f����B
	if (net.session_uri[0] == 0) {
		Serial.println(F("[0] == 0"));
		if (HIDSelector::BARCODE[0] != 0) {

			Serial.println(F("ref from barcode"));
			strcpy(net.session_uri, HIDSelector::BARCODE);

		}
		else if (HIDSelector::TENKEY[0] != 0) {
			Serial.println(F("ref from numpad"));
			String tenkey = "'";
			tenkey += HIDSelector::TENKEY;
			tenkey += "'";
			strcpy(net.session_uri, tenkey.c_str());
		}
	}

	//�o�[�R�[�h����̃��[�h�ύX���͔���
	if (checkBarcodeCommand()) { return; }

	//���M����
	{
		Serial.println(F("into http"));
		HTTPClient http;
		String url = net.getBaseURL();
		url += net.session_uri;
		Serial.println(url.c_str());
		http.begin(url.c_str());
		//http.begin("proxy server ip", 8080, url.c_str());
		JSONBuffer.clear();

		Serial.println(F("json encoder"));
		JsonObject& JSONencoder = JSONBuffer.createObject();
		JSONencoder["bc"] = HIDSelector::BARCODE;
		JSONencoder["np"] = HIDSelector::TENKEY;
		JSONencoder["bt"] = net.session_key;
		JSONencoder["name"] = net.session_name;

		uint8_t mac0[6];

#ifdef defined(ESP32)
		esp_efuse_mac_get_default(mac0);
#else 
		WiFi.macAddress(mac0);
#endif

		char macstr[16];
		memset(macstr, 0, sizeof(macstr));
		sprintf(macstr, "%02X%02X%02X%02X%02X%02X", mac0[0], mac0[1], mac0[2], mac0[3], mac0[4], mac0[5]);
		JSONencoder["mac"] = macstr;


		char* JsonCharBuffer = (char*)malloc(JSON_POST_BUFFER_SIZE);
		if (JsonCharBuffer == NULL) {
			Serial.println(F("[ERROR] malloc failed"));
		}
		JSONencoder.printTo(JsonCharBuffer, JSON_POST_BUFFER_SIZE);
		JSONencoder.end();

		Serial.print(F("JSON POST:"));
		Serial.println(JsonCharBuffer);

		bool isOk = false;
		bool isAutoFetch = false;

		while (true) {
			int httpCode = http.POST(JsonCharBuffer);
			if (httpCode > 0) { //Check for the returning code
				String payload = http.getString();
				Serial.print(F("httpCode: "));
				Serial.println(httpCode);
				Serial.print(F("payload: "));
				Serial.println(payload);
				JSONBuffer.clear();
				JsonObject& parsed = JSONBuffer.parseObject(payload);
				lcd.clear();
				if (parsed.success()
					&& parsed.containsKey("L1")
					&& parsed.containsKey("L2")
					) {

					lcd.setCursor(0, 0);
					lcd.print(parsed[F("L1")].asString());
					lcd.setCursor(0, 1);
					lcd.print(parsed[F("L2")].asString());

					if (parsed.containsKey("bt")) {
						strcpy(net.session_key, parsed["bt"].asString());
						Serial.print(F("update sessionkey bt:"));
						Serial.println(parsed["bt"].asString());
						if (net.getStartUri()[0] != 0) {
							Serial.print(F("Set default sheet name: "));
							Serial.println(net.getStartUri());
							strcpy(net.session_uri, net.getStartUri());
							Serial.println(net.session_uri);
							//memset(START_URI, 0, sizeof(START_URI));//�P�x����
							net.clearStartUri();
							isAutoFetch = true;
						}
					}

					memset(net.session_name, 0, sizeof(net.session_name));
					if (parsed.containsKey("name")) {
						strcpy(net.session_name, parsed[F("name")].asString());
					}
					else {
						//�V�[�g�I���~�X�ׁ̈AURL���N���A
						if (isAutoFetch == false) {
							memset(net.session_uri, 0, sizeof(net.session_uri));
						}
					}
					beepCheck(&parsed);
				}
				else {
					lcd.setCursor(0, 0);
					lcd.print(payload);
				}
				parsed.end();
				http.end();
				break;//while���[�v�𔲂���
			}
			else {
				if (Serial.available() > 10) { return; }//�ݒ菑�����ݎw��
				Serial.println(F("Error on HTTP request"));
				lcd.clear();
				lcd.setCursor(0, 0);
				lcd.print(F("ERROR 003"));
				lcd.setCursor(0, 1);
				lcd.print(F("CONN failure"));
				delay(3000);
				http.end();
			}

		}
		free(JsonCharBuffer);

		//�����I���V�[�g���w�肳�ꂽ�ꍇ�̏���
		if (isAutoFetch) {
			post();
		}
	}
}