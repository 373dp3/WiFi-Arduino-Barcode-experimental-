// FTDIAsync.h

#ifndef _FTDIASYNC_h
#define _FTDIASYNC_h

#if defined(ARDUINO) && ARDUINO >= 100
	#include "Arduino.h"
#else
	#include "WProgram.h"
#endif

#include <cdcftdi.h>

class FTDIAsync : public FTDIAsyncOper{
public:
	uint8_t OnInit(FTDI *pftdi);
};


#endif

