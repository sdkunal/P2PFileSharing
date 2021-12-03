import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

//Write all the common helper functions here
public class PeerCommonUtil {

	PeerCommonUtil(){}

	//Handshake between packets
	public synchronized byte[] getHandshakePacket(int inputPeerId) 
	{
		String handShakeHeader = PeerConstants.HANDSHAKE_HEADER;
		byte[] hsHeaderBytes = handShakeHeader.getBytes();
		String constZeroes = PeerConstants.ZERO_BITS_HANDSHAKE;
		byte[] zeroBytes = constZeroes.getBytes();
		byte[] peerIdBytes = String.valueOf(inputPeerId).getBytes();
		int packetLength = hsHeaderBytes.length+zeroBytes.length+peerIdBytes.length;
		byte[] hspacket = new byte[packetLength];
		for(int iter = 0 ;iter <packetLength; iter++) {
			if(iter < hsHeaderBytes.length) {
				hspacket[iter] = hsHeaderBytes[iter];
			}
			else if(iter < hsHeaderBytes.length + zeroBytes.length){
				hspacket[iter] = zeroBytes[iter - hsHeaderBytes.length];
			}
			else {
				hspacket[iter] = peerIdBytes[iter - hsHeaderBytes.length - zeroBytes.length];
			}
		}
		return hspacket; 
	}

	//write a piece that is received in the directory
	public synchronized void writeReceivedPiece(int peerId,int pieceIndex,byte[] piece) throws IOException{
		String fileName = PeerConstants.DOWNLOAD_FILE;
		fileName = System.getProperty("user.dir")+File.separator+"peer_"+peerId+File.separator+fileName+"_" + pieceIndex;
		File newFile = new File(fileName);
		FileOutputStream outputStream = new FileOutputStream(newFile);
		outputStream.write(piece);
		outputStream.close();
	}

	//Combining chunks of different downloaded files
	public synchronized void combinePiecesintoFile(int peerId,CommonConfig commonConfigFileObj) throws IOException{
		int noOfChunks = commonConfigFileObj.getNoOfChunks();
		String fileName = PeerConstants.DOWNLOAD_FILE;
		File mergedFile = new File(System.getProperty("user.dir")+File.separator+"peer_"+peerId+File.separator+fileName);
		FileOutputStream outputStream = new FileOutputStream(mergedFile);
		File[] separateFiles = new File[noOfChunks];

		for(int i=0;i<noOfChunks;i++) {
			separateFiles[i] = new File(System.getProperty("user.dir")+File.separator+"peer_"+peerId+File.separator+fileName+"_"+i);
		}

		for(int iter = 0;iter <noOfChunks; iter++) {
			FileInputStream inputStream = new FileInputStream(separateFiles[iter]);
			int totalLengthofChunk = (int)separateFiles[iter].length();
			byte[] readChunkFile = new byte[totalLengthofChunk];
			inputStream.read(readChunkFile);
			outputStream.write(readChunkFile);
			inputStream.close();
		}
		outputStream.close();
	}

	//split the whole file into chunks and write into the peer directory as chunks
	public void divideFileintoPieces(String peerId, CommonConfig commonConfigFileObj) {
		int pieceSize = commonConfigFileObj.getPieceSize();
		int fileSize = commonConfigFileObj.getFileSize();
		int noOfChunks = commonConfigFileObj.getNoOfChunks();
		String inputFile = System.getProperty("user.dir")+File.separator+PeerConstants.DOWNLOAD_FILE;

		try {		
			int iter = 0;
			int piecestobeCopied = 0;
			FileInputStream inputStream = new FileInputStream(inputFile);
			File[] chunkFiles = new File[pieceSize];
			while(iter < noOfChunks) {
				piecestobeCopied = piecestobeCopied + pieceSize;
				int pieceLength = pieceSize;
				//Last chunk will have to be fileSize - sizeofchunksCopiedtillnow as it will be of different size and not the same size as chunk size.
				if(fileSize < piecestobeCopied) {
					piecestobeCopied = piecestobeCopied - pieceSize;
					pieceLength = fileSize - piecestobeCopied;			
				}
				byte[] copy = new byte[pieceLength];
				String fileName = PeerConstants.DOWNLOAD_FILE;
				fileName = System.getProperty("user.dir")+File.separator+"peer_"+peerId+File.separator+fileName+"_"+iter;
				chunkFiles[iter] = new File(fileName);
				FileOutputStream fos = new FileOutputStream(fileName);
				inputStream.read(copy);
				fos.write(copy);
				fos.close();
				//	System.out.println(chunkLength);
				iter++;
			}
			inputStream.close();
		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

	//Create peer directory if not created
	public File directoryForLogandPeer(int peerId) {
		File fileLog = null;
		try {
			File dirPeer = new File(System.getProperty("user.dir")+File.separator+"peer_"+peerId);
			if(!dirPeer.exists()) {
				dirPeer.mkdir();
			}
			fileLog = new File(System.getProperty("user.dir")+File.separator+"log_peer_"+peerId+".log");
			boolean creationFileLog = fileLog.createNewFile();
			if(creationFileLog) {
				System.out.println("logfile created");
			}

		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return fileLog;
	}

	//return a piece requested by other peers
	public synchronized byte[] returnRequestPiece(int peerId,int pieceIndex) throws IOException {

		String fileName = PeerConstants.DOWNLOAD_FILE;
		fileName = System.getProperty("user.dir")+File.separator+"peer_"+peerId+File.separator+fileName+"_" + pieceIndex;
		File pieceFile = new File(fileName);
		FileInputStream inputStream = new FileInputStream(pieceFile);
		int length = (int)pieceFile.length();
		byte[] piece = new byte[length];
		inputStream.read(piece);
		inputStream.close();
		return piece;
	}
}
	
	