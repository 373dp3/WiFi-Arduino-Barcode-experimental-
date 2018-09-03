package jp.dp3.kota.sheets;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.net.httpserver.HttpServer;

public class BootStub {

	public static final ServerConfig c = new ServerConfig("server_config.json");
	public static final String DB_PATH = c.upload.dbpath;

	//trace,debug,info,warn,error,fatal
	static Logger l = LogManager.getLogger(BootStub.class);

	public static void main(String[] args) throws IOException {
		if(args!=null){
			boolean isHit = false;
			for(String v : args){
				if(v.compareTo("ftp")==0)
				{
					isHit = true;
					break;

				}
			}
			if(isHit){
				System.out.println("FTP mode");
				doFtp();
				return;

			}
		}

		l.info("Start main");
		l.debug("dbpath: " + DB_PATH);

		//LocalHttpdでは設定をGoogleスプレッドシートからロードしない
		/*/
		if(c.loadSheetConfigOnBoot > 0){
			l.info("SheetWrapper loadSheetConfig:" + c.loadSheetId);
			SheetWrapper wrapper = new SheetWrapper();
			wrapper.loadSheetConfig(c.loadSheetId);//中央工機様
		}
		//*/

		httpd(args);

	}

	public static void doFtp() throws IOException{
		String[] putFiles = {"logs/dp3httpd.log","/tmp/upload_queue.db"};
		String[] putFilesName = {"dp3httpd.log","upload_queue.db"};
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_hhmm");
		String datePrefix = sdf.format(Calendar.getInstance().getTime());

		FileOutputStream ostream = null;

        // FTPClientの生成
        FTPClient ftpclient = new FTPClient();

        try {
            // サーバに接続
        	l.info("Connect");
            ftpclient.connect( "dp3-02.sakura.ne.jp" );
            int reply = ftpclient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                System.err.println("connect fail");
                System.exit(1);
            }

            // ログイン
        	l.info("login");
            if (ftpclient.login("dp3-02", "tm0831gonta9n") == false) {
                System.err.println("login fail");
                System.exit(2);
            }

            // バイナリモードに設定
        	l.info("Binmode");
            ftpclient.setFileType(FTP.BINARY_FILE_TYPE);

            // pasvモードに移行
        	l.info("PASV");
            ftpclient.pasv();
            printFtpReply(ftpclient);


            l.info("test");
            ftpclient.changeWorkingDirectory("/home/dp3-02/chu");
            l.info("pwd:" + ftpclient.printWorkingDirectory());
            for(int i=0; i<putFiles.length; i++){
            	String localPath = putFiles[i];
            	String upName = putFilesName[i];

                FileInputStream fis = new FileInputStream(localPath);
                ftpclient.storeFile(datePrefix + "_" + upName, fis);
                printFtpReply(ftpclient);
            }



            l.info("test done");

            if(false){
            	l.info("ListFiles");
            	for(FTPFile file : ftpclient.listFiles(ftpclient.printWorkingDirectory())){
            		l.info("i:" + file.toString());
            	}
            	l.info("done");
            }


            // ファイル受信
            //ostream = new FileOutputStream("localfile");
            //ftpclient.retrieveFile("remotefile", ostream);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        finally {

            if (ftpclient.isConnected()) ftpclient.disconnect();

            if (ostream != null) {
                try {
                    ostream.close();
                } catch(Exception e) {
                    e.printStackTrace();
                  }
              }
          }
	}
    private static void printFtpReply(FTPClient ftpClient) {

        System.out.print(ftpClient.getReplyString());

        int replyCode = ftpClient.getReplyCode();

        if (!FTPReply.isPositiveCompletion(replyCode)) {
            System.err.println("送信したFTPコマンドは失敗しました。");
        }
    }

	public static void httpd(String[] args) throws IOException{
		LocalHttpd httpd = new LocalHttpd();

		int port = (args.length >= 1)?Integer.parseInt(args[0]):80;

		HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

		server.createContext("/", httpd);
		server.start();
		l.info("Start httpd daemon");
	}

}
