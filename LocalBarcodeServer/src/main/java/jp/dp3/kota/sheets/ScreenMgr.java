package jp.dp3.kota.sheets;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.dp3.kota.sheets.ColumnScreen.Freq;
import jp.dp3.kota.sheets.ColumnScreen.Method;
import jp.dp3.kota.sheets.ScreenClientSession.CheckResult;
import jp.dp3.kota.sheets.exception.InputMethodException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class ScreenMgr {
	final JsonObject sheetJsonObj;
	private UploadQueueMgr queueMgr = null;

	//trace,debug,info,warn,error,fatal
	Logger l = LogManager.getLogger(ScreenMgr.class);

	//MACアドレスをキーにセッションを返す
	Map<String, ScreenClientSession> sessionMap = Collections.synchronizedMap(new HashMap<String, ScreenClientSession>());

	public ScreenMgr(String json, String dbPath) throws ClassNotFoundException, SQLException{
		l.trace(json.toString());
		sheetJsonObj = Json.parse(json.toString()).asObject();
		queueMgr = new UploadQueueMgr(dbPath, sheetJsonObj);

		queueMgr.startPollingThread();

		cfg = new ServerConfig(null);
		l.trace("Sever config null");

		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run(){
				System.out.println("Shutting down queueMgr...");
				l.info("Shutting down queueMgr");
				queueMgr.shutdonw();
			}

		});

	}

	private ServerConfig cfg;
	public ScreenMgr(String json, String dbPath, final ServerConfig cfg) throws ClassNotFoundException, SQLException{
		l.trace(json.toString());
		sheetJsonObj = Json.parse(json.toString()).asObject();
//		queueMgr = new UploadQueueMgr(dbPath, sheetJsonObj);
		queueMgr = new UploadQueueMgr(dbPath, false, sheetJsonObj);

		queueMgr.startPollingThread();


		ShutDownThread th = new ShutDownThread();

		//終了検出フック
		Runtime.getRuntime().addShutdownHook(th);

		this.cfg = cfg;
		l.trace("Sever config enable");

		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run(){
				System.out.println("Shutting down queueMgr...");
				l.info("Shutting down queueMgr");
				queueMgr.shutdonw();
			}

		});
	}

	class ShutDownThread extends Thread{
		public void run() {
			final SimpleDateFormat sdfHHMM = new SimpleDateFormat("yyyyMMdd_HHmm");
			String fileName = sdfHHMM.format(new Date()) + ".xlsx";
			System.out.println(fileName);
			try{
				FileOutputStream fos = new FileOutputStream(fileName);
				queueMgr.dumpDbToXlsx(fos);
				fos.close();
				fos = null;
			}catch(Exception e){
			}
		}
	}


	public String getHtmlTableFromDb(String uri){
		String arg;
		try{
			Long.parseLong(uri);
			//エラーがなければ前後に'をつける
			arg = "'"+uri+"'";
		}catch(Exception e){
			arg = uri;
		}
		return queueMgr.getHtmlTable(arg);
	}

	public void dumpXlsx(OutputStream stream){
		queueMgr.dumpDbToXlsx(stream);
	}

	/**
	 * シート名の一覧を取得する
	 * @return
	 */
	public List<String> getSheetNameList(){
		List<String> list = new ArrayList<String>();

		l.debug("getSheetNameList");
		JsonValue jv = sheetJsonObj.get("valueRanges");
		if(jv == null) { return null; }
		for(JsonValue val : jv.asArray()){
			JsonValue range = val.asObject().get("range");
			if(range==null) {
				list.add("");
			}else{
				String[] names = range.asString().split("\\!");
				list.add(names[0]);
			}
		}
		l.trace("getSheetNameList list: " + list);
		return list;
	}

	/**
	 * シート名の配列番号を取得
	 * @param name
	 * @return
	 */
	public int getSheetIndexByName(String name){
		List<String> list = getSheetNameList();
		for(int i=0; i<list.size(); i++){
			String sheetName = list.get(i);
			if(name.compareTo(sheetName)==0){ return i; }
		}
		return -1;
	}

	/**
	 * 指定したシートの画面遷移情報を取得
	 * @param name
	 * @return
	 */
	public List<ColumnScreen> getColumnListBySheetName(String name) throws ArrayIndexOutOfBoundsException{
		l.trace("getColumnListBySheetName name:" + name);
		List<ColumnScreen> list = new ArrayList<ColumnScreen>();
		int index = getSheetIndexByName(name);
		JsonArray sheetAry = sheetJsonObj.get("valueRanges").asArray().get(index).asObject().get("values").asArray();
		if(sheetAry == null) {
			l.debug("sheetAry null, return null");
			return null;
		}
		JsonArray defAry = sheetAry.get(0).asArray();

		for(int i=1; i<sheetAry.size(); i++){
			ColumnScreen tmpScr = new ColumnScreen(sheetAry.get(i).asArray(), defAry);
			tmpScr.jsonArrayIndex = i;
			list.add(tmpScr);
		}

		if(list.size()>0){
			list.get(list.size()-1).isLastScreen = true;
		}

		l.trace(list.toString());

		return list;
	}

	/**
	 * 次のスクリーン情報を取得する
	 * @param name
	 * @param client
	 * @return
	 */
	public ColumnScreen getNextScreen(ScreenClientSession session, ClientToLocalServer client){
		return getNextScreen(session, client, false);
	}

	/**
	 * 次のスクリーン情報を取得する(維持制御付き)
	 * @param session
	 * @param client
	 * @param isKeepScreen
	 * @return
	 */
	public ColumnScreen getNextScreen(ScreenClientSession session, ClientToLocalServer client, boolean isKeepScreen){
		List<ColumnScreen> scrList = null;
		try{
			scrList = getColumnListBySheetName(client.uri);
		}catch(ArrayIndexOutOfBoundsException e){
			l.trace("getColumnListBySheetNameにてArrayIndexOutOfBoundsException : " + client.toString());
			throw e;
		}
		if(scrList == null) {
			l.info("scrList null, return null uri: " + client.toString());
			return null;
		}

		//一致するスクリーンがあれば、その次のスクリーンを採用
		boolean isHit = false;
		for(int i=0; i<scrList.size(); i++){
			if(scrList.get(i).Name.compareTo(client.name)==0){
				isHit = true;
				if(isKeepScreen){ //入力エラー等で画面維持の場合
					l.debug("isKeepScreen true");
					return scrList.get(i);
				}
				continue;
			}
			if(isHit && (scrList.get(i).freq != Freq.ZERO)){//最終行にTripTimeMs追加対応のため、ZERO判定追加
				//繰り返し入力時の入力済み情報はスキップする
				if(session.cellList.get(i).length()>0){
					continue;
				}
				return scrList.get(i);
			}
		}

		//一致しなければ最初のスクリーンを採用
		for(int i=0; i<scrList.size(); i++){
			if(scrList.get(i).freq == Freq.ZERO) { continue; }
			//繰り返し入力時の入力済み情報はスキップする
			if(session.cellList.get(i).length()>0){
				continue;
			}
			return scrList.get(i);
		}

		l.error("getNextScreen null nomatch");
		return null;
	}

	/**
	 * セッションMAPからClientに対応するセッション情報を取得
	 * @param client
	 * @return
	 */
	public ScreenClientSession getSession(ClientToLocalServer client){
		try{
			synchronized(sessionMap){
				if(sessionMap.containsKey(client.mac)==false){
					//新規セッションを生成する。
					ScreenClientSession session = new ScreenClientSession(client);
					sessionMap.put(client.mac, session);
					l.debug("Create new session :: client:" +client.toString());
				}else{
					l.trace("Use session cache: " + client.toString());
				}
				ScreenClientSession session = sessionMap.get(client.mac);
				if(session.check(client) != CheckResult.ALL_OK){
					session.clearSession(client);
				}

				return session;
			}
		}catch(Exception e){
			l.error("getSession Exception :" + e.toString() + " // " + e.getMessage() + " /// " + client.toString());
		}

		return null;
	}

	/**
	 * クライアント情報をもとにセッションを更新する
	 * @param session
	 * @param client
	 * @return 1行分の情報が確定した場合は true
	 * @throws InputMethodException
	 */
	public boolean updateSession(ScreenClientSession session, ClientToLocalServer client) throws ArrayIndexOutOfBoundsException, InputMethodException{
		List<ColumnScreen> colList = this.getColumnListBySheetName(client.uri);
		session.prepareCellListIfNeed(colList);

		// 0 の情報はアップロード時に処理する。ここでは1とNのみ。
		for(int i=0; i<colList.size(); i++){
			if(colList.get(i).Name.compareTo(client.name)!=0) { continue; }

			//[TODO]入力条件Any、Barcode、Numpadの処理が必要かの判断
			//一致した場合、情報を格納する(バーコード優先)
			if(client.barcode.length()>0){
				//キャンセル処理の判定
				if(client.barcode.compareTo("CA$%")==0){
					client.name = "";
					clearRepeatInputSession(session, client);
					return false;
				}
				if(colList.get(i).method == Method.NUMPAD){
					l.debug("Numpad required but barcode input.");
					throw new InputMethodException("Numpad required but barcode input.");
				}
				session.cellList.set(i, client.barcode);
			}else{
				//System.out.println("["+i+"] Update numpad: " + client.numpad);
				session.cellList.set(i, client.numpad);
			}

			//入力開始時刻が未更新であれば更新
			session.updateN1stTimeMs();

			//最終情報の判定
			if(i==colList.size()-1){ return true; }
			//残りの情報が全て0の場合、最終情報と同等として処理
			boolean isZero = true;
			i++;
			for(;i<colList.size(); i++){
				if(colList.get(i).freq !=  Freq.ZERO){
					isZero = false;
					break;
				}
			}
			if(isZero) { return true; }
			break;
		}
		return false;
	}

	public SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	/**
	 * ハードウェアに関する情報をセッション情報に登録する
	 * @param session
	 * @param client
	 */
	public void updateHwInfoToSession(ScreenClientSession session, ClientToLocalServer client) {
		List<ColumnScreen> colList = this.getColumnListBySheetName(client.uri);
		for(int i=0; i<colList.size(); i++){
			ColumnScreen col = colList.get(i);
			if(col.method == Method.DATETIME){
				session.cellList.set(i, sdf.format(Calendar.getInstance().getTime()));
			}
			if(col.method == Method.MAC){
				session.cellList.set(i, client.mac);
			}
			if(col.method == Method.DELTA_MS){
				if(session.systemTimeMs==-1){
					session.cellList.set(i, "");
				}else{
					long deltaMs = System.currentTimeMillis() - session.systemTimeMs;
					session.cellList.set(i, (deltaMs) + "");
					l.debug(client.mac + " deltams:" + deltaMs);
				}
				session.systemTimeMs = System.currentTimeMillis();
			}
			if(col.method == Method.TRIPTIME){
				long tripTimeMs = System.currentTimeMillis() - session.n1stTimeMs;
				session.cellList.set(i, ""+tripTimeMs);
			}
		}

	}

	/**
	 * 繰り返し入力するセッション情報のクリア
	 * @param session
	 * @param client
	 */
	public void clearRepeatInputSession(ScreenClientSession session, ClientToLocalServer client){
		List<ColumnScreen> colList = this.getColumnListBySheetName(client.uri);

		// 0 の情報はアップロード時に処理する。ここでは1とNのみ。
		l.trace("clearRepeatInputSession");
		for(int i=0; i<colList.size(); i++){
			//System.out.println("i:" + i + " dat:" + session.cellList.get(i) + " freq:" + colList.get(i).freq);
			l.trace("i:" + i + " dat:" + session.cellList.get(i) + " freq:" + colList.get(i).freq);
			if(colList.get(i).freq == Freq.N){
				session.cellList.set(i, "");
			}
		}

		//入力所要時間計算用ミリ秒をクリア
		session.clearN1stTimeMs();

	}

	private enum Beep {
		NONE, OK, ERROR
	};

	/**
	 * ESPクライアントの情報をもとにレスポンスJSONテキストを取得する
	 * @param client
	 * @return
	 */
	public String getResponse(ClientToLocalServer client){
		l.trace("getResponse :: client:" + client.toString());

		//CRUD のCreateに関する応答
		if(client.mode.compareTo("")==0){
			//通常入力処理に対する応答
			return getInputResponse(client);
		}

		//CRUD のReadに関する応答
		if(client.mode.compareTo("list")==0){
			return getReadResponse(client);
		}

		//CRUD のUpdateに関する応答
		if(client.mode.compareTo("update")==0){
			try {
				queueMgr.update(this, client.uri, Long.parseLong(client.parse("id")),
						(int)client.parseLong("posX"), client.parse("np") + client.parse("bc"));
			} catch (NumberFormatException e) {
				return "{\"result\":\"ng\", \"msg\": \"Number format exception\"}";
			} catch (IOException e) {
				return "{\"result\":\"ng\", \"msg\": \""+e.getMessage()+"\"}";
			}
			l.debug("update id:"+client.parse("id") +" to " + client.parse("np")+client.parse("bc"));
			return "{\"result\":\"ok\"}";
		}

		//CRUD のDeleteに関する応答
		if(client.mode.compareTo("delete")==0){
			try {
				queueMgr.delete(this, Long.parseLong(client.parse("id")));
			} catch (NumberFormatException e) {
				return "{\"result\":\"ng\", \"msg\": \""+e.getMessage()+"\"}";
			} catch (IOException e) {
				return "{\"result\":\"ng\", \"msg\": \""+e.getMessage()+"\"}";
			}
			l.debug("delete id:"+client.parse("id"));
			return "{\"result\":\"ok\"}";
		}

		return null;
	}

	private String getReadResponse(ClientToLocalServer client) {
		String ofs = client.parse("ofs");
		if(ofs.length() == 0) { ofs = "0"; }
		String idStr = client.parse("id");
		l.debug("direction:" + client.parse("dir")  + " id:" + idStr);

		Long id = Long.MAX_VALUE;
		if(idStr.length()>0){
			id = Long.parseLong(idStr);
		}

		int dir = 0;
		String dirStr = client.parse("dir");
		if(dirStr.length()>0){
			dir = Integer.parseInt(dirStr);
		}

		return queueMgr.getListJson(this, client.uri, client.mac, id, dir, 3840);
		/*
		return "{"
			+ "\"pflg\": \"3\", "//ページ遷移ビットフラグ。 bit1:上(新)方向遷移可能。bit2:下(旧)方向遷移可能
			+ queueMgr.getListJson(this, client.uri, client.mac, id, dir, 3840)
			+"}";
		//*/
	}

	private String getInputResponse(ClientToLocalServer client) {
		String res;
		//セッションプールからセッションを取得
		ScreenClientSession session = getSession(client);
		boolean isKeepScreen = false;
		Beep beep = Beep.NONE;

		try{
			//セッションの更新
			if(updateSession(session, client)) {
				//---- 1行分の情報を全て格納した場合にtrue ----
				beep = Beep.OK;
				l.debug("beep ok :" + client.toString());

				//セッション情報にハードウェア固有情報をセット
				updateHwInfoToSession(session, client);

				//アップロードキューに登録
				queueMgr.append(session, client);

				//情報のクリア
				clearRepeatInputSession(session, client);

			}
		}catch(ArrayIndexOutOfBoundsException obe){
			//存在しないURIを受け取った場合、初回アクセスと判定する。
			JsonObject json = new JsonObject();
			json.add("bt", Long.parseLong(""+(System.currentTimeMillis()/1000L)));
			json.add("L1", "ﾓｰﾄﾞ ｦ");
			json.add("L2", "ﾆｭｳﾘｮｸｼﾃｸﾀﾞｻｲ");
			if(client.uri.length()!=0){
				appendBeepErrorJson(json);
			}
			return json.toString();
		} catch (InputMethodException e) {
			System.err.println(e.toString());
			isKeepScreen = true;//エラー時は画面遷移を抑止
			beep = Beep.ERROR;
			l.debug("InputMethodException beep error : " + client.toString());
		}

		//次の画面情報を取得
		ColumnScreen scr = getNextScreen(session, client, isKeepScreen);

		if(scr==null) {
			l.error("scr is null, return null :: client :" + client.toString());
			return null;
		}

		switch(beep){
		case NONE:
			break;
		case OK:
			JsonObject okJson = new JsonObject();
			appendBeepOkJson(okJson);
			return scr.toJson(okJson);
		case ERROR:
			JsonObject errJson = new JsonObject();
			appendBeepErrorJson(errJson);
			return scr.toJson(errJson);
		}
		res = scr.toJson();
		return res;
	}

	private void appendBeepOkJson(JsonObject object){
		JsonObject bpJson = new JsonObject();
		bpJson.add("hz", cfg.client.beep.onOk.hz);
		bpJson.add("val", cfg.client.beep.onOk.volume);
		//JsonArray bpAry = new JsonArray();
		//for(int i=0; i<1; i++) { bpAry.add(50); }
		bpJson.add("ptn", cfg.client.beep.onOk.pattern);
		object.add("beep", bpJson);
	}
	private void appendBeepErrorJson(JsonObject object){
		JsonObject bpJson = new JsonObject();
		bpJson.add("hz", cfg.client.beep.onError.hz);
		bpJson.add("val", cfg.client.beep.onError.volume);
		//JsonArray bpAry = new JsonArray();
		//for(int i=0; i<8; i++) { bpAry.add(50);bpAry.add(50); }
		//bpJson.add("ptn", bpAry);
		bpJson.add("ptn", cfg.client.beep.onError.pattern);
		object.add("beep", bpJson);
	}


}
