package jp.dp3.kota.sheets;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchGetValuesResponse;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;

public class SheetWrapper {
	//trace,debug,info,warn,error,fatal
	static Logger l = LogManager.getLogger(SheetWrapper.class);

	/** Application name. */
    private static final String APPLICATION_NAME =
        "Google Sheets API Java Quickstart";

    /** Directory to store user credentials for this application. */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(
        System.getProperty("user.home"), ".credentials/sheets.googleapis.com-java-quickstart");

    /** Global instance of the {@link FileDataStoreFactory}. */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY =
        JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;

    /** Global instance of the scopes required by this quickstart.
     *
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials/sheets.googleapis.com-java-quickstart
     */
    private static final List<String> SCOPES =
        Arrays.asList(SheetsScopes.SPREADSHEETS);

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            l.error(t.toString() + " // " + t.getMessage());
            System.exit(1);
        }
    }

    /**
     * Creates an authorized Credential object.
     * @return an authorized Credential object.
     * @throws IOException
     */
    public static Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in =
        		SheetWrapper.class.getResourceAsStream("/client_secret.json");
        GoogleClientSecrets clientSecrets =
            GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(DATA_STORE_FACTORY)
                .setAccessType("offline")
                .build();
        Credential credential = new AuthorizationCodeInstalledApp(
            flow, new LocalServerReceiver()).authorize("user");
        l.info("Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }

    /**
     * Build and return an authorized Sheets API client service.
     * @return an authorized Sheets API client service
     * @throws IOException
     */
    public static Sheets getSheetsService() throws IOException {
        Credential credential = authorize();
        return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }


    public void loadSheetConfig(String sheetId) throws IOException{
    	l.trace("loadSheetConfig");

        Sheets service = getSheetsService();
        List<String> ranges = new ArrayList<String>();

        // True if grid data should be returned.
        // This parameter is ignored if a field mask was set in the request.
        boolean includeGridData = false;

        Sheets.Spreadsheets.Get request = service.spreadsheets().get(sheetId);
        request.setRanges(ranges);
        request.setIncludeGridData(includeGridData);

        Spreadsheet response = request.execute();
        //System.out.println(response);

        List<String> rangeList = new ArrayList<String>();
        for(Sheet sh : response.getSheets())
        {
            if(sh.getProperties().getSheetType().compareTo("GRID")!=0) { continue; }
            System.out.println(sh.getProperties().getTitle() + "("+ sh.getProperties().getSheetId()+")");
            rangeList.add(sh.getProperties().getTitle()+"!A1:Z10");
        }

        // --------------------------------------------------------------
        // シート毎の設定情報相当部分を一括取得

        String valueRenderOption = "0";//FORMATTEDVALUE
        String dateTimeRenderOption = "1";//FORMATTEDSTRING

        Sheets.Spreadsheets.Values.BatchGet request2 =
                service.spreadsheets().values().batchGet(sheetId);
        request2.setRanges(rangeList);
        request2.setValueRenderOption(valueRenderOption);
        request2.setDateTimeRenderOption(dateTimeRenderOption);
        request2.setMajorDimension("COLUMNS");

        BatchGetValuesResponse response2 = request2.execute();

        System.out.println(response2);
        FileWriter fw = new FileWriter("sheet_config.json");
        fw.write(response2.toPrettyString());
        fw.close();

    }

    public void uploadData(String sheetId, String sheetName, List<List<Object>> list) throws IOException{
    	if(list==null) { return; }
    	if(list.size()==0) { return; }
    	l.trace("uploadData : " + sheetId + " " + sheetName + " size:" + list.size());

        Sheets service = getSheetsService();

        ValueRange valueRange = new ValueRange();
        valueRange.setValues(list);

        try{
            service.spreadsheets().values()
        	.append(sheetId, sheetName+"!A1:Z1", valueRange)
        	.setValueInputOption("RAW")
        	.execute();
        }catch(IOException e){
        	l.error("uploadData IOException" + e.toString() + " // " + e.getMessage());
        	throw e;
        }

    }


}
