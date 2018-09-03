package jp.dp3.kota.sheets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

public class ColumnScreen {
	public Freq freq = Freq.NOOP;
	public Method method = Method.NOOP;
	public String L1 = "";
	public String L2 = "";
	public String Name = "";
	public enum Freq {
		NOOP, ZERO, ONCE, N
	}
	public enum Method {
		NOOP, DATETIME, MAC, DELTA_MS, ANY, BARCOCE, NUMPAD, TRIPTIME
	}
	public int jsonArrayIndex = -1;
	public boolean isLastScreen = false;

	//trace,debug,info,warn,error,fatal
	//import org.apache.logging.log4j.LogManager;
	//import org.apache.logging.log4j.Logger;
	Logger l = LogManager.getLogger(ColumnScreen.class);

	public ColumnScreen(String L1, String L2, String Name, Freq freq){
		this.L1 = L1;
		this.L2 = L2;
		this.Name = Name;
		this.freq = freq;
	}

	public ColumnScreen(JsonArray itemList, JsonArray defList){
		for(int i=0; i<defList.size(); i++){
			if(defList.get(i).asString().compareTo("Name")==0){
				this.Name = itemList.get(i).asString();
			}
			if(defList.get(i).asString().compareTo("LCD1")==0){
				this.L1 = itemList.get(i).asString();
			}
			if(defList.get(i).asString().compareTo("LCD2")==0){
				this.L2 = itemList.get(i).asString();
			}
			if(defList.get(i).asString().compareTo("InputFreq")==0){
				String tmpFreq = itemList.get(i).asString();
				if(tmpFreq.compareTo("0")==0){
					this.freq = Freq.ZERO;
				}
				if(tmpFreq.compareTo("1")==0){
					this.freq = Freq.ONCE;
				}
				if(tmpFreq.compareTo("N")==0){
					this.freq = Freq.N;
				}
			}
			if(defList.get(i).asString().compareTo("InputType")==0){
				String tmpFreq = itemList.get(i).asString();
				if(tmpFreq.compareTo("DateTime")==0){
					this.method = Method.DATETIME;
				}
				if(tmpFreq.compareTo("Mac")==0){
					this.method = Method.MAC;
				}
				if(tmpFreq.compareTo("DeltaTimeMs")==0){
					this.method = Method.DELTA_MS;
				}
				if(tmpFreq.compareTo("TripTimeMs")==0){
					this.method = Method.TRIPTIME;
				}
				if(tmpFreq.compareTo("Any")==0){
					this.method = Method.ANY;
				}
				if(tmpFreq.compareTo("Barcode")==0){
					this.method = Method.BARCOCE;
				}
				if(tmpFreq.compareTo("Numpad")==0){
					this.method = Method.NUMPAD;
				}
			}
		}
		l.trace(this.toJson());
	}

	public String toJson(JsonObject json){
		json.add("name", this.Name);
		json.add("L1", this.L1);
		json.add("L2", this.L2);

		return json.toString();
	}

	public String toJson(){
		JsonObject json = new JsonObject();
		return toJson(json);
	}
}
