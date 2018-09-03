// TableViewManager.h

#ifndef _TABLEVIEWMANAGER_h
#define _TABLEVIEWMANAGER_h

#if defined(ARDUINO) && ARDUINO >= 100
	#include "arduino.h"
#else
	#include "WProgram.h"
#endif
#include "Config.h"
#include "CommonController.h"
#include "LcdManager.h"
#include "HIDSelector.h"
#include "NetworkManager.h"

#include <ArduinoJson.h>

#define LCD_SIZE_X	(16)
#define LCD_SIZE_Y	(2)
#define DELETE_WORD_LENGTH	(5)

//extern StaticJsonBuffer<JSON_BUFFER_SIZE> JSONBuffer;
extern LcdManager scr;
extern NetworkManager net;
extern Max_LCD lcd;



class TableViewWrapper : public CommonController {
	class TableViewRowModel
	{
	public:
		TableViewRowModel(JsonObject* jsonObj);
		char * strId;
		char * strHhMm;
		int colSize = 0;
		char ** ppCols;
		void Dispose() {
			freeChar(strId);
			freeChar(strHhMm);
			if (ppCols == NULL) return;
			for (int i = 0; i < colSize; i++) {
				freeChar(ppCols[i]);
			}
			free(ppCols);
			ppCols = NULL;
			colSize = 0;
		};
		void dumpSerial();
	};
	class TableViewModel
	{
	public:
		TableViewModel(JsonObject* jsonObj);
		int rowSize = 0;
		int8_t pageFlag = 0;
		TableViewRowModel ** ppRows;
		void Dispose() {
			if (ppRows == NULL) return;
			for (int i = 0; i < rowSize; i++) {
				ppRows[i]->Dispose();
				delete ppRows[i];
				ppRows[i] = NULL;
			}
			free(ppRows);
			ppRows = NULL;
			rowSize = 0;
		};
		void dumpSerial();
	};
public:
	TableViewWrapper(CommonController * pBackController);
	TableViewWrapper(char *listInfoJson, CommonController * pBackController = NULL);
	~TableViewWrapper();
	void Dispose() {
		if (pModel != NULL) pModel->Dispose();
		delete pModel;
		pModel = NULL;
	};
	void dumpSerial() { 
		if (pModel == NULL) return;
		pModel->dumpSerial();
	};

	void onChange() { this->valueChange(); };
	void onBarcodeEnter();
	void onTenkeyEnter();
	void resume() {};
	void updateLcd();


private:
	void refreshModel();
	void parseResponseJson(const char* json);

	void getList(char* buf, int bufsize, int direction = 0, const char* strId = NULL);
	void getOtherPage(int8_t direction);
	void postUpdate();
	void postDelete();

	void _postCommon(JsonObject& JSONencoder, char* responseBuf, int bufsize);
	void _printPostResponse(String &payload, char * responseBuf, int bufsize);
	void valueChange();

	void _updateLcdCELL_SELECT();
	void _updateLcdVALUE_EDIT();
	static void freeChar(char* ptr) {
		if (ptr == NULL) return;
		free(ptr);
		ptr = NULL;
	};
	static void copyChar(char** ppTo, const char* ptrFrom) {
		if (ptrFrom == NULL) {
			*ppTo = NULL;
		}
		else {
			*ppTo = (char*)malloc(strlen(ptrFrom) + 1);
			strcpy(*ppTo, ptrFrom);
		}
	}
	static void D(const char* legend, const char* value, int indet = 0) {
		for (int i = 0; i < indet; i++) {
			Serial.print("  ");
		}
		Serial.print(legend);
		Serial.print(": ");
		Serial.println(value);
	};
	static void D(const char* msg) { Serial.println(msg); }
	const int JSON_BUF_LENGTH = 1024*5;
	TableViewModel * pModel = NULL;
	int8_t posX = 0;
	int8_t posY = 0;
	int8_t lcdPosY = 0;

	enum InputState{
		CELL_SELECT,
		VALUE_EDIT,
		ROW_DELETE
	};
	InputState state = CELL_SELECT;
	CommonController * pBackController = NULL;
	
};

#endif

