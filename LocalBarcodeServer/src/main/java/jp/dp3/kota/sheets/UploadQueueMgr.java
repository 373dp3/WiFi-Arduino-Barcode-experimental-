package jp.dp3.kota.sheets;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class UploadQueueMgr {

	private String dbpath = "";
    private Connection connection = null;
    private final String jsonValueKey = "v";
    private final JsonObject jsonObject;
    private Map<String, Long> uriToUploadMaxIdMap =  Collections.synchronizedMap(new HashMap<String, Long>());
    public final String sheetId;

    //trace,debug,info,warn,error,fatal
    Logger l = LogManager.getLogger(SheetWrapper.class);

    public UploadQueueMgr(String sqliteDbFilePath, boolean isCreateNewDb, final JsonObject json) throws ClassNotFoundException, SQLException{
		this.jsonObject = json;
		if(json!=null) {
			JsonValue value = json.get("spreadsheetId");
			if(value!=null) {
				sheetId = value.asString();
			}else{
				sheetId = "";
				l.error("cannot get sheetid from json");
			}
		}else{
			sheetId = "";
			l.info("json null, set sheetId=\"\"");
		}
		_UploadQueueMgr(sqliteDbFilePath, isCreateNewDb);
    }

	public UploadQueueMgr(String sqliteDbFilePath, final JsonObject json) throws ClassNotFoundException, SQLException{
		this.jsonObject = json;
		if(json!=null) {
			JsonValue value = json.get("spreadsheetId");
			if(value!=null) {
				sheetId = value.asString();
			}else{
				sheetId = "";
				l.error("cannot get sheetid from json");
			}
		}else{
			sheetId = "";
			l.info("json null, set sheetId=\"\"");
		}
		_UploadQueueMgr(sqliteDbFilePath, true);
	}



	protected volatile boolean isActive = false;
	private Thread workerThread = null;
	protected long maxUploadId = -1;
	private boolean isUploadDone = false;

	public void batchUpload(){
		isUploadDone = false;
		//Sheet処理インスタンス確保
		SheetWrapper wrapper = new SheetWrapper();
		//Sheet一覧取得
		List<String> nameList = getSheetNameList();
		for(String sheetName : nameList){
			if(isActive==false) { return; }
			UploadInfoResult result = null;
			try {
				//アップロード対象の情報取得
				result = getUploadInfo(sheetName, maxUploadId);
				if(result.list.size()==0){ continue; }

				//アップロード
				l.info("Upload to sheet: " + sheetName + " (" + result.list.size() + ")");
				l.trace("begin : "+result.list.get(0).toString());
				l.trace("end : "+result.list.get(result.list.size()-1).toString());
				wrapper.uploadData(this.sheetId, sheetName, result.list);
				isUploadDone = true;

				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				l.error("batchUpload InterruptedException: " + e.toString());
			} catch (Exception ex){
				ex.printStackTrace();
				l.error("batchUpload Exception: " + ex.toString());
			}
			if((result!=null) && (result.maxId > maxUploadId)) {
				l.debug("maxUploadId " + maxUploadId + " -> " + result.maxId);
				maxUploadId = result.maxId;
			}

		}
	}

	public void startPollingThread(){
		if(sheetId == null) { return; }//sheetidがなければアップロードを行わない
		if(sheetId.length() ==0) { return; }

		isActive = true;
		workerThread = new Thread(){
			@Override
			public void run() {
				super.run();
				long avoidTimeMs = -1;
				l.info("Upload interval sec: " + BootStub.c.upload.interval_sec + "[sec]");
				while(isActive){
					//処理対象時間に入っていなければ回避
					if(avoidTimeMs > System.currentTimeMillis()){
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							l.debug("startPollingThread 1 InterruptedException" + e.toString());
						}
						continue;
					}
					try {
						batchUpload();
						if(isActive==false) { break; }

						//一定期間回避処理
						if(isUploadDone){
							//avoidTimeMs = System.currentTimeMillis() + 30*1000L;//30秒に1回アップロード処理着手
							avoidTimeMs = System.currentTimeMillis() + BootStub.c.upload.interval_sec*1000L;
						}
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						l.debug("startPollingThread 2 InterruptedException" + e.toString());
					}
				}
				l.info("UploadQueueMgr polling thread exit.");
			}
		};
		workerThread.start();
	}

	public void shutdonw(){
		isActive = false;
		if(workerThread!=null){
			try {
				workerThread.join(10000L);
			} catch (InterruptedException e) {
				l.debug("shutdonw InterruptedException" + e.toString());
				e.printStackTrace();
			}
		}
	}

	private void _UploadQueueMgr(String sqliteDbFilePath, boolean isCreateNewDb) throws ClassNotFoundException, SQLException{
		this.dbpath = sqliteDbFilePath;
		File file = new File(this.dbpath);
		if(file.exists() && isCreateNewDb){
			file.delete();
		}

		//DB接続
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:"+file.getAbsolutePath());
        Statement statement = connection.createStatement();
        statement.setQueryTimeout(30);

		/*
		CREATE TABLE [UploadQueue] (
			[id] INTEGER PRIMARY KEY AUTOINCREMENT,
			[unixtimeMs] INTEGER NOT NULL,
			[uri] TEXT NOT NULL,
			[json] TEXT NOT NULL
		);
		 */
        String sql = "CREATE TABLE [UploadQueue] ([id] INTEGER PRIMARY KEY AUTOINCREMENT,[unixtimeMs] INTEGER NOT NULL,[uri] TEXT NOT NULL,[mac] TEXT NOT NULL,[json] TEXT,[tr] TEXT);";
        try{
            statement.execute(sql);
        }catch(Exception e){
        	if(isCreateNewDb){
    			l.debug("_UploadQueueMgr Exception on sql statement" + e.toString() + " //" + e.getMessage());
        	}
        }
        statement.close();
	}

	public boolean isOpen(){
		if((connection != null)) { return true; }
		return false;
	}

	public void close(){
		try{
			if(connection!=null) {
				connection.close();
				connection = null;
			}
		}catch(Exception e){}

	}

	public void append(ScreenClientSession session, ClientToLocalServer client){
		String sql = "INSERT INTO UploadQueue(unixtimeMs,uri,mac,json, tr) VALUES(?,?,?,?,?)";

		try(PreparedStatement pstate = this.connection.prepareStatement(sql)){

			pstate.setLong(1, System.currentTimeMillis());
			pstate.setString(2, client.uri);
			pstate.setString(3, client.mac);

			JsonObject obj = new JsonObject();
			JsonArray ary = new JsonArray();
			StringBuffer sb = new StringBuffer("<tr>");
			for(String s : session.cellList){
				ary.add(s);

				sb.append("<td>"+s+"</td>");
			}
			sb.append("</tr>");
			obj.set(jsonValueKey, ary);
			pstate.setString(4, obj.toString());
			pstate.setString(5, sb.toString());

			pstate.executeUpdate();
			pstate.close();

		}catch(Exception e){
			l.debug("append Exception on sql statement" + e.toString() + " //" + e.getMessage());
		}

	}

	enum DataType {
		DATETIME,
		NUMBER,
		STRING
	};

	public void dumpDbToXlsx(OutputStream stream){
		if(jsonObject==null) { return; }

		JsonValue value = jsonObject.get("valueRanges");
		if(value == null) { return; }

		String sql = "SELECT tr, unixtimeMs FROM UploadQueue WHERE uri = ? ORDER BY id";



		Workbook book = null;
		try {
            book = new SXSSFWorkbook();
            Font font = book.createFont();
            font.setFontName("ＭＳ ゴシック");
            font.setFontHeightInPoints((short) 11);
            DataFormat format = book.createDataFormat();

            //日時表示用のスタイル
            CellStyle style_datetime = book.createCellStyle();
            style_datetime.setDataFormat(format.getFormat("yyyy/mm/dd hh:mm:ss"));
            style_datetime.setVerticalAlignment(VerticalAlignment.TOP);
            style_datetime.setFont(font);

            Row row;
            int rowNumber;
            Cell cell;
            int colNumber;

            //シートの作成(3シート作ってみる)
            Sheet sheet;

    		JsonArray ary = value.asArray();
    		for(int i=0; i<ary.size(); i++){
    			JsonValue sheetValue = ary.get(i);
    			if(sheetValue == null) { continue; }
    			JsonObject obj = sheetValue.asObject();
    			JsonValue jsonValueSheetName = obj.get("range");
    			if(jsonValueSheetName==null) { continue; }
    			JsonValue jsonValueValues = obj.get("values");
    			if(jsonValueValues==null) { continue; }
    			String sheetName = jsonValueSheetName.asString();	//シート名
    			sheetName = sheetName.split("!")[0];
    			String sheetNameOrg = sheetName;
    			sheetName = sheetName.replaceAll("'", "");
    			JsonArray valuesAry = jsonValueValues.asArray();

    			List<String> legendAry = new ArrayList<String>();
    			List<DataType> dateFlgAry = new ArrayList<DataType>();

    			//先頭(j=0)は凡例の為、除外
    			for(int j=1; j<valuesAry.size(); j++){
    				JsonValue jsonValueCols = valuesAry.get(j);
    				if(jsonValueCols==null) { continue; }
    				JsonArray colsAry = jsonValueCols.asArray();
    				if(colsAry.size()<1) { continue; }
    				legendAry.add(colsAry.get(0).asString());
    				DataType type = DataType.STRING;
    				if(colsAry.get(6).asString().compareTo("DateTime")==0){
    					type = DataType.DATETIME;
    				}
    				if(colsAry.get(6).asString().compareTo("DeltaTimeMs")==0){
    					type = DataType.NUMBER;
    				}
    				if(colsAry.get(6).asString().compareTo("TripTimeMs")==0){
    					type = DataType.NUMBER;
    				}
    				dateFlgAry.add(type);
    			}

    			//シート出力
                sheet = book.createSheet();
                if (sheet instanceof SXSSFSheet) {
                    ((SXSSFSheet) sheet).trackAllColumnsForAutoSizing();
                }
                book.setSheetName(i, sheetName);
                //ヘッダ行の作成
                rowNumber = 0;
                colNumber = 0;

        		//ヘッダ出力
                row = sheet.createRow(rowNumber);
                for(int j=0; j<legendAry.size(); j++){
                    cell = row.createCell(colNumber++);
                    cell.setCellValue(legendAry.get(j));
                }

        		try(PreparedStatement pstate = this.connection.prepareStatement(sql)){
        			pstate.setString(1, sheetNameOrg);
        			ResultSet rs = pstate.executeQuery();
                    rowNumber = 0;
                    colNumber = 0;
        			while(rs.next()){
        				rowNumber++;
                        row = sheet.createRow(rowNumber);

        				//HTML情報を分解して配列を作成する。
        				String oldHtmlTr = rs.getString(1);
        				oldHtmlTr = oldHtmlTr.substring("<tr><td>".length(), oldHtmlTr.length() - "</td></tr>".length());
        				String[] trAry = oldHtmlTr.split("</td><td>");
        				for(int j=0; j<trAry.length; j++){
        					DataType type = dateFlgAry.get(j);
                            cell = row.createCell(j);
        					switch (type) {
							case DATETIME:
			                    cell.setCellStyle(style_datetime);
			                    cell.setCellType(CellType.STRING);
	                            cell.setCellValue(new Date(rs.getLong(2)));
								break;
							case NUMBER:
								if(trAry[j].length()>0){
				                    cell.setCellType(CellType.NUMERIC);
		                            cell.setCellValue(Double.parseDouble(trAry[j]));
								}else{
				                    cell.setCellType(CellType.BLANK);
								}
								break;
							default:
								try{
		                            cell.setCellValue(Double.parseDouble(trAry[j]));
				                    cell.setCellType(CellType.NUMERIC);
								}catch(Exception e){
				                    cell.setCellType(CellType.STRING);
		                            cell.setCellValue(trAry[j]);
								}
							}
        				}
        			}
        		}catch(Exception e){
        			e.printStackTrace();
        			l.error(e.toString());
        		}


    		}

            book.write(stream);

		}catch(Exception e){
			l.error("Error on dumpDbToXlsx : "+e.toString());
		}finally{
            if (book != null) {
                try {
                    ((SXSSFWorkbook) book).dispose();
                }
                catch (Exception e2) {
                }
            }
		}

	}

	final SimpleDateFormat sdfHHMM = new SimpleDateFormat("HH:mm");

	public String getListJson(ScreenMgr mgr, String uri, String mac, long id, int dir, int sizelimit){
		int defaultRowCount = 8;
		String sql = "SELECT id, unixtimeMs, json FROM UploadQueue WHERE uri = ?  and id < ? ORDER BY id DESC limit "
					+ defaultRowCount;

		if(dir>0){//指定IDよりも新しい情報の取得
			sql = "SELECT id, unixtimeMs, json FROM "
				+ "(SELECT id, unixtimeMs, json FROM UploadQueue WHERE uri = ?  and id > ? ORDER BY id limit "
				+ defaultRowCount + ")"
				+ " ORDER BY id DESC";
		}

		l.debug("dir:" + dir + " id:" + id);
		l.debug("sql:" + sql);

		// +"\"d\":[ {\"id\": 0, \"hhmm\": \"11:1"+ofs+"\", \"cols\":[ \"AAAA\",\"10\",\"0\" ] } ,"
		StringBuffer sb = new StringBuffer(" \"d\":[ ");
		Calendar cal = Calendar.getInstance();

		//必要な配列の取得
		List<Integer> targetColsList = getTargetColList(mgr, uri);

		//DBの結果処理
		long minId = Long.MAX_VALUE;
		long maxId = Long.MIN_VALUE;
		try(PreparedStatement pstate = this.connection.prepareStatement(sql)){
			pstate.setString(1, uri);
			pstate.setLong(2, id);
			ResultSet rs = pstate.executeQuery();
			boolean isFirstLoop = true;
			int preSize = -1;
			if (targetColsList.size()>0) while(rs.next()){
				long tmpId = rs.getLong(1);
				if(isFirstLoop){
					sb.append("{ \"id\": " + tmpId);
					isFirstLoop = false;
				}else{
					sb.append(", { \"id\": " + tmpId);
				}
				if(minId > tmpId) { minId = tmpId;}
				if(maxId < tmpId) { maxId = tmpId;}

				//時刻のみの取得
				cal.setTimeInMillis(rs.getLong(2));
				sb.append(", \"hhmm\": \"" + sdfHHMM.format(cal.getTime()) +"\"");

				//jsonをパースし、InputFreqがNの項目のみを抽出する
				JsonObject jsonDb = Json.parse(rs.getString(3)).asObject();
				if(jsonDb == null) { continue; }
				JsonArray aryDb = jsonDb.get("v").asArray();
				if(aryDb == null) { continue; }
				//	-- colsのビルド --
				sb.append(", \"cols\": [\"" + aryDb.get(targetColsList.get(0)).asString() + "\"");
				for(int i=1; i<targetColsList.size(); i++){
					sb.append(", \"" + aryDb.get(targetColsList.get(i)).asString() + "\"");
				}
				sb.append("]}");

				//前ループとのサイズ増加分をもとに、前回と同レベルのマージンが無いのであれば現ループで終了する。
				int tmpLen = sb.length();
				if(preSize>0) {
					if(tmpLen > sizelimit - (tmpLen - preSize)) { break; }
				}
				preSize = tmpLen;

			}//while loop end
		} catch (SQLException e) {
			e.printStackTrace();
			l.debug("getSheetNameList SQLException on sql statement" + e.toString() + " //" + e.getMessage());
		}//DB処理ループ

		sb.append("] ");

		//次ページフラグの準備
		int pflg = 0;

		//minIdよりも小さい値(古い値)があるか？
		sql = "SELECT id FROM UploadQueue WHERE uri = ?  and id < ? ORDER BY id DESC limit 1";
		try(PreparedStatement pstate = this.connection.prepareStatement(sql)){
			pstate.setString(1, uri);
			pstate.setLong(2, minId);
			ResultSet rs = pstate.executeQuery();
			if (targetColsList.size()>0) while(rs.next()){
				pflg += 2;
				break;
			}//while loop end
		} catch (SQLException e) {
			e.printStackTrace();
			l.debug("getSheetNameList SQLException on sql statement" + e.toString() + " //" + e.getMessage());
		}//DB処理ループ

		//maxIdよりも大きい値(新しい値)があるか？
		sql = "SELECT id FROM "
				+ "(SELECT id FROM UploadQueue WHERE uri = ?  and id > ? ORDER BY id limit 1)"
				+ " ORDER BY id DESC";
		try(PreparedStatement pstate = this.connection.prepareStatement(sql)){
			pstate.setString(1, uri);
			pstate.setLong(2, maxId);
			ResultSet rs = pstate.executeQuery();
			if (targetColsList.size()>0) while(rs.next()){
				pflg += 1;
				break;
			}//while loop end
		} catch (SQLException e) {
			e.printStackTrace();
			l.debug("getSheetNameList SQLException on sql statement" + e.toString() + " //" + e.getMessage());
		}//DB処理ループ

		l.debug("{"
				+ "\"pflg\": \""+pflg+"\", "//ページ遷移ビットフラグ。 bit1:上(新)方向遷移可能。bit2:下(旧)方向遷移可能
				+ sb.toString()
				+"}");

		return "{"
				+ "\"pflg\": \""+pflg+"\", "//ページ遷移ビットフラグ。 bit1:上(新)方向遷移可能。bit2:下(旧)方向遷移可能
				+ sb.toString()
				+"}";
//		return sb.toString();
	}

	private List<Integer> getTargetColList(ScreenMgr mgr, String uri) {
		List<Integer> targetColsList = new ArrayList<Integer>();
		List<ColumnScreen> cols = mgr.getColumnListBySheetName(uri);
		for(int i=0; i<cols.size(); i++){
			if(cols.get(i).freq != ColumnScreen.Freq.N) { continue; }
			targetColsList.add(i);
		}
		return targetColsList;
	}

	public void update(ScreenMgr mgr, String uri, long id, int posX, String updateValue) throws IOException{
		/* 実装方針
		 * ・現時点での情報を取得
		 * ・InputFreqから対象配列を取得
		 * ・Jsonパース
		 * ・該当キーの更新
		 * ・DB内のJSONフィールドを更新
		 * */
		//現状取得
		String oldHtmlTr = null;
		{
			String sql = "SELECT tr FROM UploadQueue WHERE id = ? limit 1";

			//Inner DB
			// json
			//{"v":["2018/08/18 18:37:36","AABBCCDD","","StaffName_bc","Location_bc","ItemCode_bc","Hinban_bc","10","1"]}
			// tr
			//<tr><td>2018/08/18 18:37:36</td><td>AABBCCDD</td><td></td><td>StaffName_bc</td><td>Location_bc</td><td>ItemCode_bc</td><td>Hinban_bc</td><td>10</td><td>1</td></tr>

			//必要な配列の取得
			List<Integer> targetColsList = getTargetColList(mgr, uri);

			//DBの結果処理
			try(PreparedStatement pstate = this.connection.prepareStatement(sql)){
				pstate.setLong(1, id);
				ResultSet rs = pstate.executeQuery();
				if (targetColsList.size()>0) while(rs.next()){
					oldHtmlTr = rs.getString(1);
				}//while loop end
			} catch (SQLException e) {
				e.printStackTrace();
				l.debug("getSheetNameList SQLException on sql statement" + e.toString() + " //" + e.getMessage());
			}//DB処理ループ
		}
		if(oldHtmlTr==null) {
			throw new IOException("Cannot find target row in DB via id:" + id);
		}

		//HTML情報を分解して配列を作成する。
		oldHtmlTr = oldHtmlTr.substring("<tr><td>".length(), oldHtmlTr.length() - "</td></tr>".length());
		String[] trAry = oldHtmlTr.split("</td><td>");

		//更新位置配列の取得と上書き
		List<Integer> targetColsList = getTargetColList(mgr, uri);
		if((posX<0) || (posX > targetColsList.size()-1)) {
			throw new IOException("Out of bounds in targetColsList, posX:" + posX);
		}
		trAry[targetColsList.get(posX)] = updateValue;

		//変数jsonを更新
		StringBuffer jsonSb = new StringBuffer("{\"v\":[");
		jsonSb.append("\""+trAry[0]+"\"");
		for(int i=1; i<trAry.length; i++){
			jsonSb.append(", \""+trAry[i]+"\"");
		}
		jsonSb.append("]}");

		//変数oldHtmlTrを更新
		StringBuffer htmlSb = new StringBuffer("<tr><td>" + trAry[0]);
		for(int i=1; i<trAry.length; i++){
			htmlSb.append("</td><td>"+trAry[i]);
		}
		htmlSb.append("</td></tr>");

		//DBへUPDATE
		{
			String sql = "UPDATE UploadQueue set json = ?, tr = ? where id = ? ";

			try(PreparedStatement pstate = this.connection.prepareStatement(sql)){

				pstate.setString(1, jsonSb.toString());
				pstate.setString(2, htmlSb.toString());
				pstate.setLong(3, id);

				pstate.executeUpdate();
				pstate.close();

			}catch(Exception e){
				l.debug("update Exception on sql statement" + e.toString() + " //" + e.getMessage());
			}
		}
	}

	public void delete(ScreenMgr mgr, long id) throws IOException{
		String sql = "DELETE FROM UploadQueue where id = ? ";

		try(PreparedStatement pstate = this.connection.prepareStatement(sql)){

			pstate.setLong(1, id);

			pstate.executeUpdate();
			pstate.close();

		}catch(Exception e){
			l.debug("delete Exception on sql statement" + e.toString() + " //" + e.getMessage());
		}
	}



	public String getHtmlTable(String uri){
		String sql = "SELECT tr FROM UploadQueue WHERE uri = ? ORDER BY id limit 5000";
		StringBuffer sb = new StringBuffer("<html><body><table>");

		try(PreparedStatement pstate = this.connection.prepareStatement(sql)){
			pstate.setString(1, uri);
			ResultSet rs = pstate.executeQuery();

			while(rs.next()){
				sb.append(rs.getString(1));
				sb.append("\r\n");
			}
		} catch (SQLException e) {
			e.printStackTrace();
			l.debug("getSheetNameList SQLException on sql statement" + e.toString() + " //" + e.getMessage());
		}
		sb.append("</table></body></html>");

		return sb.toString();
	}

	public List<String> getSheetNameList(){
		String sql = "SELECT uri FROM UploadQueue GROUP BY uri";
		List<String> list = new ArrayList<String>();

		try(Statement stat = connection.createStatement();
			ResultSet rs = stat.executeQuery(sql)){

			while(rs.next()){
				list.add(rs.getString(1));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			l.debug("getSheetNameList SQLException on sql statement" + e.toString() + " //" + e.getMessage());
		}

		return list;
	}

	/**
	 * 指定したIDを超えるIDのアップロード情報を取得
	 * @param sheetName
	 * @param gtId
	 * @return
	 */
	public UploadInfoResult getUploadInfo(String sheetName, long gtId){
		String sql = "SELECT json, id as mid FROM UploadQueue WHERE uri=? and id>? order by id";
		List<List<Object>> resultList = new ArrayList<List<Object>>();
		UploadInfoResult ans = new UploadInfoResult();
		ans.list = resultList;

		try(PreparedStatement pstat = connection.prepareStatement(sql);){
			pstat.setString(1, sheetName);
			pstat.setLong(2, gtId);
			ResultSet rs = pstat.executeQuery();

			while(rs.next()){
				ans.maxId = rs.getLong(2);

				List<Object> list = new ArrayList<Object>();
				list.add("");//先頭はメタデータの識別子用スペース
				JsonObject obj = Json.parse(rs.getString(1)).asObject();
				JsonArray ary = obj.get(this.jsonValueKey).asArray();
				for(JsonValue val : ary){
					list.add(val.asString());
				}
				resultList.add(list);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			l.debug("getUploadInfo SQLException on sql statement" + e.toString() + " //" + e.getMessage()
					+ " /// (sheetName: "+ sheetName +", igId:" + gtId+")");
		}

		return ans;
	}

	/**
	 * アップロード用のオブジェクトリストを全取得
	 * @param sheetName
	 * @return
	 */
	public List<List<Object>> getUploadInfo(String sheetName){

		return getUploadInfo(sheetName, -1).list;
	}

	public class UploadInfoResult{
		public List<List<Object>> list = null;
		public long maxId=-1;
	}
}
