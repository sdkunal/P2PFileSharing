public class NeighbourPeerNode {
	private int peerId = -1;
	private String hostName = "";
	private int portNumber = -1;
	private int haveFile = 0;
	private int[] bitField = null;

	private NeighbourPeerNode() {}

	private NeighbourPeerNode(int peerId, String hostName, int portNumber, int haveFile) {
		this.setPeerId(peerId);
		this.setHostName(hostName);
		this.setPortNumber(portNumber);
		this.setHaveFile(haveFile);
	}

	public static NeighbourPeerNode getPeerNodeObject(String row) {		
		String[] parameters = row.split(" ");
		int peerId = Integer.parseInt(parameters[0]);
		String hostName = parameters[1];
		int portNumber = Integer.parseInt(parameters[2]);
		int haveFile = Integer.parseInt(parameters[3]);
		NeighbourPeerNode pn = new NeighbourPeerNode(peerId,hostName,portNumber,haveFile);
		return pn;
	}

	public int getPeerId() {
		return peerId;
	}

	public void setPeerId(int peerId) {
		this.peerId = peerId;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public int getPortNumber() {
		return portNumber;
	}

	public void setPortNumber(int portNumber) {
		this.portNumber = portNumber;
	}

	public int getHaveFile() {
		return haveFile;
	}

	public void setHaveFile(int haveFile) {
		this.haveFile = haveFile;
	}

	public int[] getBitfield() {
		return bitField;
	}

	public void setBitfield(int[] bitfield) {
		this.bitField = bitfield;
	}

}
