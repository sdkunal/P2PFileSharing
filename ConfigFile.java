import java.util.ArrayList;
import java.util.List;

public class ConfigFile {
	private int noOfNeighbors = 0;
	private int unChokingInterval = 0;
	private int optUnChokingInterval = 0;
	private String fileName = "";
	private int fileSize = 0;
	private int chunkSize = 0;
	private int noOfChunks = 0;
	
	private ConfigFile() {}
	
	private ConfigFile(int noOfNeighbors,int unChokingInterval,int optUnChokingInterval,String fileName,int fileSize,int chunkSize) {
		
		this.setNoOfNeighbors(noOfNeighbors);
		this.setUnChokingInterval(unChokingInterval);
		this.setOptUnChokingInterval(optUnChokingInterval);
		this.setFileName(fileName);
		this.setFileSize(fileSize);
		this.setChunkSize(chunkSize);
	}
	
	public static ConfigFile getConfigFileObject(List<String> rows) {
		
		if(rows != null && rows.size() == 6) {
			int noOfNeighbors = Integer.parseInt(rows.get(0).split(" ")[1]);
			int unChokingInterval = Integer.parseInt(rows.get(1).split(" ")[1]);
			int optUnChokingInterval = Integer.parseInt(rows.get(2).split(" ")[1]);
			String fileName = rows.get(3).split(" ")[1];
			int fileSize = Integer.parseInt(rows.get(4).split(" ")[1]);
			int chunkSize = Integer.parseInt(rows.get(5).split(" ")[1]);
			
			ConfigFile configFileObj = new ConfigFile(noOfNeighbors,unChokingInterval,optUnChokingInterval,fileName,fileSize,chunkSize);
			double fileSizeDoub = (double) fileSize;
			double chunkSizeDoub = (double) chunkSize;
			int noOfChunks = (int)Math.ceil(fileSizeDoub/chunkSizeDoub);
			configFileObj.setNoOfChunks(noOfChunks);
		//	System.out.println(noOfChunks);
			return configFileObj;
		}
		return null;
	}
	
	public int getNoOfNeighbors() {
		return noOfNeighbors;
	}
	
	public void setNoOfNeighbors(int noOfNeighbors) {
		this.noOfNeighbors = noOfNeighbors;
	}
	
	public int getUnChokingInterval() {
		return unChokingInterval;
	}
	
	public void setUnChokingInterval(int unChokingInterval) {
		this.unChokingInterval = unChokingInterval;
	}
	
	public int getOptUnChokingInterval() {
		return optUnChokingInterval;
	}
	
	public void setOptUnChokingInterval(int optUnChokingInterval) {
		this.optUnChokingInterval = optUnChokingInterval;
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public int getFileSize() {
		return fileSize;
	}
	
	public void setFileSize(int fileSize) {
		this.fileSize = fileSize;
	}
	
	public int getChunkSize() {
		return chunkSize;
	}
	
	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}
	
	public int getNoOfChunks() {
		return noOfChunks;
	}
	
	public void setNoOfChunks(int noOfChunks) {
		this.noOfChunks = noOfChunks;
	}
  
}