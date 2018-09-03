// 
// 
// 

#include "TxManager.h"

void TxManager::init(FTDI * ft)
{
	ftdi = ft;

	memset(mac, 0, sizeof(mac));
}

bool TxManager::put(const char * msg, uint8_t len, uint32_t timeout, uint8_t retry)
{
	checkMac();

	uint8_t  buf[64];

	//送信前にFTDIのバッファをクリア
	uint16_t rcvd = 64;
	while (rcvd > 2) {
		rcvd = 64;
		ftdi->RcvData(&rcvd, buf);
	}

	//送信
	seq++;

	for (uint8_t t = 0; t < retry; t++) {
		String str = String(seq);
		str = String(str + ",");
		str = String(str + (const char*)msg);
		Serial.print(str);
		ftdi->SndData(str.length(), (uint8_t*)str.c_str());


		//受信処理
		uint32_t limit_ms = millis() + timeout;
		while (millis() < limit_ms) {
			memset(buf, 0, sizeof(buf));

			rcvd = 64;
			ftdi->RcvData(&rcvd, buf);

			// The device reserves the first two bytes of data
			//   to contain the current values of the modem and line status registers.
			if ((rcvd > 2) && (checkAck((char*)(buf + 2)))) {
				//Serial.print((char*)(buf + 2));
				return true;
			}
		}
	}

	return false;
}

void TxManager::checkMac()
{
	if (mac[0] != 0x00) { return; }

	//自分のMACを取得
	uint8_t buf[64];
	memset(buf, 0, sizeof(buf));

	//送信前にバッファクリア
	uint16_t rcvd = 64;
	while (rcvd > 2) {
		rcvd = 64;
		ftdi->RcvData(&rcvd, buf);
	}

	//送信
	char msg[] = "\r\n";
	ftdi->SndData(2, (uint8_t*)msg);

	uint32_t limit_ms = millis() + 1000UL;
	while (millis() < limit_ms) {
		memset(buf, 0, sizeof(buf));

		rcvd = 64;
		ftdi->RcvData(&rcvd, buf);

		// The device reserves the first two bytes of data
		//   to contain the current values of the modem and line status registers.
		if (rcvd > 2) {
			//Serial.print("mac: ");
			//Serial.print((char*)(buf + 2));
			if (grepMac((char*)(buf + 2), mac)) {
				Serial.print("mac: ");
				Serial.println(mac);
				
				return;
			}
		}
		delay(80);
	}

}



bool TxManager::grepMac(char * msg, char* mac_buf)
{
	char *p;
	p = strtok(msg, "\r\n \:");

	while (p)
	{
		uint8_t len = strlen(p);
		if (len == 8) {
			strcpy(mac_buf, p);
			return true;
		}
		p = strtok(NULL, "\r\n \:");
	}

	return false;
}

bool TxManager::checkAck(char * msg)
{
	char *p;
	p = strtok(msg, "\r\n ,");
	bool isMacOk = false;

	while (p)
	{
		uint8_t len = strlen(p);
		if (len == 8) {
			//Serial.print("Mac:");
			//Serial.println(p);
			if (strcmp(p, mac) == 0) {
				//Serial.println("Mac is match");
				isMacOk = true;
			}
		}
		else if(len <= 3) {
			//Serial.print("Seq:");
			//Serial.println(p);
			uint16_t retSeq = strtol(p, 0, 10);
			if (isMacOk && (retSeq == seq)) {
				Serial.println("Ack OK");
				return true;
			}
		}
		p = strtok(NULL, "\r\n ,");
	}

	return false;
}

