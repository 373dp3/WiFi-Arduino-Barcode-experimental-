// 
// 
// 

#include "HIDSelector.h"

char HIDSelector::BARCODE[BARCODE_BUFFER_SIZE];
char HIDSelector::TENKEY[TENKEY_BUFFER_SIZE];
uint8_t HIDSelector::KEYSTROKE[KEY_STROKE_BUFFER_SIZE];
CommonController* HIDSelector::pCommCtrler = NULL;

uint8_t HIDSelector::OemToAscii(uint8_t mod, uint8_t key) {
	uint8_t shift = (mod & 0x22);

	// [a-z]
	if (VALUE_WITHIN(key, 0x04, 0x1d)) {
		isTenKey = false;
		return (key - 4 + 'A');
	}// Numbers
	else if (VALUE_WITHIN(key, 0x1e, 0x27)) {
		isTenKey = false;
		if (shift)
			return ((uint8_t)pgm_read_byte(&numKeys[key - 0x1e]));
		else
			return ((key == UHS_HID_BOOT_KEY_ZERO) ? '0' : key - 0x1e + '1');
	}// Keypad Numbers
	else if (VALUE_WITHIN(key, 0x59, 0x61)) {
		//if(kbdLockingKeys.kbdLeds.bmNumLock == 1)
		isTenKey = true;
		return (key - 0x59 + '1');
	}
	else if (VALUE_WITHIN(key, 0x2d, 0x38))
		return ((shift) ? (uint8_t)pgm_read_byte(&symKeysUp[key - 0x2d]) : (uint8_t)pgm_read_byte(&symKeysLo[key - 0x2d]));
	//                  return (uint8_t)pgm_read_byte(&symKeysLo[key - 0x2d]);
	else if (VALUE_WITHIN(key, 0x54, 0x58)) {
		isTenKey = true;
		return (uint8_t)pgm_read_byte(&padKeys[key - 0x54]);
	}
	else {
		switch (key) {
		case UHS_HID_BOOT_KEY_SPACE: return (0x20);
		case UHS_HID_BOOT_KEY_ENTER: return (0x13);
		case UHS_HID_BOOT_KEY_ZERO2: 
			isTenKey = true;
			return '0';
		case UHS_HID_BOOT_KEY_PERIOD: 
			isTenKey = true;
			return '.';
		}
	}
	return (0);
}


// Override HIDComposite to be able to select which interface we want to hook into

// Return true for the interface we want to hook into
bool HIDSelector::SelectInterface(uint8_t iface, uint8_t proto)
{
	if (proto != 0)
		return true;

	return false;
}

void HIDSelector::init()
{
	memset(TENKEY, 0, sizeof(TENKEY));
	memset(BARCODE, 0, sizeof(BARCODE));
	memset(KEYSTROKE, 0, sizeof(KEYSTROKE));
}

void HIDSelector::setCommonController(CommonController * pCC)
{
	pCommCtrler = pCC;
}


// Will be called for all HID data received from the USB interface
void HIDSelector::ParseHIDData(USBHID *hid, uint8_t ep, bool is_rpt_id, uint8_t len, uint8_t *buf) {
#if 0
	if (len && buf) {
		Notify(PSTR("\r\n"), 0x80);
		for (uint8_t i = 0; i < len; i++) {
			D_PrintHex<uint8_t >(buf[i], 0x80);
			Notify(PSTR(" "), 0x80);
		}
	}
#else
	if (len && buf) {
	}
	else {
		return;
	}
	//キーが何も押されていない場合は処理せず終了
	if (buf[2] == 0x00) {
		memset(KEYSTROKE, 0, sizeof(KEYSTROKE));
		return;
	}

	//バッファに無い新しいOEMコードを調査
	uint8_t tmpbuf[KEY_STROKE_BUFFER_SIZE];
	memset(tmpbuf, 0, KEY_STROKE_BUFFER_SIZE);
	for (int i = 2; i < len; i++) {
		if (buf[i] == 0) { break; }
		tmpbuf[i] = buf[i];//仮置き
		for (int j = 0; j < KEY_STROKE_BUFFER_SIZE; j++) {
			if (buf[i] == KEYSTROKE[j]) {
				//存在したら取り消してbreak;
				tmpbuf[i] = 0;
				break;
			}
		}
	}
	memcpy(KEYSTROKE, buf, KEY_STROKE_BUFFER_SIZE);

	//処理対象ループ
	for (int i = 0; i < KEY_STROKE_BUFFER_SIZE; i++)
	{
		if (tmpbuf[i] == 0x00) { continue; }

		uint8_t c = OemToAscii(buf[0], tmpbuf[i]);
		switch (c) {
		case 0x00:
			break;
		case 0x13:
			if (!isTenKey) {
				Serial.println();
				if (pCommCtrler!=NULL) {
					pCommCtrler->onBarcodeEnter();
					memset(BARCODE, 0, sizeof(BARCODE));
				}
			}
			break;
		default:
			if (isTenKey) {
				updateTenkeyBuffer(c);
				pCommCtrler->onChange();
			}
			else {
				//Serial.print((char)c);
				updateBarcodeBuffer((char)c);
			}
			break;
		}

		//OEMコードに基づく処理変更
		switch (buf[2])
		{
		case UHS_HID_BOOT_KEY_ENTER:
			break;
		case 0x2A:
			if (TENKEY[0] != 0x00) {
				TENKEY[strlen(TENKEY) - 1] = 0x00;
				pCommCtrler->onChange();
			}
			break;
		case 0x58:/*tenkey enter*/
			if (pCommCtrler != NULL) {
				pCommCtrler->onTenkeyEnter();
			}
			memset(TENKEY, 0, sizeof(TENKEY));
		default:
			break;
		}
	}
#endif

}

void HIDSelector::updateTenkeyBuffer(char c)
{
	uint16_t len = strlen(TENKEY);
	TENKEY[len] = c;
	TENKEY[TENKEY_BUFFER_SIZE - 1] = 0x00;//終端の保証
}

void HIDSelector::updateBarcodeBuffer(char c)
{
	uint16_t len = strlen(BARCODE);
	BARCODE[len] = c;
	BARCODE[BARCODE_BUFFER_SIZE - 1] = 0x00;//終端の保証

}
