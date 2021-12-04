import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

//Write all the common helper functions here
public class PeerCommonUtil {

	PeerCommonUtil(){}

	//Create peer directory if not created
	public File makePeerAndLogDirectory(int peerId) {
		File logFile = null;
		try {
			File peerDir = new File(System.getProperty("user.dir")+File.separator+"peer_"+peerId);
			if(!peerDir.exists()) {
				peerDir.mkdir();
			}
			logFile = new File(System.getProperty("user.dir")+File.separator+"log_peer_"+peerId+".log");
			boolean logFileCreated = logFile.createNewFile();
			if(logFileCreated) {
				System.out.println("logfile created");
			}

		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return logFile;
	}

	//split the whole file into chunks and write into the peer directory as chunks
	public void splitFileintoChunks(String peerId, CommonConfig configFileObj) {
		int noOfChunks = configFileObj.getNoOfChunks();
		int chunkSize = configFileObj.getPieceSize();
		int fileSize = configFileObj.getFileSize();
		String sourceFile = System.getProperty("user.dir")+File.separator+PeerConstants.DOWNLOAD_FILE;

		try {		
			FileInputStream fis = new FileInputStream(sourceFile);
			File[] chunkFiles = new File[chunkSize];
			int i = 0,chunkstobeCopied = 0;
			while(i < noOfChunks) {
				chunkstobeCopied = chunkstobeCopied + chunkSize;
				int chunkLength = chunkSize;
				//Last chunk will have to be fileSize - sizeofchunksCopiedtillnow as it will be of different size and not the same size as chunk size.
				if(fileSize < chunkstobeCopied) {
					chunkstobeCopied = chunkstobeCopied - chunkSize;
					chunkLength = fileSize - chunkstobeCopied;			
				}
				byte[] copy = new byte[chunkLength];
				String fileName = PeerConstants.DOWNLOAD_FILE;
				fileName = System.getProperty("user.dir")+File.separator+"peer_"+peerId+File.separator+fileName+"_"+i;
				chunkFiles[i] = new File(fileName);
				FileOutputStream fos = new FileOutputStream(fileName);
				fis.read(copy);
				fos.write(copy);
				fos.close();
				//	System.out.println(chunkLength);
				i++;
			}
			fis.close();
		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

	public synchronized byte[] getHandshakePacket(int sourcePeerId) 
	{
		String hsHeader = PeerConstants.HANDSHAKE_HEADER;
		byte[] headerBytes = hsHeader.getBytes();
		String zeroes = PeerConstants.ZERO_BITS_HANDSHAKE;
		byte[] zeroBytes = zeroes.getBytes();
		byte[] peerIdBytes = String.valueOf(sourcePeerId).getBytes();
		int packetLen = headerBytes.length+zeroBytes.length+peerIdBytes.length;
		byte[] hspacket = new byte[packetLen];
		for(int i = 0 ;i<packetLen;i++) {
			if(i < headerBytes.length) {
				hspacket[i] = headerBytes[i];
			}
			else if(i < headerBytes.length + zeroBytes.length){
				hspacket[i] = zeroBytes[i-headerBytes.length];
			}
			else {
				hspacket[i] = peerIdBytes[i - headerBytes.length - zeroBytes.length];
			}
		}

		return hspacket; 

	}

	//return a piece requested by other peers
	public synchronized byte[] returnPiece(int peerId,int chunkIndex) throws IOException {

		String fileName = PeerConstants.DOWNLOAD_FILE;
		fileName = System.getProperty("user.dir")+File.separator+"peer_"+peerId+File.separator+fileName+"_"+chunkIndex;
		File chunkFile = new File(fileName);
		FileInputStream fis = new FileInputStream(chunkFile);
		int length = (int)chunkFile.length();
		byte[] chunk = new byte[length];
		fis.read(chunk);
		fis.close();
		return chunk;
	}

	//write a piece that u received to the peer directory
	public synchronized void writePiece(int peerId,int chunkIndex,byte[] piece) throws IOException{
		String fileName = PeerConstants.DOWNLOAD_FILE;
		fileName = System.getProperty("user.dir")+File.separator+"peer_"+peerId+File.separator+fileName+"_"+chunkIndex;
		File file = new File(fileName);
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(piece);
		fos.close();
	}

	//join the whole file from chunks of pieces once all the pieces have been downloaded
	public synchronized void joinChunksintoFile(int peerId,CommonConfig configFileObj) throws IOException{
		int noOfChunks = configFileObj.getNoOfChunks();
		String fileName = PeerConstants.DOWNLOAD_FILE;
		File combinedFile = new File(System.getProperty("user.dir")+File.separator+"peer_"+peerId+File.separator+fileName);
		FileOutputStream fos = new FileOutputStream(combinedFile);
		File[] splitFiles = new File[noOfChunks];

		for(int i=0;i<noOfChunks;i++) {
			splitFiles[i] = new File(System.getProperty("user.dir")+File.separator+"peer_"+peerId+File.separator+fileName+"_"+i);
		}

		for(int i=0;i<noOfChunks;i++) {
			FileInputStream fis = new FileInputStream(splitFiles[i]);
			int lengthOfChunkFile = (int)splitFiles[i].length();
			byte[] readChunkFile = new byte[lengthOfChunkFile];
			fis.read(readChunkFile);
			fos.write(readChunkFile);
			fis.close();
		}
		fos.close();
	}
}
