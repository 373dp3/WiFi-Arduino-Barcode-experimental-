// 
// 
// 

#include "TableViewManager.h"


TableViewWrapper::TableViewWrapper(CommonController * pBackController)
{
	char * buf = (char*)malloc(JSON_BUFFER_SIZE * sizeof(char*));
	getList(buf, JSON_BUFFER_SIZE);
	this->parseResponseJson(buf);
	free(buf);

	this->state = CELL_SELECT;
	this->pBackController = pBackController;

	this->dumpSerial();
	this->updateLcd();
}

TableViewWrapper::TableViewWrapper(char * listInfoJson, CommonController * pBackController)
{
	this->parseResponseJson(listInfoJson);

	this->state = CELL_SELECT;
	this->pBackController = pBackController;

	this->dumpSerial();
	this->updateLcd();
}

TableViewWrapper::~TableViewWrapper()
{
	Dispose();
}

void TableViewWrapper::refreshModel()
{
	D("refreshModel");
	Dispose();

	char * buf = (char*)malloc(JSON_BUFFER_SIZE * sizeof(char*));
	getList(buf, JSON_BUFFER_SIZE);
	this->parseResponseJson(buf);
	free(buf);

	this->dumpSerial();
	D("refreshModel . done");
}

void TableViewWrapper::parseResponseJson(const char * json)
{
	D("parseResponseJson");
	char* JsonCharBuffer = (char*)malloc(this->JSON_BUF_LENGTH);
	memset(JsonCharBuffer, 0, this->JSON_BUF_LENGTH);
	JSONBuffer.clear();
	strcpy(JsonCharBuffer, json);
	JsonObject& parsed = JSONBuffer.parseObject(JsonCharBuffer);
	if (parsed.success()) {
		this->pModel = new TableViewModel(&parsed);
	}
	else {
		D("[ERROR] parse failer.");
		Serial.println(json);
	}
	free(JsonCharBuffer);
	D("parseResponseJson . done");
}

void TableViewWrapper::getList(char* buf, int bufsize, int direction, const char* strId)
{
	Serial.println(F("getList"));

	JSONBuffer.clear();
	JsonObject& JSONencoder = JSONBuffer.createObject();
	if (strId != NULL) {
		JSONencoder["id"] = strId;
		D("id: ", strId);
	}
	JSONencoder["name"] = "";
	JSONencoder["mode"] = "list";
	JSONencoder["dir"] = String(direction);

	this->_postCommon(JSONencoder, buf, bufsize);
	HIDSelector::clearBuffer();

	Serial.println(F("getList . done"));
}

void TableViewWrapper::getOtherPage(int8_t direction)
{
	D("pageFlag:", String(pModel->pageFlag).c_str());
	D("direction:", String(direction).c_str());
	//�J�ډ\�ȃy�[�W���Ȃ��ꍇ�͏I��
	if (pModel->pageFlag == 0) {
		D("no tr page");
		return;
	}
	//��(�V)��������
	if ((direction > 0) && ((pModel->pageFlag & 0x01) == 0)) {
		D("no up(new) page");
		return;
	}
	//��(��)��������
	if ((direction < 0) && ((pModel->pageFlag & 0x02) == 0)) {
		D("no down(old) page");
		return;
	}
	String strId = String(this->pModel->ppRows[posY]->strId);

	Dispose();

	char * buf = (char*)malloc(JSON_BUFFER_SIZE * sizeof(char*));
	getList(buf, JSON_BUFFER_SIZE, direction, strId.c_str());
	this->parseResponseJson(buf);
	free(buf);
	strId.~String();

	//�t�H�[�J�X����Z���̌���
	if (direction > 0) {
		//��(�V)�����̏ꍇ
		lcdPosY = pModel->rowSize - LCD_SIZE_Y;
		posY = pModel->rowSize - 1;
		if (lcdPosY < 0) { lcdPosY = 0; }
		if (posY < 0) { posY = 0; }
		//D("set lcdPosY:", String(lcdPosY).c_str());
		//D("set posY:", String(posY).c_str());
	}
	if (direction < 0) {
		lcdPosY = 0;
		posY = 0;
	}//��(��)�����̏ꍇ

	D("getOtherPage . done");
}

void TableViewWrapper::postUpdate()
{
	//post���s
	JSONBuffer.clear();
	Serial.println(F("postUpdate"));
	JsonObject& JSONencoder = JSONBuffer.createObject();
	JSONencoder["mode"] = "update";
	JSONencoder["id"] = this->pModel->ppRows[posY]->strId;
	JSONencoder["posX"] = posX;

	this->_postCommon(JSONencoder, NULL, 0);

	HIDSelector::clearBuffer();
	Serial.println(F("postUpdate . done"));
}

void TableViewWrapper::postDelete()
{
	JSONBuffer.clear();
	Serial.println(F("postDelete"));
	JsonObject& JSONencoder = JSONBuffer.createObject();
	JSONencoder["mode"] = "delete";
	JSONencoder["id"] = this->pModel->ppRows[posY]->strId;

	this->_postCommon(JSONencoder, NULL, 0);

	HIDSelector::clearBuffer();
}

void TableViewWrapper::_postCommon(JsonObject & JSONencoder, char * responseBuf, int bufsize)
{
	Serial.println(F("_postCommon"));

	//���M����
	{
		HTTPClient http;
		String url = net.getBaseURL();
		url += net.session_uri;
		Serial.println(url.c_str());
		http.begin(url.c_str());

		JSONencoder["bc"] = HIDSelector::BARCODE;
		JSONencoder["np"] = HIDSelector::TENKEY;
		JSONencoder["bt"] = net.session_key;
		//JSONencoder["name"] = "";

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

				_printPostResponse(payload, responseBuf, bufsize);

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

	}//���M�����I���

	Serial.println(F("_postCommon . done"));

}

void TableViewWrapper::_printPostResponse(String & payload, char * responseBuf, int bufsize)
{
	D("_printPostResponse");
	JsonObject& parsed = JSONBuffer.parseObject(payload);
	lcd.clear();
	switch (state)
	{
	case TableViewWrapper::CELL_SELECT:
		D("_printPostResponse . CELL_SELECT");
		if (parsed.success() && parsed.containsKey("d")) {
			if (payload.length() < bufsize) {
				strcpy(responseBuf, payload.c_str());
			}
			else {
				lcd.print("SIZE OVER");
			}
			beepCheck(&parsed);
		}
		else {
			Serial.println("no contains d");
			lcd.print(payload);
		}
		break;
	case TableViewWrapper::VALUE_EDIT:
		break;
	case TableViewWrapper::ROW_DELETE:
		break;
	default:
		break;
	}
	parsed.end();
	D("_printPostResponse . done");
}


void TableViewWrapper::valueChange()
{
	if (state == CELL_SELECT) {
		switch (HIDSelector::TENKEY[0])
		{
		case '8':// up
			posY--;
			if (posY < 0) {
				//�T�[�o�A�g�̃y�[�W���菈��
				posY = 0;
				getOtherPage(1);//��̂ق������Ԃ��V�����̂ŁA8�̓v���X
			}
			else {
				lcdPosY = posY;//lcd��1�s�ڂɕ\������s�ԍ�
			}
			break;
		case '2':// down
			posY++;
			if (posY > this->pModel->rowSize - 1) { 
				//�T�[�o�A�g�̃y�[�W���菈��
				posY = this->pModel->rowSize - 1;
				getOtherPage(-1);//���̂ق������Ԃ��Â��B2�̓}�C�i�X
			}
			else {
				lcdPosY = posY - 1;//lcd��1�s�ڂɕ\������s�ԍ�
			}
			if (lcdPosY < 0) { lcdPosY = 0; }
			break;
		case '4':// left
			posX--;
			if (posX < 0) { posX = 0; }
			break;
		case '6':// right
			posX++;
			break;
		default:
			break;
		}
		this->updateLcd();


		memset(HIDSelector::TENKEY, 0, sizeof(HIDSelector::TENKEY));
	}

	if (state == VALUE_EDIT) {
		scr.onChangeTenkey(HIDSelector::TENKEY);
	}

	if(state == ROW_DELETE) {
		switch (HIDSelector::TENKEY[0])
		{
		case '1':// ok
			//�T�[�o�ւ̍폜�R�}���h���M
			Serial.print(F("delete ok, posY:"));
			Serial.println(posY);
			D("id:", this->pModel->ppRows[posY]->strId);
			this->postDelete();

			//��ʑJ��
			posX = 0; posY = 0; lcdPosY = 0;
			state = CELL_SELECT;
			this->refreshModel();
			this->_updateLcdCELL_SELECT();

			break;
		case '9':// Cancel
			Serial.println(F("delete cancel"));
			state = CELL_SELECT;
			this->updateLcd();
			break;
		default:
			break;
		}

		memset(HIDSelector::TENKEY, 0, sizeof(HIDSelector::TENKEY));
	}

}


void TableViewWrapper::onBarcodeEnter()
{
	if (strcmp(HIDSelector::BARCODE, "RB$%") == 0) {
#ifdef DP3_ESP32
#else
		ESP.reset();
#endif
	}
	if (strcmp(HIDSelector::BARCODE, "HO$%") == 0) {
	}
	//EE$% (Edit End)
	if (strcmp(HIDSelector::BARCODE, "EE$%") == 0) {
		HIDSelector::setCommonController(this->pBackController);
		this->pBackController->resume();
	}
}

void TableViewWrapper::onTenkeyEnter()
{
	if (state == VALUE_EDIT) {
		//�T�[�o�ւ̕ҏW�R�}���h���M
		Serial.print(F("edit pos:"));
		Serial.print(posX);
		Serial.print(F(", "));
		Serial.print(posY);
		Serial.print(F(" => "));
		Serial.println(HIDSelector::TENKEY);
		D("id:", this->pModel->ppRows[posY]->strId);
		this->postUpdate();
		state = CELL_SELECT;
		this->refreshModel();
		//��ʑJ��
		this->_updateLcdCELL_SELECT();
	}
	else {
		Serial.print("posX:"); Serial.print(posX);
		Serial.print(" colSize:"); Serial.println(pModel->ppRows[0]->colSize);
		//�폜��I�����Ă��邩�H
		if (posX >= pModel->ppRows[0]->colSize) {
			lcd.clear();
			lcd.setCursor(0, 0);
			//���ޮ �Ͻ�?
			lcd.print(F("\xbb\xb8\xbc\xde\xae\x20\xbc\xcf\xbd\xb6\x3f"));
			lcd.setCursor(0, 1);
			lcd.print(F("OK:1 Cancel:9"));
			state = ROW_DELETE;
		}
		else {
			state = VALUE_EDIT;
			this->_updateLcdVALUE_EDIT();
		}
	}
}

void TableViewWrapper::updateLcd()
{
	if (state == CELL_SELECT) {
		this->_updateLcdCELL_SELECT();
	}
	else {
		this->_updateLcdVALUE_EDIT();
	}
}

void TableViewWrapper::_updateLcdCELL_SELECT()
{
	lcd.clear();
	lcd.setCursor(0, 0);
	if (this->pModel == NULL) {
		lcd.print(F("No model"));
		return;
	}
	int rowSize = this->pModel->rowSize;
	if (rowSize == 0) {
		lcd.print(F("Row size: 0"));
		return;
	}
	int colSize = this->pModel->ppRows[0]->colSize;
	if (colSize == 0) {
		lcd.print(F("Col size: 0"));
		return;
	}

	//�z��𒴂��Ȃ����̕ۏ�(�폜�t��)
	if (posX > colSize) { posX = colSize; }

	int printLen = 0;

	Serial.print("use lcdPosY:"); Serial.println(lcdPosY);
	Serial.print("use posY:"); Serial.println(posY);

	//���������̕`��
	if (posX == 0) {
		for (int i = lcdPosY; i < lcdPosY + LCD_SIZE_Y; i++) {
			if (i >= pModel->rowSize) { break; }
			lcd.setCursor(0, i - lcdPosY);
			char * val = pModel->ppRows[i]->strHhMm;
			lcd.print(val);
			int tmpLen = strlen(val);
			if (printLen < tmpLen) { printLen = tmpLen; }
		}
	}
	printLen += 1;

	//X�����\�����_�̒���
	int printLenOrg = printLen;
	int orgX = 0;
	//Serial.println("--");
	//Serial.print("colSize:"); Serial.println(colSize);
	//Serial.print("posX:"); Serial.println(posX);
	for (int i = 0; i < colSize - orgX + 1/* +1�͍폜�� */; i++) {
		//Serial.print("	i:"); Serial.println(i);
		int maxLen = 0;
		if (i == colSize - orgX) {
			//�폜
			maxLen = DELETE_WORD_LENGTH;
			//Serial.println("append delete_word_length into maxLen.");
		}
		else {
			//�ʏ�Z��
			for (int j = 0; j < rowSize; j++) {
				int tmpLen = strlen(pModel->ppRows[j]->ppCols[i + orgX]);
				if (maxLen < tmpLen) { maxLen = tmpLen; }
			}
		}
		printLen += maxLen + 1;
		//���߂����ꍇ�́A���_��1�I�t�Z�b�g���čČv�Z
		//(���񂩂璴�߂���ꍇ�͏��O)
		Serial.print("	printLen:"); Serial.println(printLen);
		if ((printLen >= LCD_SIZE_X) && (i > 0)) {
			i = -1;
			printLen = printLenOrg;
			orgX++;
			//Serial.print("size over. inc offset: "); Serial.println(orgX);
		}
		//���߂����ɂ��ǂ蒅�����ꍇ�͏I��
		if (i + orgX == posX) {
			//Serial.println("Break");
			break;
		}
	}
	//Serial.print("orgX: "); Serial.println(orgX);
	printLen = printLenOrg;


	//�z�񕔕��̕`��
	for (int i = orgX; i < colSize + 1; i++) {
		int maxLen = 0;
		if (i >= colSize) {
			for (int j = 0; j < rowSize; j++) {
				//���ޮ
				int tmpLen = DELETE_WORD_LENGTH;//���p�J�i5����
				if (maxLen < tmpLen) { maxLen = tmpLen; }
				//�\���͈͊O�̃f�[�^��LCD�ɃR�}���h�𑗂�Ȃ�
				if (j < lcdPosY) { continue; }
				if (j >= lcdPosY + LCD_SIZE_Y) { continue; }
				lcd.setCursor(printLen, j - lcdPosY);
				lcd.print(F("\xbb\xb8\xbc\xde\xae"));
			}
		}
		else {
			for (int j = 0; j < rowSize; j++) {
				char* val = pModel->ppRows[j]->ppCols[i];
				int tmpLen = strlen(val);
				if (maxLen < tmpLen) { maxLen = tmpLen; }
				//�\���͈͊O�̃f�[�^��LCD�ɃR�}���h�𑗂�Ȃ�
				if (j < lcdPosY) { continue; }
				if (j >= lcdPosY + LCD_SIZE_Y) { continue; }
				lcd.setCursor(printLen, j - lcdPosY);
				lcd.print(val);
			}
		}

		//�J�[�\���̕`�攻��
		if (i == posX) {
			lcd.setCursor(printLen - 1, posY - lcdPosY);
			lcd.print("[");
			lcd.setCursor(printLen + maxLen, posY - lcdPosY);
			lcd.print("]");
		}
		printLen += maxLen + 1;

		//�\���͈͂𒴂��Ă���ꍇ��'>'�Ŗ��߂�
		if ((printLen >= LCD_SIZE_X) && (i != posX)) {
			char* tmpChar = (char*)malloc(maxLen + 1);
			for (int i = 0; i < maxLen; i++) tmpChar[i] = '>';
			tmpChar[maxLen] = 0x00;
			lcd.setCursor(printLen - maxLen - 1, 0);
			lcd.print(tmpChar);
			if (pModel->rowSize > 1) {
				lcd.setCursor(printLen - maxLen - 1, 1);
			}
			lcd.print(tmpChar);
			free(tmpChar);
			break;
		}
	}
	//*/

}

void TableViewWrapper::_updateLcdVALUE_EDIT()
{
	lcd.clear();
	lcd.setCursor(0, 0);
	lcd.print(F("EDIT: "));
	lcd.print(this->pModel->ppRows[posY]->ppCols[posX]);
}


TableViewWrapper::TableViewModel::TableViewModel(JsonObject * jsonObj)
{
	if (jsonObj == NULL) return;
	JsonObject& json = (*jsonObj);
	if (json.containsKey("d")) {
		JsonArray& ary = json["d"].asArray();
		int size = ary.size();
		this->rowSize = size;
		this->ppRows = (TableViewRowModel**)malloc(size * sizeof(TableViewRowModel**));
		for (int i = 0; i < size; i++) {
			TableViewRowModel *row = new TableViewRowModel(&(ary[i].asObject()));
			this->ppRows[i] = row;
		}
	}
	if (json.containsKey("pflg")) {
		int tmpFlg = atol(json["pflg"].asString());
		if ((tmpFlg >= 0) && (tmpFlg <= 3)) {
			this->pageFlag = tmpFlg;
		}
		else {
			this->pageFlag = 0;
		}
	}
}

void TableViewWrapper::TableViewModel::dumpSerial()
{
	D("rowSize", String(this->rowSize).c_str());
	for (int i = 0; i < rowSize; i++) {
		this->ppRows[i]->dumpSerial();
	}
}

TableViewWrapper::TableViewRowModel::TableViewRowModel(JsonObject * jsonObj)
{
	if (jsonObj == NULL) return;
	JsonObject& json = (*jsonObj);
	if (json.containsKey("id")) {
		copyChar(&this->strId, json["id"].asString());
	}
	if (json.containsKey(F("hhmm"))) {
		copyChar(&this->strHhMm, json["hhmm"].asString());
	}
	if (json.containsKey("cols")) {
		JsonArray& ary = json["cols"].asArray();
		int size = ary.size();
		this->colSize = size;
		this->ppCols = (char**)malloc(size * sizeof(char**));
		for (int i = 0; i < size; i++) {
			copyChar(&(ppCols[i]), ary[i].asString());
		}
	}
}

void TableViewWrapper::TableViewRowModel::dumpSerial()
{
	D("id", this->strId, 1);
	D("hhmm", this->strHhMm, 1);
	D("colSize", String(this->colSize).c_str(), 1);

	Serial.print(F("    "));
	for (int i = 0; i < colSize; i++) {
		Serial.print(ppCols[i]);
		if (i == colSize - 1) {
			Serial.println();
			break;
		}
		Serial.print(", ");
	}
}
