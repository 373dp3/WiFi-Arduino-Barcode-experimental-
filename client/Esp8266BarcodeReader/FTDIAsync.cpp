// 
// 
// 

#include "FTDIAsync.h"

uint8_t FTDIAsync::OnInit(FTDI * pftdi)
{
	uint8_t rcode = 0;

	rcode = pftdi->SetBaudRate(115200);

	if (rcode)
	{
		ErrorMessage<uint8_t>(PSTR("SetBaudRate"), rcode);
		return rcode;
	}
	rcode = pftdi->SetFlowControl(FTDI_SIO_DISABLE_FLOW_CTRL);

	if (rcode)
		ErrorMessage<uint8_t>(PSTR("SetFlowControl"), rcode);

	return rcode;
}
