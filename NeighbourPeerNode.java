
public class NeighbourPeerNode {
	private int peerId = -1;
	private String host = "";
	private int portNum = -1;
	private int haveFile = 0;
	private int[] bitField = null;

	private NeighbourPeerNode(int peerId, String host, int portNum, int haveFile) {
		this.setPeerId(peerId);
		this.setHostName(host);
		this.setPortNumber(portNum);
		this.setHaveFile(haveFile);
	}
	
	private NeighbourPeerNode() {}


	public static NeighbourPeerNode getPeerNodeObject(String row) {		
		String[] parameters = row.split(" ");
		int peerId = Integer.parseInt(parameters[0]);
		String host = parameters[1];
		int portNum = Integer.parseInt(parameters[2]);
		int haveFile = Integer.parseInt(parameters[3]);
		NeighbourPeerNode pn = new NeighbourPeerNode(peerId,host,portNum,haveFile);
		return pn;
	}

	public int getPeerId() {
		return peerId;
	}

	public void setPeerId(int peerId) {
		this.peerId = peerId;
	}
	
	public void setHaveFile(int haveFile) {
		this.haveFile = haveFile;
	}
	
	public int getHaveFile() {
		return haveFile;
	}
	
	public int[] getBitfield() {
		return bitField;
	}
	
	public String getHostName() {
		return host;
	}

	public void setHostName(String host) {
		this.host = host;
	}

	public int getPortNumber() {
		return portNum;
	}
	
	public void setPortNumber(int portNum) {
		this.portNum = portNum;
	}
	
	public void setBitfield(int[] bitfield) {
		this.bitField = bitfield;
	}

}
