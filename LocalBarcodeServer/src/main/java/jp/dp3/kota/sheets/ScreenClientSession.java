package jp.dp3.kota.sheets;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ScreenClientSession {
	public String mac = "";
	public long bootTimeSec = 0;
	public String uri = "";
	public List<String> cellList = null;
	public long systemTimeMs = -1;
	public long n1stTimeMs = -1;

	//trace,debug,info,warn,error,fatal
	Logger l = LogManager.getLogger(ScreenClientSession.class);

	public ScreenClientSession(ClientToLocalServer client){
		if(client !=null) {
			l.trace("cliet client session :: client:" +client.toString() );
		}else{
			l.trace("cliet client session :: client: null");
		}
		clearSession(client);
	}

	public void updateN1stTimeMs(){
		if(n1stTimeMs != -1) { return; }
		n1stTimeMs = System.currentTimeMillis();
	}
	public void clearN1stTimeMs(){
		n1stTimeMs = -1;
	}

	public void clearSession(ClientToLocalServer client) {
		if(client == null) { return; }
		this.mac = client.mac;
		this.bootTimeSec = client.bootTimeSec;
		this.uri = client.uri;
		n1stTimeMs = -1;

		cellList = new ArrayList<String>();
	}

	public void prepareCellListIfNeed(List<ColumnScreen> colList){
		if(colList.size()==cellList.size()) { return; }
		cellList.clear();

		//一致していないでの同数確保する。
		for(int i=0; i<colList.size(); i++){
			cellList.add("");
		}
	}

	public enum CheckResult {
		NOOP, ALL_OK, MAC_MATCH_DIFF_SESSION, MAC_MATCH_DIFF_URI, NOMATCH
	}

	public CheckResult check(ClientToLocalServer client){
		if(mac.compareTo(client.mac)!=0) { return CheckResult.NOMATCH; }
		if(bootTimeSec != client.bootTimeSec) { return CheckResult.MAC_MATCH_DIFF_SESSION; }
		if(uri.compareTo(client.uri)!=0) { return CheckResult.MAC_MATCH_DIFF_URI; }

		return CheckResult.ALL_OK;
	}
}
