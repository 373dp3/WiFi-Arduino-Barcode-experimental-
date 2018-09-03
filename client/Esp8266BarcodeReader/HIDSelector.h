// HIDSelector.h

#ifndef _HIDSELECTOR_h
#define _HIDSELECTOR_h

#if defined(ARDUINO) && ARDUINO >= 100
	#include "Arduino.h"
#else
	#include "WProgram.h"
#endif
#include <hidcomposite.h>
#include <hidboot.h>
#include "CommonController.h"

const uint8_t numKeys[10] PROGMEM = { '!', '@', '#', '$', '%', '^', '&', '*', '(', ')' };
const uint8_t symKeysUp[12] PROGMEM = { '_', '+', '{', '}', '|', '~', ':', '"', '~', '<', '>', '?' };
const uint8_t symKeysLo[12] PROGMEM = { '-', '=', '[', ']', '\\', ' ', ';', '\'', '`', ',', '.', '/' };
const uint8_t padKeys[5] PROGMEM = { '/', '*', '-', '+', 0x13 };

#define TENKEY_BUFFER_SIZE	(6)
#define BARCODE_BUFFER_SIZE	(24)
#define KEY_STROKE_BUFFER_SIZE (5)

typedef void(*FUNC_PTR)(void);

/* テンキーのOEMコード
	bs	2A
	ent	58
	tab	2B
	/	54
	*	55
	-	56
	+	57
	left	50
	right	4F
	down	51
	up	52

	1	59
	2	5A
	...
	9	61
	0	62
*/

class HIDSelector : public HIDComposite
{
public:
	static char TENKEY[TENKEY_BUFFER_SIZE];
	static char BARCODE[BARCODE_BUFFER_SIZE];
	static void clearBuffer() {
		memset(HIDSelector::TENKEY, 0, sizeof(HIDSelector::TENKEY));
		memset(HIDSelector::BARCODE, 0, sizeof(HIDSelector::BARCODE));
	};
	HIDSelector(USB *p) : HIDComposite(p) {};
	void init();
	bool isTenKey = false;

	static void setCommonController(CommonController* pCC);

protected:
	static uint8_t KEYSTROKE[KEY_STROKE_BUFFER_SIZE];

	void ParseHIDData(USBHID *hid, uint8_t ep, bool is_rpt_id, uint8_t len, uint8_t *buf); // Called by the HIDComposite library
	bool SelectInterface(uint8_t iface, uint8_t proto);
	uint8_t OemToAscii(uint8_t mod, uint8_t key);
	static CommonController* pCommCtrler;
	void updateTenkeyBuffer(char c);
	void updateBarcodeBuffer(char c);
};

#endif

