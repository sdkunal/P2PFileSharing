import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class peerProcess {
	private static ReadInput rdIpObj = null;
	private static CommonConfig commonConfigObj = null;
	private static Map<Integer,NeighbourPeerNode> peerNeighbors = new LinkedHashMap<>();
	private static int sourcePeerId = -1;
	private static ConcurrentHashMap<Integer,Integer> mapBitfield = new ConcurrentHashMap<>();
	private static ConcurrentHashMap < Integer, Integer > peerdownloadRate = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Integer,NeighborPeerInteraction> peerBondsNeighbors = new ConcurrentHashMap<>();
	private static CopyOnWriteArrayList<Integer> interestedPeersList = new CopyOnWriteArrayList <> ();
	private static CopyOnWriteArrayList<Integer> unchokedPeersList = new CopyOnWriteArrayList <> ();
	private static CopyOnWriteArrayList<Integer> completedPeersList = new CopyOnWriteArrayList <> ();
	private static int currentPeerIndex = -1;
	private static int noOfPeers = -1;
	private static ServerSocket listener = null;
	private static AtomicInteger peersHavingWholeFile = new AtomicInteger(0);
	private static int totalPieces = 0; 
	private static int inputPort = 0;
	private static AtomicInteger optimisticallyUnchokedPeer = new AtomicInteger(-1);
	private static boolean wholeFile;
	private static PeerCommonUtil peerCommonUtilObj = null;
	private static Logs logsObj = null;

	class NeighborPeerInteraction {
		int peerId = -1;
		Socket socket = null;
		NeighbourPeerNode peerNode = null;
		DataInputStream inputStream = null;
		DataOutputStream outputStream = null;
		boolean unchoked = false;
		byte[] message = null;

		public NeighborPeerInteraction(Socket socket, NeighbourPeerNode peerNode) throws IOException {
			this.socket = socket;
			this.peerNode = peerNode;
			peerId = peerNode.getPeerId();
			message = new byte[5];
			inputStream = new DataInputStream(socket.getInputStream());
			outputStream = new DataOutputStream(socket.getOutputStream());				
		}

		//create a new thread and start interaction 
		public void beginCommunication() throws Exception {
			NeighborPeerInteractionThread nbit = new NeighborPeerInteractionThread();
			Thread neighborPeerThread = new Thread(nbit,"Thread_"+peerNode.getPeerId());
			neighborPeerThread.start();
			System.out.println(neighborPeerThread.getName() +" started");
		}

		//convert message into bytes that can be sent to other peers
		public synchronized byte[] receiveMessageInfo(int type,byte[] payload) {
			//message length byte array 
			int payLoadSize = payload != null?payload.length:0;
			int lengthSize = 1 + payLoadSize;	
			ByteBuffer bufferBytes = ByteBuffer.allocate(4); //allocate 4 bytes to byte buffer
			byte[] messageSize = bufferBytes.putInt(lengthSize).array();//write the integer value 'lengthSize' as 4 bytes

			//message type byte
			byte messageType = (byte)(char)type;

			//concatenate message length byte array, message type byte and payload byte array
			byte[] message = new byte[messageSize.length + lengthSize];
			int index = 0;
			for(int i = 0;i<messageSize.length;i++) {
				message[index] = messageSize[i];
				index++;
			}
			message[index] = messageType;
			index++;
			if(payload != null) {
				for(int i=0;i<payload.length;i++) {
					message[index] = payload[i];
					index++;
				}
			}
			return message;
		}

		//convert from int array to byte array
		public synchronized byte[] convertIntToByteArray(int[] data) {
			ByteBuffer byteBuffer = ByteBuffer.allocate(data.length * 4);        
			IntBuffer intBuffer = byteBuffer.asIntBuffer();
			intBuffer.put(data);
			byte[] byteArray = byteBuffer.array();
			return byteArray;
		}

		//convert byte array to int array
		public synchronized int[] convertByteToIntArray(byte[] bytes) {
			int[] message = new int[bytes.length/4];
			int index = 0;
			for(int i=0;i<bytes.length;i = i + 4) {
				byte[] eachbit = new byte[4];
				System.arraycopy(bytes, i, eachbit, 0, 4);
				message[index] = ByteBuffer.wrap(eachbit).getInt();
				index++;    	
			}
			return message;
		}

		//send bitfield messages
		public synchronized void sendBitFieldMessages(){
			int[] bitfield = new int[totalPieces];
			for(int i=0;i<mapBitfield.size();i++) {
				bitfield[i] = mapBitfield.get(i);
			}
			byte[] payload = convertIntToByteArray(bitfield);
			byte[] message = receiveMessageInfo(PeerConstants.message.BITFIELD.getValue(),payload);
			try {
				outputStream.write(message);
				outputStream.flush();
				logsObj.logSentbitfield(sourcePeerId, peerId);
			}
			catch(IOException ie) {
				ie.printStackTrace();
			}			
		}

		public synchronized void sendWholeMessage() {
			byte[] message = receiveMessageInfo(PeerConstants.message.COMPLETE.getValue(),null);
			try {
				outputStream.write(message);
				outputStream.flush();
			}
			catch(IOException ie) {
				System.exit(0);
				//ie.printStackTrace();
			}
		}

		//Don't send choke message if the peer is optimistically unchoked but remove it from unchokedPeersList and set unchoked = false
		public synchronized void sendChokeMessage() {
			if(optimisticallyUnchokedPeer.get() != peerId) {
				byte[] message = receiveMessageInfo(PeerConstants.message.CHOKE.getValue(),null);
				try {
					outputStream.write(message);
					outputStream.flush();
				}catch(IOException e) {
					System.exit(0);
					e.printStackTrace();				
				}
			}
			if(unchoked) {
				unchoked = false;
				int index = unchokedPeersList.indexOf(peerId);
				if(index != -1) {
					unchokedPeersList.remove(index);
				}	
			}
		}

		public synchronized void sendUnChokeMessage(boolean isOptimistically) {
			byte[] message = receiveMessageInfo(PeerConstants.message.UNCHOKE.getValue(),null);
			try {
				outputStream.write(message);
				outputStream.flush();
			}catch(IOException e) {
				System.exit(0);
				e.printStackTrace();			
			}

			//Set unchoked = true only if it is not optimistically unchoked neighbour
			if(!isOptimistically) {
				unchoked = true;
				unchokedPeersList.addIfAbsent(peerId);
			}				
		}

		//check if peer has any interesting pieces that I don't have
		public synchronized boolean checkIfPeerHasInterestingPieces() {
			boolean interested = false;
			int[] peer_bitfield = peerNode.getBitfield();
			for(int i =0;i < commonConfigObj.getNoOfChunks();i++) {
				if(mapBitfield.get(i) == 0 && peer_bitfield[i] == 1) {
					interested = true;
					break;
				}
			}
			return interested;
		}

		public synchronized void sendInterestedOrNotInterestedMessage() {	
			boolean isInterested = checkIfPeerHasInterestingPieces();
			byte[] message;
			//send interested msg as peer has pieces that I don't have
			message = isInterested ? receiveMessageInfo(PeerConstants.message.INTERESTED.getValue(),null):receiveMessageInfo(PeerConstants.message.NOT_INTERESTED.getValue(),null);		
			try {
				outputStream.write(message);
				if(isInterested) {
					logsObj.interestedMessage(sourcePeerId, peerId);
				}
				else {
					logsObj.notInterestedmessage(sourcePeerId, peerId);
				}
				outputStream.flush();
			}catch(IOException ie) {
				ie.printStackTrace();
			}	
		}

		//get index of a random chunk that peer has and I don't have. If no such chunk is found return -1
		private synchronized int findAndCompareRandomPiece() {
			int randomPieceIndex = -1;
			List<Integer> pieceIndexOfPeer = new ArrayList<>();
			int[] peer_bitfield = peerNode.getBitfield();
			for(int i=0;i<totalPieces;i++) {
				//add index of the chunk that Peer has and I don't have
				if(mapBitfield.get(i) == 0 && peer_bitfield[i] == 1) { 
					pieceIndexOfPeer.add(i);
				}
			}
			//select a random chunk that peer has and I don't have
			if(pieceIndexOfPeer.size() > 0) {
				Random rand = new Random();
				int randomIdx = rand.nextInt(pieceIndexOfPeer.size());
				randomPieceIndex = pieceIndexOfPeer.get(randomIdx);
			}

			return randomPieceIndex;
		}

		public synchronized void sendRequestMessage() {
			int randomPieceIndex = findAndCompareRandomPiece();
			if(randomPieceIndex != -1) {
				ByteBuffer bufferBytes = ByteBuffer.allocate(4);
				byte[] payload = bufferBytes.putInt(randomPieceIndex).array();
				byte[] message = receiveMessageInfo(PeerConstants.message.REQUEST.getValue(),payload);
				try {
					outputStream.write(message);
					logsObj.requestMessage(sourcePeerId, peerId, randomPieceIndex);
					outputStream.flush();
				}catch(IOException ie) {
					ie.printStackTrace();
				}			
			}
			else {
				sendInterestedOrNotInterestedMessage();
			}
		}

		//send a piece that is requested by the peer if the peer is unchoked or optimistically unchoked and u have the piece
		public synchronized void sendRequestedPiece(int pieceIndex) {
			if((unchoked || (optimisticallyUnchokedPeer.get() == peerId)) && mapBitfield.get(pieceIndex) == 1) {
				try {
					byte[] piece = peerCommonUtilObj.returnPiece(sourcePeerId, pieceIndex);
					ByteBuffer bufferBytes = ByteBuffer.allocate(4);
					byte[] index = bufferBytes.putInt(pieceIndex).array();
					byte[] payload = new byte[index.length+piece.length];
					System.arraycopy(index, 0, payload, 0, index.length);
					System.arraycopy(piece, 0, payload, 4, piece.length);
					byte[] pieceMsg = receiveMessageInfo(PeerConstants.message.PIECE.getValue(),payload);
					outputStream.write(pieceMsg);
					logsObj.requestPiece(sourcePeerId, peerId, pieceIndex);
					outputStream.flush();
				}catch(IOException ie){
					ie.printStackTrace();
				}		
			}
		}

		public synchronized void sendHaveMessage(int pieceIndex) {
			ByteBuffer bb = ByteBuffer.allocate(4);
			byte[] payload = bb.putInt(pieceIndex).array();
			byte[] message = receiveMessageInfo(PeerConstants.message.HAVE.getValue(),payload);
			try {
				outputStream.write(message);
				outputStream.flush();
				logsObj.haveMessage(sourcePeerId, peerId, pieceIndex);
			}catch(IOException ie) {
				ie.printStackTrace();
			}
		}

		//update peer bitfield when u receive a 'have' message from the peer and send a request for the piece if you don't have it
		public synchronized void updateNeighbourPeerBitfield(int havePieceIndex) {
			int[] peer_bitfield = peerNode.getBitfield();
			peer_bitfield[havePieceIndex] = 1;
			peerNode.setBitfield(peer_bitfield);
			//			System.out.println(peerNode.getPeerId()+"'s file");
			//			for(int i = 0;i<peer_bitfield.length;i++)
			//			   System.out.print(peer_bitfield[i]);
			//			System.out.println("");
			if(mapBitfield.get(havePieceIndex) == 0) {
				ByteBuffer bb = ByteBuffer.allocate(4);
				byte[] payload = bb.putInt(havePieceIndex).array();
				byte[] message = receiveMessageInfo(PeerConstants.message.REQUEST.getValue(),payload);
				try {
					outputStream.write(message);
					outputStream.flush();
				}catch(IOException ie) {
					ie.printStackTrace();
				}
			}
		}

		public synchronized void checkForWholeFile() {
			int[] peer_bitfield = peerNode.getBitfield();
			boolean hasPeerCompletedFile = true;
			for(int i=0;i<peer_bitfield.length;i++) {
				if(peer_bitfield[i] == 0) {
					hasPeerCompletedFile = false;
					break;
				}
			}
			if(hasPeerCompletedFile) {
				int index = interestedPeersList.indexOf(peerId);
				if(index != -1) {
					interestedPeersList.remove(index);
				}
				if(optimisticallyUnchokedPeer.get() == peerId) {
					optimisticallyUnchokedPeer.set(-1);
				}
				sendChokeMessage();
				peerNode.setHaveFile(1);
				if(!completedPeersList.contains(peerId)) {
					peerdownloadRate.put(peerId,-1);
					completedPeersList.add(peerId);
					peersHavingWholeFile.incrementAndGet(); //if neighbor has completed file
					System.out.println(peerId +" (neighbor)has finished downloading");			
				}

				if(!wholeFile) {//if I haven't completed downloading, then send interested message
					sendInterestedOrNotInterestedMessage();
				}
			}
		}

		class NeighborPeerInteractionThread implements Runnable{

			public void run() {		
				//System.out.println(peerId);
				peerdownloadRate.put(peerId, 0);
				sendBitFieldMessages();
				try {
					while(peersHavingWholeFile.get() < noOfPeers) {

						//Read first 4 bytes of the message which is size of the payload
						for(int i = 0;i<4;i++) {
							inputStream.read(message, i, 1);
						}		

						int size = ByteBuffer.wrap(message).getInt();
						//System.out.println("size of message = "+size);

						//Read next 1 byte which is the type of message
						inputStream.read(message,0,1);
						int type = message[0];
						//System.out.println("type of message = " + type);

						//if type = Bitfield
						if(type == PeerConstants.message.BITFIELD.getValue()) {
							byte[] bytes = new byte[size-1];
							for(int i = 0;i<size-1;i++) {
								inputStream.read(bytes, i, 1);
							}
							int[] peer_bitfield = convertByteToIntArray(bytes);
							peerNode.setBitfield(peer_bitfield);
							//if peer has full file, then increase peersHavingWholeFile
							boolean hasFullFile = true;
							logsObj.logReceivebitfield(sourcePeerId, peerId);
							for(int i=0;i<peer_bitfield.length;i++) {
								if(peer_bitfield[i] == 0) {
									hasFullFile = false;
									break;
								}
							}
							if(hasFullFile && !completedPeersList.contains(peerId)) {
								peerNode.setHaveFile(1);
								completedPeersList.add(peerId);
								peersHavingWholeFile.incrementAndGet();//if neighbor has the full file initially
								System.out.println(peerId + " has the full file");
								sendInterestedOrNotInterestedMessage();//send interested message since the peer has full file
							}
						}
						else if(type == PeerConstants.message.INTERESTED.getValue()) {
							//System.out.println(peerId +" Interested");
							interestedPeersList.addIfAbsent(peerId);
							logsObj.receiveInterestedmessage(sourcePeerId, peerId);
						}
						//if the peer sends not interested message, then remove it from interested_peerslist and choke it.
						else if(type == PeerConstants.message.NOT_INTERESTED.getValue()) {
							int index = interestedPeersList.indexOf(peerId);
							if(index != -1) {
								logsObj.receiveNotinterestedMessage(sourcePeerId, peerId);
								interestedPeersList.remove(index);
							}				
							if(peerId == optimisticallyUnchokedPeer.get()) {
								optimisticallyUnchokedPeer.set(-1);
							}
							sendChokeMessage();
						}
						else if(type == PeerConstants.message.CHOKE.getValue()) {
							//System.out.println(peerId +" has choked me");
							logsObj.Choking(sourcePeerId, peerId);

						}
						else if(type == PeerConstants.message.UNCHOKE.getValue()) {
							//send request message if peer has some interesting piece that I don't have else do nothing
							//System.out.println(peerId +" has unchoked me");
							logsObj.Unchoking(sourcePeerId, peerId);
							sendRequestMessage(); 
						}
						else if(type == PeerConstants.message.REQUEST.getValue()) {

							interestedPeersList.addIfAbsent(peerId);
							//get the index of the requested piece from the payload
							byte[] payload = new byte[size-1];
							for(int i = 0;i<size-1;i++) {
								inputStream.read(payload, i, 1);
							}
							int indexPiece = ByteBuffer.wrap(payload).getInt();
							//System.out.println(peerId +" has requested piece " + indexPiece);
							logsObj.receiveRequestdmessage(sourcePeerId, peerId, indexPiece);

							//send the requested piece only if the peer is either unchoked or optimistically unchoked and if source peer has the piece
							sendRequestedPiece(indexPiece);					
						}
						//Received piece, write it to the file, update my bitfield, check if u have completed downloading the file. If no then send request message to this peer and send have message to all the peers
						else if(type == PeerConstants.message.PIECE.getValue()) {
							byte[] piece = new byte[size-5];
							byte[] indexArr = new byte[4];
							for(int i = 0;i<4;i++) {
								inputStream.read(indexArr,i,1);
							}
							int indexOfPieceRecvd = ByteBuffer.wrap(indexArr).getInt();
							int downloadRatePeer = peerdownloadRate.get(peerId);
							peerdownloadRate.put(peerId, downloadRatePeer+1);
							if(mapBitfield.get(indexOfPieceRecvd) == 0) {
								//System.out.println("Recieved piece " +indexOfPieceRecvd+" from "+peerId);
								mapBitfield.put(indexOfPieceRecvd, 1);
								for(int i=0;i<piece.length;i++) {
									inputStream.read(piece, i, 1);
								}
								peerCommonUtilObj.writePiece(sourcePeerId, indexOfPieceRecvd, piece);
								boolean haveICompleted = true;
								int numberOfPiecesIHave = 0;
								for(Map.Entry<Integer, Integer> e:mapBitfield.entrySet()) {
									if(e.getValue() == 0) {
										haveICompleted = false;
									}
									else {
										numberOfPiecesIHave++;
									}
								}
								logsObj.getPiece(sourcePeerId, peerId, indexOfPieceRecvd, numberOfPiecesIHave);
								//check if I have completed the full file, if yes increment peersHavingWholeFile
								if(haveICompleted && !completedPeersList.contains(sourcePeerId)) {
									completedPeersList.add(sourcePeerId);
									peersHavingWholeFile.incrementAndGet(); //check if I have completed the download of file fully 
									System.out.println(sourcePeerId +" (I) have completed downloading");
									wholeFile = true;
									logsObj.downloadComplete(sourcePeerId);
									TimeUnit.SECONDS.sleep(2);
									peerCommonUtilObj.joinChunksintoFile(sourcePeerId, commonConfigObj);
									//broadcast 'bitfield' message to all the peers	so that they can update their bitfield copy of ur bitfield 			
									//									for(Map.Entry<Integer, NeighborPeerInteraction> entry:peerBondsNeighbors.entrySet()) {
									//										NeighborPeerInteraction npiObjAdjacentPeer = entry.getValue();
									//										npiObjAdjacentPeer.sendBitFieldMessages();
									//									}
								}
								else if(!completedPeersList.contains(sourcePeerId)){
									sendRequestMessage();
								}
							}				

							//broadcast 'have' message to all the peers	so that they can update their bitfield copy of ur bitfield and request this piece if they don't have it			
							for(Map.Entry<Integer, NeighborPeerInteraction> entry:peerBondsNeighbors.entrySet()) {
								NeighborPeerInteraction npiObjAdjacentPeer = entry.getValue();
								npiObjAdjacentPeer.sendHaveMessage(indexOfPieceRecvd);
							}
						}
						//update neighbour peerbitfield, check if the peer has completed the file and update peersHavingWholeFile accordingly
						else if(type == PeerConstants.message.HAVE.getValue()) {
							byte[] index = new byte[4];
							for(int i=0;i<4;i++) {
								inputStream.read(index, i, 1);
							}
							int havePieceIndex = ByteBuffer.wrap(index).getInt();

							if(havePieceIndex > -1) {
								logsObj.receiveHavemessage(sourcePeerId, peerId, havePieceIndex);
								updateNeighbourPeerBitfield(havePieceIndex);
								checkForWholeFile();
							}

						}
						else if(type == PeerConstants.message.COMPLETE.getValue()) {
							logsObj.killPeerprocess();
							System.exit(0);
						}

					}
					System.out.println(peerId +" Thread finished");
					System.out.println("Total peers with Full file = "+peersHavingWholeFile.get());
					//TimeUnit.SECONDS.sleep(8);
					//inputStream.close();
					//outputStream.close();
					//socket.close();
				}
				catch(IOException e) {
					//System.exit(0);
					//e.printStackTrace();
				}

				catch(Exception e) {
					//System.exit(0);
					//e.printStackTrace();
				}
				//System.exit(0);
			}
		}

	}

	/* A peerNode with peerId is
	  Only OptimisticallyUnchoked = if optimisticallyUnchokedPeer == peerId 
	  OptimisticallyUnchoked && Unchoked = if (optimisticallyUnchokedPeer == peerId && peerNode.unchoked == true)
	  Only unchoked = if peerNode.unchoked == true
	 */

	class ChokeUnChoke implements Runnable{

		public List<Map.Entry<Integer, Integer> > getPeersAccordingToDownloadRate() {
			List<Map.Entry<Integer, Integer> > sortedPeersList = new LinkedList<Map.Entry<Integer, Integer> >(peerdownloadRate.entrySet()); 
			Collections.sort(sortedPeersList, new Comparator<Map.Entry<Integer, Integer> >() { 
				public int compare(Map.Entry<Integer, Integer> o1,  
						Map.Entry<Integer, Integer> o2) 
				{ 
					return (o1.getValue()).compareTo(o2.getValue()); 
				} 
			});
			return sortedPeersList;
		}

		public void run() {

			int unchokeInterval = commonConfigObj.getunchokingInterval();
			try {
				while(peersHavingWholeFile.get() < noOfPeers) {

					int interestedPeersSize = interestedPeersList.size();
					if(interestedPeersSize > 0) {
						int preferredNeighbors = commonConfigObj.getNumPreferredNeighbours();

						if(interestedPeersSize < preferredNeighbors) {
							Iterator<Integer> itr = interestedPeersList.iterator();
							while(itr.hasNext()) {
								int peerId = itr.next();
								NeighborPeerInteraction npiObj = peerBondsNeighbors.get(peerId);
								if(!npiObj.unchoked) {//Don't send unchoked if it is already unchoked
									npiObj.sendUnChokeMessage(false);
								}							
							}
						}
						else {
							//Get interested peers according to download rate and add them or select randomly if the peer has full file
							List<Map.Entry<Integer, Integer> > sortedPeersMapList = getPeersAccordingToDownloadRate();
							List<Integer> sortedPeersDR = new ArrayList<>();
							for(Map.Entry<Integer, Integer> e:sortedPeersMapList) {
								sortedPeersDR.add(e.getKey());
							}
							Random rand = new Random();
							List<Integer> tempPeersList = new ArrayList<>(interestedPeersList);
							int[] preferredPeers = new int[preferredNeighbors];
							//select preferred Neighbors and unchoke them
							for(int i=0;i<preferredNeighbors;i++) {
								int randomIdx = rand.nextInt(tempPeersList.size());
								int peerId = tempPeersList.get(randomIdx);
								preferredPeers[i] = peerId;
								NeighborPeerInteraction npiObj = peerBondsNeighbors.get(peerId);
								//Don't send unchoked if it is already unchoked
								if(!npiObj.unchoked) {
									npiObj.sendUnChokeMessage(false);
								}							
								tempPeersList.remove(randomIdx);
							}
							
							logsObj.prefferedNeighbors(sourcePeerId, preferredPeers);							

							//choke the peers who have not been selected
							Iterator<Integer> itr = tempPeersList.iterator();
							while(itr.hasNext()) {
								int peerId = itr.next();
								NeighborPeerInteraction npiObj = peerBondsNeighbors.get(peerId);
								npiObj.sendChokeMessage();
							}
						}
					}
					TimeUnit.SECONDS.sleep(unchokeInterval);
				}
			}catch(InterruptedException ie) {
				//System.exit(0);
				//ie.printStackTrace();
			}catch(Exception e) {
				//System.exit(0);
				//e.printStackTrace();
			}
			//System.exit(0);
		}
	}

	//Optimistic Unchoke thread 
	class OptimisticUnChoke implements Runnable{

		public void run() {
			try {
				while(peersHavingWholeFile.get() < noOfPeers) {
					if(interestedPeersList.size() > 0) {
						int optimisticSleepingInterval = commonConfigObj.getOptimisticUnchokingInterval();
						int interestedPeersSize = interestedPeersList.size();
						Random rand = new Random();
						int random = rand.nextInt(interestedPeersSize); 
						int peerId = interestedPeersList.get(random);
						optimisticallyUnchokedPeer.set(peerId);
						NeighborPeerInteraction npiObj = peerBondsNeighbors.get(peerId);
						npiObj.sendUnChokeMessage(true);
						logsObj.optimisticUnchoking(sourcePeerId, peerId);
						TimeUnit.SECONDS.sleep(optimisticSleepingInterval);
						optimisticallyUnchokedPeer.set(-1);

						//choke the peer if it is only optimistically unchoked and not an unchoked peer 
						if(!npiObj.unchoked) {
							npiObj.sendChokeMessage();
						}			
					}
				}
			}
			catch(InterruptedException ie) {
				//System.exit(0);
				//ie.printStackTrace();
			}
			//System.exit(0);
		}
	}

	//Establishes TCP Connections with all the peers who started before the current peer by exchanging handshake packets with them
	class Client implements Runnable{

		@Override
		public void run() {
			int index = 0;
			Iterator<Entry<Integer, NeighbourPeerNode>> itr = peerNeighbors.entrySet().iterator();

			while(index < currentPeerIndex) {
				Entry<Integer, NeighbourPeerNode> entry = itr.next();
				int peerId = entry.getKey();
				NeighbourPeerNode peerObj = entry.getValue();
				String hostName = peerObj.getHostName();
				int portNumber = peerObj.getPortNumber();
				try {	
					//Establish TCP Connection
					Socket socket = new Socket(hostName,portNumber);
					DataInputStream inputStream = new DataInputStream(socket.getInputStream());
					DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

					//Send handshake
					byte[] handShakeHeader = peerCommonUtilObj.getHandshakePacket(sourcePeerId);
					outputStream.write(handShakeHeader);

					//Receive handshake
					byte[] receivedHandshake = new byte[handShakeHeader.length];
					inputStream.readFully(receivedHandshake);
					int receivedPeerId = Integer.parseInt(new String(Arrays.copyOfRange(receivedHandshake,28,32)));

					//Add the socket to connection established 
					if(receivedPeerId == peerId) {		
						//System.out.println(receivedPeerId);
						NeighborPeerInteraction npi = new NeighborPeerInteraction(socket,peerObj);
						npi.beginCommunication();
						peerBondsNeighbors.put(peerId, npi);
						logsObj.sourceTcpconnection(sourcePeerId, peerId);
					}		
					index++;
					outputStream.flush();

				}
				catch(UnknownHostException uhe) {
					uhe.printStackTrace();
				}
				catch(IOException ioe) {
					ioe.printStackTrace();
				}
				catch(Exception ie) {
					ie.printStackTrace();
				}
			}			
		}
	}

	//Establishes TCP Connections with all the peers starting after current peer by waiting for their requests and then exchanging handshake packets with them once it is done.
	class Server implements Runnable{

		@Override
		public void run() {
			int index = currentPeerIndex;
			try {
				listener = new ServerSocket(inputPort);
				while(index < noOfPeers-1) {
					Socket socket = listener.accept();
					DataInputStream inputStream = new DataInputStream(socket.getInputStream());
					DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

					//Receive handshake
					byte[] handshakePacket = new byte[32];
					inputStream.readFully(handshakePacket);
					int peerId = Integer.parseInt(new String(Arrays.copyOfRange(handshakePacket,28,32)));
					// System.out.println(peerId);

					//send Handshake
					byte[] sendHandshake = peerCommonUtilObj.getHandshakePacket(sourcePeerId);
					outputStream.write(sendHandshake);

					NeighbourPeerNode peerObj = peerNeighbors.get(peerId);
					NeighborPeerInteraction npi = new NeighborPeerInteraction(socket,peerObj);
					npi.beginCommunication();
					peerBondsNeighbors.put(peerId, npi);
					logsObj.destinationTcpconnection(sourcePeerId, peerId);
					index++;

				}
				//	listener.close();
			}
			catch(UnknownHostException uhe) {
				uhe.printStackTrace();
			}
			catch(IOException ioe) {
				ioe.printStackTrace();
			}
			catch(Exception ie) {
				ie.printStackTrace();
			}

		}
	}

	//set adjacent peer nodes and add to peerMap
	private static void setPeerNodes(List<String> peerRows)throws Exception{
		noOfPeers = 0;

		for(String row:peerRows) {
			int peerId = Integer.parseInt(row.split(" ")[0]);
			if(peerId == sourcePeerId) {
				currentPeerIndex = noOfPeers;
				inputPort = Integer.parseInt(row.split(" ")[2]);
				wholeFile = Integer.parseInt(row.split(" ")[3]) == 1?true:false;

			}
			else {
				NeighbourPeerNode pnObj = NeighbourPeerNode.getPeerNodeObject(row);	
				peerNeighbors.put(peerId,pnObj);
			}
			noOfPeers++;
		}
		noOfPeers = peerRows.size();

	}

	public static void main(String[] args) throws Exception {

		sourcePeerId = Integer.parseInt(args[0]);
		//Read Common.cfg and set the CommonConfig object

		rdIpObj = ReadInput.getReadInputObj();
		List<String> configRows = rdIpObj.parseInput("Common.cfg");
		commonConfigObj = CommonConfig.getCommonConfigFileObj(configRows);
		totalPieces = commonConfigObj.getNoOfChunks();
		peerCommonUtilObj = new PeerCommonUtil();
		//	System.out.println(configFileReader.getNoOfNeighbors());

		//Read PeerInfo.cfg and set the PeerNode.java 
		List<String> peerRows = rdIpObj.parseInput("PeerInfo.cfg");
		setPeerNodes(peerRows);

		//make peer directory
		File logFile = peerCommonUtilObj.makePeerAndLogDirectory(sourcePeerId);
		logsObj = new Logs(logFile);
		logsObj.readCommoncfg(sourcePeerId, commonConfigObj);

		//current peer has file, set bitfield as true for all bits and split the file into chunks
		int bit = 0;
		if(wholeFile && !completedPeersList.contains(sourcePeerId)) {
			completedPeersList.add(sourcePeerId);
			peersHavingWholeFile.incrementAndGet(); //if I have the file initially
			System.out.println(sourcePeerId +" (I) have the full file");
			bit = 1;
			peerCommonUtilObj.splitFileintoChunks(""+sourcePeerId, commonConfigObj);
			peerCommonUtilObj.joinChunksintoFile(sourcePeerId, commonConfigObj);
		}

		for(int i = 0;i<totalPieces;i++) {
			mapBitfield.put(i, bit);
		}

		peerProcess p1 = new peerProcess();
		//start the client and server threads to initialize the TCP Connections with all the other peers
		Client clientObj = p1.new Client();
		Thread client = new Thread(clientObj,"Client Thread");
		client.start();

		Server serverObj = p1.new Server();
		Thread server = new Thread(serverObj,"Server Thread");
		server.start();
		//end

		ChokeUnChoke chokeObj = p1.new ChokeUnChoke();
		Thread chokeThread = new Thread(chokeObj,"Choke thread");
		chokeThread.start();

		OptimisticUnChoke optChokeObj = p1.new OptimisticUnChoke();
		Thread optUnChokeThread = new Thread(optChokeObj,"Optimistic Choke thread");
		optUnChokeThread.start();
		System.out.println("Total Peers "+noOfPeers);

		while(true) {
			TimeUnit.SECONDS.sleep(10);
			if(peersHavingWholeFile.get() >= noOfPeers) {
				for(Map.Entry<Integer, NeighborPeerInteraction> entry:peerBondsNeighbors.entrySet()) {
					NeighborPeerInteraction npiObjAdjacentPeer = entry.getValue();
					npiObjAdjacentPeer.sendWholeMessage();
				}

				TimeUnit.SECONDS.sleep(10);
				System.out.println("Correctly exiting");
				logsObj.killPeerprocess();
				System.exit(0);
			}
			//			else {
			//				for(Map.Entry<Integer, NeighborPeerInteraction> entry:peerBondsNeighbors.entrySet()) {
			//					NeighborPeerInteraction npiObjAdjacentPeer = entry.getValue();
			//					npiObjAdjacentPeer.sendBitFieldMessages();
			//				}
			//			}
		}

	}

}
