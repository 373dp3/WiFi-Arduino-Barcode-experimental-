// TxManager.h

#ifndef _TXMANAGER_h
#define _TXMANAGER_h

#if defined(ARDUINO) && ARDUINO >= 100
	#include "Arduino.h"
#else
	#include "WProgram.h"
#endif

#include <cdcftdi.h>

//typedef void(*FUNC_PTR)(void);

/*
MAC、SEQ、各種情報を保持して受信確認に責任を持つ
*/
class TxManager {
public:
	char mac[9];
	uint8_t seq = 255;
	void init(FTDI* ft);
	bool put(const char* msg, uint8_t len, uint32_t timeout=250, uint8_t retry = 3);

private:
	FTDI* ftdi;
	void checkMac();
	bool grepMac(char* msg, char* mac_buf);
	bool checkAck(char* msg);
};

#endif

