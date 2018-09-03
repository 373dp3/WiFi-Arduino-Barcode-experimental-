package jp.dp3.kota.sheets;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.sql.SQLException;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class LocalHttpd implements HttpHandler {
	//private static final Class<SimpleHttpd> cls = SimpleHttpd.class;
	//private static final byte[] NOT_FOUND = "<html><head><title>404 - Not Found</title></head><body>404 - Not Found</body></html>".getBytes();
	private static final byte[] SERVER_ERROR = "<html><head><title>500 - Error</title></head><body>500 - Error</body></html>".getBytes();

	//trace,debug,info,warn,error,fatal
	//import org.apache.logging.log4j.LogManager;
	//import org.apache.logging.log4j.Logger;
	Logger l = LogManager.getLogger(LocalHttpd.class);

	private ScreenMgr mgr = null;
	public LocalHttpd(){

		try {
			//String config = "sheet_config.json";
			//l.info("config: " + config);
			//mgr = new ScreenMgr(loadStringFromFile(config), BootStub.DB_PATH, BootStub.c);
			String config = "sheet_config.xlsx";
			l.info("config: " + config);
			printIpAddress();

			mgr = new ScreenMgr(
					LocalHttpd.loadJsonStringFromXlsx(config),
					BootStub.DB_PATH, BootStub.c);

		} catch (IOException e) {
			e.printStackTrace();
			l.fatal("new ScreenMgr IOException: " + e.toString());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			l.fatal("new ScreenMgr ClassNotFoundException: " + e.toString());
		} catch (SQLException e) {
			e.printStackTrace();
			l.fatal("new ScreenMgr SQLException: " + e.toString());
		}
	}

	@Override
	public void handle(HttpExchange ex) throws IOException {

		try {
			l.trace("in comming new handle");
			l.info("uri:"+ex.getRequestURI().toString());
			String[] parseUri = ex.getRequestURI().toString().split("\\/");
			if((parseUri.length>2) && (parseUri[1].compareTo("html_table")==0)){
				l.info("csv mode"); // ブラウザ対応、CSV提供


				String encoding = "Shift_JIS";
				ex.getResponseHeaders().set("Content-Type", "text/html; charset="+encoding);

				StringBuilder sb = new StringBuilder(mgr.getHtmlTableFromDb(parseUri[2]));
				ex.sendResponseHeaders(200, sb.toString().getBytes("Shift_JIS").length);
				OutputStreamWriter out = new OutputStreamWriter(ex.getResponseBody(), encoding);
				out.write(sb.toString());
				if(l.isTraceEnabled()){
					l.debug("200 ok: " + sb.toString());
				}else{
					l.info("200 ok");
				}
				out.close();
			}else if((parseUri.length>=2) && (parseUri[1].compareTo("dump_xlsx")==0)){
				ByteArrayOutputStream bos = null;
				try{
					bos = new ByteArrayOutputStream();
					mgr.dumpXlsx(bos);
					byte[] binary = bos.toByteArray();
					ex.getResponseHeaders().set("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheetl; ");
					ex.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"dump.xlsx\"");
					ex.sendResponseHeaders(200, binary.length);
					ex.getResponseBody().write(binary);

				}catch(Exception e){

				}finally{
					if(bos!=null){
						bos.close();
						bos = null;
					}
				}

			}else{
				//ESP対応
				ClientToLocalServer client = new ClientToLocalServer(ex.getRequestBody());
				client.uri = ex.getRequestURI().toString().replaceAll("\\/", "");
				l.info(client.toString());

				String encoding = "Shift_JIS";
				ex.getResponseHeaders().set("Content-Type", "text/json; charset="+encoding);

				StringBuilder sb = new StringBuilder(mgr.getResponse(client));
				ex.sendResponseHeaders(200, sb.toString().getBytes("Shift_JIS").length);
				OutputStreamWriter out = new OutputStreamWriter(ex.getResponseBody(), encoding);
				out.write(sb.toString());
				if(l.isTraceEnabled()){
					l.debug("200 ok: " + sb.toString());
				}else{
					l.info("200 ok");
				}
				out.close();

			}
			ex.close();

		} catch (Exception e) {
			OutputStream out = ex.getResponseBody();
			System.err.println("error\n");
			l.error("error on handle : " + e.toString() + "//" + e.getMessage());
			ex.sendResponseHeaders(500, SERVER_ERROR.length);
			out.write(SERVER_ERROR);
			out.close();
			ex.close();
		}
	}

	public void printIpAddress(){
		java.util.Enumeration enuIfs;
		try {
			enuIfs = NetworkInterface.getNetworkInterfaces();

			if (null != enuIfs)
			{
			    while (enuIfs.hasMoreElements())
			    {
			        NetworkInterface ni = (NetworkInterface)enuIfs.nextElement();
			        java.util.Enumeration enuAddrs = ni.getInetAddresses();
			        boolean isHeader = true;
			        while (enuAddrs.hasMoreElements())
			        {
			            InetAddress in4 = (InetAddress)enuAddrs.nextElement();
			            String ipStr = in4.getHostAddress();
			            if(ipStr.length()>15) { continue; }
			            if(ipStr.compareTo("127.0.0.1")==0) { continue; }
			            if(ipStr.compareTo("0:0:0:0:0:0:0:1")==0) { continue; }
			            if(ni.getDisplayName().contains("Virtual")) { continue; }
			            l.warn(ni.getDisplayName() + " ip:" + ipStr);
			        }
			    }
			}
		} catch (SocketException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}

	public static String loadJsonStringFromXlsx(String filePath) throws IOException{
		String ans = "";

		try {
			Workbook workbook = WorkbookFactory.create(new File(filePath));
			FormulaEvaluator feval = workbook.getCreationHelper().createFormulaEvaluator();

			final JsonObject bookJson = new JsonObject();
			bookJson.add("spreadsheetId", "");
			final JsonArray valueRanges = new JsonArray();
			bookJson.add("valueRanges", valueRanges);

			int sheetSize = workbook.getNumberOfSheets();
			StringBuffer sb = new StringBuffer();
			for(int i=0; i<sheetSize; i++){
				Sheet sheet = workbook.getSheetAt(i);
				sb.append(sheet.getSheetName() + " ");
				final JsonObject sheetJson = new JsonObject();

				sheetJson.add("majorDimension", "COLUMNS");
				String ragneValue = "";
				try{
					Long.parseLong(sheet.getSheetName());
					//例外が発生しない場合は、前後に'をつける
					ragneValue = "'"+sheet.getSheetName()+"'!A1:Z10";
				}catch(Exception e){
					ragneValue = sheet.getSheetName()+"!A1:Z10";
				}
				sheetJson.add("range", ragneValue);

				final JsonArray valuesJson = new JsonArray();
				//todo
				for(int j=0; j<100; j++){
					final JsonArray cellArray = new JsonArray();
					for(int k=0; k<8; k++){
						if(sheet.getRow(k) == null) {
							if(k==0) { break; }
							cellArray.add("");
							continue;
						}
						Cell cell = sheet.getRow(k).getCell(j);
						if(cell==null) { cellArray.add(""); continue; }
						Object obj = getCellValue(cell, feval);
						if(obj == null) { cellArray.add(""); continue; }
						String cellString = obj.toString();
						if(cellString == null) { cellArray.add(""); continue; }
						if(cellString.length()==0) { cellArray.add(""); continue; }
						cellArray.add(cellString);
					}
					if(cellArray.size()==0) { break; }
					if(cellArray.get(0).asString().length()==0) { break; }//先頭行が空なら対象外
					valuesJson.add(cellArray);
				}
				sheetJson.add("values", valuesJson);
				//System.out.println(valuesJson.toString());


				valueRanges.add(sheetJson);
			}
			System.out.println(sb.toString());

			ans = bookJson.toString();

		} catch (EncryptedDocumentException | InvalidFormatException e) {
			e.printStackTrace();
		}


		return ans;
	}

    public static Object getCellValue(Cell cell, FormulaEvaluator feval) {
        Objects.requireNonNull(cell, "cell is null");

        CellType cellType = cell.getCellTypeEnum();
        if (cellType == CellType.BLANK) {
            return null;
        } else if (cellType == CellType.BOOLEAN) {
            return cell.getBooleanCellValue();
        } else if (cellType == CellType.ERROR) {
            return null;
        } else if (cellType == CellType.FORMULA) {
        	feval.evaluateInCell(cell);
            return getCellValue(cell, feval);
        } else if (cellType == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue();
            } else {
                return ""+((int)cell.getNumericCellValue());
            }
        } else if (cellType == CellType.STRING) {
            return cell.getStringCellValue();
        } else {
            throw new RuntimeException("Unknow type cell");
        }
    }


	private String loadStringFromFile(String file) throws IOException{
		l.trace("in comming loadJsonFromFile");

		FileInputStream fr = new FileInputStream(file);
		InputStreamReader isr = new InputStreamReader(fr, "UTF-8");
		BufferedReader br = new BufferedReader(isr);
		StringBuffer sb = new StringBuffer();
		while(true){
			String line = br.readLine();
			l.trace(line);
			if(line==null) { break; }
			sb.append(line);
		}
		br.close();
		isr.close();
		fr.close();
		String jsonTxt = sb.toString();

		return jsonTxt;
	}

}
