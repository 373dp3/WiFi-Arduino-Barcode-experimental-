package jp.dp3.kota.sheets;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;



public class ClientToLocalServer {
	public String barcode = "";
	public String numpad = "";

	//trace,debug,info,warn,error,fatal
	Logger l = LogManager.getLogger(ClientToLocalServer.class);

	//起動時にLanServerへ問い合わせたときの応答時間UnixtTimeSec
	public long bootTimeSec = 0;

	//列識別用のName
	public String name = "";
	public String mac = "";
	public String uri = "";

	//制御モード
	public String mode = "";

	JsonObject obj = null;

	public ClientToLocalServer(){}

	public ClientToLocalServer(InputStream stream) throws IOException{

		if(stream.available()>300*1024){
			l.error("クライアントからのパラメータが容量上限を超過しました");
			throw new IOException("Input size over");
		}

		//JsonObject obj = null;
		InputStreamReader isr = new InputStreamReader(stream);

		try{
			obj = Json.parse(isr).asObject();
		}catch(ParseException pe){
			//System.err.println("ParseException at ClientToLocalServer");
			l.error("JSONパースエラー:" + pe.toString() + " // " + pe.getMessage());
		}
		isr.close();
		if(obj == null) { return; }

		this.barcode = parse("bc");
		this.numpad = parse("np");
		this.mode =parse("mode");
		String btStr = parse("bt");
		if(btStr.length()>0){
			this.bootTimeSec = Long.parseLong(btStr);
		}
		this.name = parse("name");
		this.mac = parse("mac");
		l.trace(toString());
	}

	public String parse(String key){
		if(obj == null) { return ""; }
		if(obj.get(key)!=null){
			return obj.get(key).asString();
		}
		return "";
	}
	public long parseLong(String key){
		if(obj == null) { return -1; }
		if(obj.get(key)!=null){
			return obj.get(key).asLong();
		}
		return -1;
	}

	@Override
	public String toString(){
		return "name: " + name + " barcode: " + this.barcode
				+ " num: "+ this.numpad
				+ " mac: " + this.mac
				+ " uri: " + this.uri
				+ " bt: " + this.bootTimeSec;
	}

}
