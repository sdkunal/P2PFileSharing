import java.util.ArrayList;
import java.util.List;

//package com.peer.config;
public class CommonConfig
{
    private int unchokingInterval;
    private int numPreferredNeighbours;
    private String fileName;
    private int optimisticUnchokingInterval;
    private int fileSize;
    private int pieceSize = 0;
	private int noOfChunks = 0;
    
    public CommonConfig()
    {
    }

    public CommonConfig(int numPreferredNeighbours,
                        int unchokingInterval,
    					int optimisticUnchokingInterval,
                        String fileName,
    					int fileSize,
    					int pieceSize)
    {
    	this.setNumPreferredNeighbours(numPreferredNeighbours);
        this.setUnchokingInterval(unchokingInterval);
        this.setOptimisticUnchokingInterval(optimisticUnchokingInterval);
        this.setFileName(fileName);
        this.setFileSize(fileSize);
        this.setPieceSize(pieceSize);        
    }

    public static CommonConfig getCommonConfigFileObj(List<String> rows) {
		
		if(rows != null && rows.size() == 6) {
			int numPreferredNeighbours = Integer.parseInt(rows.get(0).split(" ")[1]);
			int unchokingInterval = Integer.parseInt(rows.get(1).split(" ")[1]);
			int optimisticUnchokingInterval = Integer.parseInt(rows.get(2).split(" ")[1]);
			String fileName = rows.get(3).split(" ")[1];
			int fileSize = Integer.parseInt(rows.get(4).split(" ")[1]);
			int pieceSize = Integer.parseInt(rows.get(5).split(" ")[1]);
			
			CommonConfig commonConfigFileObj = new CommonConfig(numPreferredNeighbours,unchokingInterval,optimisticUnchokingInterval,fileName,fileSize,pieceSize);
			double fileSizeDoub = (double) fileSize;
			double chunkSizeDoub = (double) pieceSize;
			int noOfChunks = (int)Math.ceil(fileSizeDoub/chunkSizeDoub);
			commonConfigFileObj.setNoOfChunks(noOfChunks);
		//	System.out.println(noOfChunks);
			return commonConfigFileObj;
		}
		return null;
	}

    public int getNumPreferredNeighbours()
    {
        return numPreferredNeighbours;
    }
 
    public int getunchokingInterval()
    {
        return unchokingInterval;
    }
    
    public int getOptimisticUnchokingInterval()
    {
        return optimisticUnchokingInterval;
    }

    public String getFileName()
    {
        return fileName;
    }

    public int getFileSize()
    {
        return fileSize;
    }
   
   public int getPieceSize()
    {
        return pieceSize;
    } 

    public void setNumPreferredNeighbours(int numPreferredNeighbours)
    {
        this.numPreferredNeighbours = numPreferredNeighbours;
    }
    
    public void setUnchokingInterval(int unchokingInterval)
    {
        this.unchokingInterval = unchokingInterval;
    }

    public void setOptimisticUnchokingInterval(int optimisticUnchokingInterval)
    {
        this.optimisticUnchokingInterval = optimisticUnchokingInterval;
    }
    
    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }
   
    public void setFileSize(int fileSize)
    {
        this.fileSize = fileSize;
    }
   
    public void setPieceSize(int pieceSize)
    {
        this.pieceSize = pieceSize;
    }
    
    public int getNoOfChunks() {
		return noOfChunks;
	}
	
	public void setNoOfChunks(int noOfChunks) {
		this.noOfChunks = noOfChunks;
	}
}