// 
// 
// 

#include "LcdManager.h"
LcdManager::Status  LcdManager::status = LcdManager::BOOT;
ScreenParam LcdManager::param;

LcdManager::LcdManager(Max_LCD * max_lcd, uint8_t beepPinCode)
{
	lcd = max_lcd;

	memset(&param, 0, sizeof(param));

	beepPin = beepPinCode;
}

void LcdManager::update(Status stat, uint32_t timeout_ms, Status nextStatus)
{
	return;//[TODO] [DEBUG]WiFi化テストの為に一時的に無効化

	if (timeout_ms != 0) {
		if (nextStatus == LcdManager::NOOP) {
			_pre_status = LcdManager::status;
		}
		else {
			_pre_status = nextStatus;
		}
		limit_ms = timeout_ms + millis();
	}else{
		limit_ms = 0;
	}
	LcdManager::status = stat;

	switch (LcdManager::status)
	{
	case LcdManager::BOOT:
		//ﾁｭｳｵｳｺｳｷ
		lcd->setCursor(0, 0);
		lcd->print("\xc1\xad\xb3\xb5\xb3\xba\xb3\xb7 ");
		lcd->setCursor(0, 1);
		//ｶﾝﾊﾞﾝ
		lcd->print("IoT\xb6\xdd\xca\xde\xdd v0.0.1");

		break;
	case LcdManager::REQ_STUFF_CODE:
		lcd->clear();
		//ｽﾀｯﾌｺｰﾄﾞ
		lcd->setCursor(0, 0);
		lcd->print("\xbd\xc0\xaf\xcc\xba\xb0\xc4\xde?");

		break;
	case LcdManager::REQ_LOCATION_CODE:
		lcd->clear();
		//ﾊﾞｼｮｺｰﾄﾞ
		lcd->setCursor(0, 0);
		lcd->print("S) ");
		lcd->print(param.stuff_code);
		lcd->setCursor(0, 1);
		lcd->print("\xca\xde\xbc\xae\xba\xb0\xc4\xde?");

		break;
	case LcdManager::REQ_OPERATION_CODE:
		lcd->clear();
		//
		lcd->setCursor(0, 0);
		lcd->print("P) ");
		lcd->print(param.place_code);
		lcd->setCursor(0, 1);
		lcd->print("Operation code?");
		break;
	case LcdManager::WAIT_BARCODE:
		lcd->clear();
		//
		lcd->setCursor(0, 0);
		lcd->print("Scan barcode");
		break;
	case LcdManager::WAIT_QUANTITY_INPUT:
		lcd->clear();
		//
		lcd->setCursor(0, 0);
		lcd->print(param.barcode);
		lcd->setCursor(0, 1);
		lcd->print("Quantity?");
		lcd->setCursor(0, 10);
		break;
	case LcdManager::SENDING:
		lcd->clear();
		//
		lcd->setCursor(0, 0);
		lcd->print("Sending ...");
		break;
	case LcdManager::SENDOK:
		lcd->clear();
		//
		lcd->setCursor(0, 0);
		lcd->print("Send ok.");
		break;
	case LcdManager::ERR_TX_FAIL:
		lcd->clear();
		//ﾊﾞｼｮｺｰﾄﾞ
		lcd->setCursor(0, 0);
		lcd->print("!! SEND ERR !!");
		BEEP_LONG;
		break;
	case LcdManager::ERR_NO_MEMORY:
		lcd->clear();
		//ﾊﾞｼｮｺｰﾄﾞ
		lcd->setCursor(0, 0);
		lcd->print("!! Memory ERR !!");
		lcd->setCursor(0, 1);
		lcd->print("PUSH RESET BUTTON");
		BEEP_LONG;
		break;
	case LcdManager::ERR_OTHER:
		lcd->clear();
		//ﾊﾞｼｮｺｰﾄﾞ
		lcd->setCursor(0, 0);
		lcd->print("!! INPUT ERR !!");
		BEEP_LONG;
		break;
	case LcdManager::NOOP:
		break;
	default:
		break;
	}
}

void LcdManager::Task()
{
	if (limit_ms == 0) { return; }
	if (millis() < limit_ms) { return; }
	//以前の画面に復帰
	update(_pre_status);
}

void LcdManager::valueFixed(char * value)
{
	switch (LcdManager::status)
	{
	case LcdManager::REQ_STUFF_CODE:
		strcpy(param.stuff_code, value);
		update(REQ_LOCATION_CODE);
		break;
	case LcdManager::REQ_LOCATION_CODE:
		strcpy(param.place_code, value);
		update(REQ_OPERATION_CODE);
		break;
	case LcdManager::REQ_OPERATION_CODE:
		//if ((value[0] != '+') && (value[0] != '-') && (value[0] != '0')) {
		//	update(ERR_OTHER, 2000);
		//	return;
		//}
		strcpy(param.op_code, value);
		update(WAIT_BARCODE);
		break;
	case LcdManager::WAIT_BARCODE:
		strcpy(param.barcode, value);
		param.millis = millis();
		update(WAIT_QUANTITY_INPUT);
		break;
	case LcdManager::WAIT_QUANTITY_INPUT:
		strcpy(param.qty, value);
		update(SENDING);
		break;
	default:
		break;
	}
}

void LcdManager::onChangeTenkey(char* tenkey)
{
	lcd->setCursor(16 - strlen(tenkey) - 1, 1);
	lcd->print(" ");
	lcd->print(tenkey);
}
