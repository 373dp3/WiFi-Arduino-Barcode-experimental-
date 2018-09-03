package jp.dp3.kota.sheets;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

public class ServerConfig {
	public final Client client;
	public final Upload upload;
	public final int loadSheetConfigOnBoot;
	public final String loadSheetId;

	//trace,debug,info,warn,error,fatal
	Logger l = LogManager.getLogger(ServerConfig.class);

	public ServerConfig(String path){
		l.trace("path: " + path);
		JsonObject obj=null;
		try{
			FileReader fr = new FileReader(path);
			obj = Json.parse(fr).asObject();
			fr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			l.error("FileNotFoundException : " + e.toString());
		} catch (IOException e) {
			e.printStackTrace();
			l.error("IOException : " + e.toString());
		} catch(NullPointerException e){
			l.error("NullPointerException : " + e.toString());
		}
		//objがnullの場合、デフォルト値をロードするように
		if(obj==null){
			this.client = new Client(null);
			this.upload = new Upload(null);
			loadSheetConfigOnBoot = 0;
			loadSheetId = "";
			l.error("json obj null");
		}else{
			loadSheetConfigOnBoot = obj.getInt("loadSheetConfigOnBoot", 0);
			loadSheetId = obj.getString("loadSheetId", "");
			if(obj.get("client")==null){
				this.client = new Client(null);
				l.error("client obj null");
			}else{
				this.client = new Client(obj.get("client").asObject());
			}

			if(obj.get("upload")==null){
				this.upload = new Upload(null);
				l.error("upload obj null");
			}else{
				this.upload = new Upload(obj.get("upload").asObject());
			}

		}
	}

	public class Client{
		public final Beep beep;
		public Client(JsonObject object){
			if(object==null){
				this.beep = new Beep(null);
				l.error("client obj null");
			}else{
				this.beep = new Beep(object.get("beep").asObject());
			}
		}
		public class Beep{
			public final OnOkError onOk;
			public final OnOkError onError;
			public Beep(JsonObject object){
				if(object==null){
					l.error("beep obj null");
					this.onOk = new OnOkError(null);
					this.onError = new OnOkError(null);
				}else{
					this.onOk = new OnOkError(object.get("onOk").asObject());
					this.onError = new OnOkError(object.get("onError").asObject());
				}
			}
			public class OnOkError{
				public final int hz;
				public final int volume;
				public final JsonArray pattern;
				public OnOkError(JsonObject object){
					if(object==null){
						l.error("OnOkError obj null");
						this.volume = 256;
						this.hz = 3999;
						this.pattern = new JsonArray();
						return;
					}
					this.hz = object.getInt("hz", 3999);
					this.volume = object.getInt("volume", 256);
					List<Long> ary = new ArrayList<Long>();
					if(object.get("pattern")==null){
						l.error("pattern obj null");
						this.pattern = new JsonArray();
						return;
					}
					pattern = object.get("pattern").asArray();
				}
			}

		}
	}
	public class Upload
	{
		public final long interval_sec;
		public final String dbpath;
		public Upload(JsonObject object){
			if(object==null){
				l.error("upload obj null");
				this.interval_sec = 30;
				this.dbpath = "upload_queue_nospec.db";
				return;
			}
			this.interval_sec = object.getLong("interval_sec", 30);
			this.dbpath = object.getString("dbpath", "upload_queue_nospec.db");
		}
	}


}
