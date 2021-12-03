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
	private static ReadFiles rfObj = null;
	private static ConfigFile configFileObj = null;
	private static Map<Integer,NeighbourPeerNode> neighborPeers = new LinkedHashMap<>();
	private static int sourcePeerId = -1;
	private static ConcurrentHashMap<Integer,Integer> bitfieldHM = new ConcurrentHashMap<>();
	private static ConcurrentHashMap < Integer, Integer > downloadRate = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Integer,NeighborPeerInteraction> neighborPeerConnections = new ConcurrentHashMap<>();
	private static CopyOnWriteArrayList<Integer> interested_peers = new CopyOnWriteArrayList <> ();
	private static CopyOnWriteArrayList<Integer> unchoked_peers = new CopyOnWriteArrayList <> ();
	private static CopyOnWriteArrayList<Integer> completed_peers = new CopyOnWriteArrayList <> ();
	private static int currentPeerIndex = -1;
	private static int totalPeers = -1;
	private static ServerSocket listener = null;
	private static AtomicInteger peersWithEntireFile = new AtomicInteger(0);
	private static int totalChunks = 0; 
	private static int sourcePortNumber = 0;
	private static AtomicInteger optimisticallyUnchokedPeer = new AtomicInteger(-1);
	private static boolean complete_file;
	private static PeerCommonUtil utilObj = null;
	private static Logs logFileObj = null;

	class NeighborPeerInteraction {
		int peerId = -1;
		Socket socket = null;
		NeighbourPeerNode peerNode = null;
		DataInputStream inputStream = null;
		DataOutputStream outputStream = null;
		boolean unchoked = false;
		byte[] msg = null;

		public NeighborPeerInteraction(Socket socket, NeighbourPeerNode peerNode) throws IOException {
			this.socket = socket;
			this.peerNode = peerNode;
			peerId = peerNode.getPeerId();
			msg = new byte[5];
			inputStream = new DataInputStream(socket.getInputStream());
			outputStream = new DataOutputStream(socket.getOutputStream());				
		}

		//create a new thread and start interaction 
		public void startInteraction() throws Exception {
			NeighborPeerInteractionThread nbit = new NeighborPeerInteractionThread();
			Thread neighborPeerThread = new Thread(nbit,"Thread_"+peerNode.getPeerId());
			neighborPeerThread.start();
			System.out.println(neighborPeerThread.getName() +" started");
		}

		//convert message into bytes that can be sent to other peers
		public synchronized byte[] getMessage(int type,byte[] payload) {
			//message length byte array 
			int payLoadSize = payload != null?payload.length:0;
			int totalLen = 1 + payLoadSize;	
			ByteBuffer bb = ByteBuffer.allocate(4); //allocate 4 bytes to byte buffer
			byte[] messageLength = bb.putInt(totalLen).array();//write the integer value 'totalLen' as 4 bytes

			//message type byte
			byte messageType = (byte)(char)type;

			//concatenate message length byte array, message type byte and payload byte array
			byte[] message = new byte[messageLength.length + totalLen];
			int index = 0;
			for(int i = 0;i<messageLength.length;i++) {
				message[index] = messageLength[i];
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
		public synchronized byte[] intArrayTobyteArray(int[] data) {
			ByteBuffer byteBuffer = ByteBuffer.allocate(data.length * 4);        
			IntBuffer intBuffer = byteBuffer.asIntBuffer();
			intBuffer.put(data);
			byte[] byteArray = byteBuffer.array();
			return byteArray;
		}

		//convert byte array to int array
		public synchronized int[] byteArrayTointArray(byte[] bytes) {
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
		public synchronized void sendBitField(){
			int[] bitfield = new int[totalChunks];
			for(int i=0;i<bitfieldHM.size();i++) {
				bitfield[i] = bitfieldHM.get(i);
			}
			byte[] payload = intArrayTobyteArray(bitfield);
			byte[] message = getMessage(PeerConstants.messageType.BITFIELD.getValue(),payload);
			try {
				outputStream.write(message);
				outputStream.flush();
				logFileObj.log_bitfield_sent(sourcePeerId, peerId);
			}
			catch(IOException ie) {
				ie.printStackTrace();
			}			
		}

		public synchronized void sendCompleteMsg() {
			byte[] message = getMessage(PeerConstants.messageType.COMPLETE.getValue(),null);
			try {
				outputStream.write(message);
				outputStream.flush();
			}
			catch(IOException ie) {
				System.exit(0);
				//ie.printStackTrace();
			}
		}

		//Don't send choke message if the peer is optimistically unchoked but remove it from unchoked_peers and set unchoked = false
		public synchronized void sendChokeMsg() {
			if(optimisticallyUnchokedPeer.get() != peerId) {
				byte[] message = getMessage(PeerConstants.messageType.CHOKE.getValue(),null);
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
				int index = unchoked_peers.indexOf(peerId);
				if(index != -1) {
					unchoked_peers.remove(index);
				}	
			}
		}

		public synchronized void sendUnChokeMsg(boolean isOptimistically) {
			byte[] message = getMessage(PeerConstants.messageType.UNCHOKE.getValue(),null);
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
				unchoked_peers.addIfAbsent(peerId);
			}				
		}

		//check if peer has any interesting pieces that I don't have
		public synchronized boolean checkIfPeerHasInterestingPieces() {
			boolean interested = false;
			int[] peer_bitfield = peerNode.getBitfield();
			for(int i =0;i < configFileObj.getNoOfChunks();i++) {
				if(bitfieldHM.get(i) == 0 && peer_bitfield[i] == 1) {
					interested = true;
					break;
				}
			}
			return interested;
		}

		public synchronized void sendInterestedorNotMsg() {	
			boolean isInterested = checkIfPeerHasInterestingPieces();
			byte[] message;
			//send interested msg as peer has pieces that I don't have
			message = isInterested ? getMessage(PeerConstants.messageType.INTERESTED.getValue(),null):getMessage(PeerConstants.messageType.NOT_INTERESTED.getValue(),null);		
			try {
				outputStream.write(message);
				if(isInterested) {
					logFileObj.log_send_interested_message(sourcePeerId, peerId);
				}
				else {
					logFileObj.log_send_not_interested_message(sourcePeerId, peerId);
				}
				outputStream.flush();
			}catch(IOException ie) {
				ie.printStackTrace();
			}	
		}

		//get index of a random chunk that peer has and I don't have. If no such chunk is found return -1
		private synchronized int getRandomChunkNeeded() {
			int randomPieceIdx = -1;
			List<Integer> chunksIndicesOfPeer = new ArrayList<>();
			int[] peer_bitfield = peerNode.getBitfield();
			for(int i=0;i<totalChunks;i++) {
				//add index of the chunk that Peer has and I don't have
				if(bitfieldHM.get(i) == 0 && peer_bitfield[i] == 1) { 
					chunksIndicesOfPeer.add(i);
				}
			}
			//select a random chunk that peer has and I don't have
			if(chunksIndicesOfPeer.size() > 0) {
				Random rand = new Random();
				int randomIdx = rand.nextInt(chunksIndicesOfPeer.size());
				randomPieceIdx = chunksIndicesOfPeer.get(randomIdx);
			}

			return randomPieceIdx;
		}

		public synchronized void sendRequestMsg() {
			int randomChunkIdx = getRandomChunkNeeded();
			if(randomChunkIdx != -1) {
				ByteBuffer bb = ByteBuffer.allocate(4);
				byte[] payload = bb.putInt(randomChunkIdx).array();
				byte[] message = getMessage(PeerConstants.messageType.REQUEST.getValue(),payload);
				try {
					outputStream.write(message);
					logFileObj.log_send_request_message(sourcePeerId, peerId, randomChunkIdx);
					outputStream.flush();
				}catch(IOException ie) {
					ie.printStackTrace();
				}			
			}
			else {
				sendInterestedorNotMsg();
			}
		}

		//send a piece that is requested by the peer if the peer is unchoked or optimistically unchoked and u have the piece
		public synchronized void sendPieceMsg(int pieceIndex) {
			if((unchoked || (optimisticallyUnchokedPeer.get() == peerId)) && bitfieldHM.get(pieceIndex) == 1) {
				try {
					byte[] piece = utilObj.returnPiece(sourcePeerId, pieceIndex);
					ByteBuffer bb = ByteBuffer.allocate(4);
					byte[] index = bb.putInt(pieceIndex).array();
					byte[] payload = new byte[index.length+piece.length];
					System.arraycopy(index, 0, payload, 0, index.length);
					System.arraycopy(piece, 0, payload, 4, piece.length);
					byte[] pieceMsg = getMessage(PeerConstants.messageType.PIECE.getValue(),payload);
					outputStream.write(pieceMsg);
					logFileObj.log_send_piece_message(sourcePeerId, peerId, pieceIndex);
					outputStream.flush();
				}catch(IOException ie){
					ie.printStackTrace();
				}		
			}
		}

		public synchronized void sendHaveMsg(int pieceIndex) {
			ByteBuffer bb = ByteBuffer.allocate(4);
			byte[] payload = bb.putInt(pieceIndex).array();
			byte[] message = getMessage(PeerConstants.messageType.HAVE.getValue(),payload);
			try {
				outputStream.write(message);
				outputStream.flush();
				logFileObj.log_send_have_message(sourcePeerId, peerId, pieceIndex);
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
			if(bitfieldHM.get(havePieceIndex) == 0) {
				ByteBuffer bb = ByteBuffer.allocate(4);
				byte[] payload = bb.putInt(havePieceIndex).array();
				byte[] message = getMessage(PeerConstants.messageType.REQUEST.getValue(),payload);
				try {
					outputStream.write(message);
					outputStream.flush();
				}catch(IOException ie) {
					ie.printStackTrace();
				}
			}
		}

		public synchronized void checkForCompleteFile() {
			int[] peer_bitfield = peerNode.getBitfield();
			boolean hasPeerCompletedFile = true;
			for(int i=0;i<peer_bitfield.length;i++) {
				if(peer_bitfield[i] == 0) {
					hasPeerCompletedFile = false;
					break;
				}
			}
			if(hasPeerCompletedFile) {
				int index = interested_peers.indexOf(peerId);
				if(index != -1) {
					interested_peers.remove(index);
				}
				if(optimisticallyUnchokedPeer.get() == peerId) {
					optimisticallyUnchokedPeer.set(-1);
				}
				sendChokeMsg();
				peerNode.setHaveFile(1);
				if(!completed_peers.contains(peerId)) {
					downloadRate.put(peerId,-1);
					completed_peers.add(peerId);
					peersWithEntireFile.incrementAndGet(); //if neighbor has completed file
					System.out.println(peerId +" (neighbor)has finished downloading");			
				}

				if(!complete_file) {//if I haven't completed downloading, then send interested message
					sendInterestedorNotMsg();
				}
			}
		}

		class NeighborPeerInteractionThread implements Runnable{

			public void run() {		
				//System.out.println(peerId);
				downloadRate.put(peerId, 0);
				sendBitField();
				try {
					while(peersWithEntireFile.get() < totalPeers) {

						//Read first 4 bytes of the message which is size of the payload
						for(int i = 0;i<4;i++) {
							inputStream.read(msg, i, 1);
						}		

						int size = ByteBuffer.wrap(msg).getInt();
						//System.out.println("size of message = "+size);

						//Read next 1 byte which is the type of message
						inputStream.read(msg,0,1);
						int type = msg[0];
						//System.out.println("type of message = " + type);

						//if type = Bitfield
						if(type == PeerConstants.messageType.BITFIELD.getValue()) {
							byte[] bytes = new byte[size-1];
							for(int i = 0;i<size-1;i++) {
								inputStream.read(bytes, i, 1);
							}
							int[] peer_bitfield = byteArrayTointArray(bytes);
							peerNode.setBitfield(peer_bitfield);
							//if peer has full file, then increase peersWithEntireFile
							boolean hasFullFile = true;
							logFileObj.log_bitfield_received(sourcePeerId, peerId);
							for(int i=0;i<peer_bitfield.length;i++) {
								if(peer_bitfield[i] == 0) {
									hasFullFile = false;
									break;
								}
							}
							if(hasFullFile && !completed_peers.contains(peerId)) {
								peerNode.setHaveFile(1);
								completed_peers.add(peerId);
								peersWithEntireFile.incrementAndGet();//if neighbor has the full file initially
								System.out.println(peerId + " has the full file");
								sendInterestedorNotMsg();//send interested message since the peer has full file
							}
						}
						else if(type == PeerConstants.messageType.INTERESTED.getValue()) {
							//System.out.println(peerId +" Interested");
							interested_peers.addIfAbsent(peerId);
							logFileObj.log_receiving_interested_message(sourcePeerId, peerId);
						}
						//if the peer sends not interested message, then remove it from interested_peerslist and choke it.
						else if(type == PeerConstants.messageType.NOT_INTERESTED.getValue()) {
							int index = interested_peers.indexOf(peerId);
							if(index != -1) {
								logFileObj.log_receiving_not_interested_message(sourcePeerId, peerId);
								interested_peers.remove(index);
							}				
							if(peerId == optimisticallyUnchokedPeer.get()) {
								optimisticallyUnchokedPeer.set(-1);
							}
							sendChokeMsg();
						}
						else if(type == PeerConstants.messageType.CHOKE.getValue()) {
							//System.out.println(peerId +" has choked me");
							logFileObj.log_choking(sourcePeerId, peerId);

						}
						else if(type == PeerConstants.messageType.UNCHOKE.getValue()) {
							//send request message if peer has some interesting piece that I don't have else do nothing
							//System.out.println(peerId +" has unchoked me");
							logFileObj.log_unchoking(sourcePeerId, peerId);
							sendRequestMsg(); 
						}
						else if(type == PeerConstants.messageType.REQUEST.getValue()) {

							interested_peers.addIfAbsent(peerId);
							//get the index of the requested piece from the payload
							byte[] payload = new byte[size-1];
							for(int i = 0;i<size-1;i++) {
								inputStream.read(payload, i, 1);
							}
							int indexPiece = ByteBuffer.wrap(payload).getInt();
							//System.out.println(peerId +" has requested piece " + indexPiece);
							logFileObj.log_receiving_request_message(sourcePeerId, peerId, indexPiece);

							//send the requested piece only if the peer is either unchoked or optimistically unchoked and if source peer has the piece
							sendPieceMsg(indexPiece);					
						}
						//Received piece, write it to the file, update my bitfield, check if u have completed downloading the file. If no then send request message to this peer and send have message to all the peers
						else if(type == PeerConstants.messageType.PIECE.getValue()) {
							byte[] piece = new byte[size-5];
							byte[] indexArr = new byte[4];
							for(int i = 0;i<4;i++) {
								inputStream.read(indexArr,i,1);
							}
							int indexOfPieceRecvd = ByteBuffer.wrap(indexArr).getInt();
							int downloadRatePeer = downloadRate.get(peerId);
							downloadRate.put(peerId, downloadRatePeer+1);
							if(bitfieldHM.get(indexOfPieceRecvd) == 0) {
								//System.out.println("Recieved piece " +indexOfPieceRecvd+" from "+peerId);
								bitfieldHM.put(indexOfPieceRecvd, 1);
								for(int i=0;i<piece.length;i++) {
									inputStream.read(piece, i, 1);
								}
								utilObj.writePiece(sourcePeerId, indexOfPieceRecvd, piece);
								boolean haveICompleted = true;
								int numberOfPiecesIHave = 0;
								for(Map.Entry<Integer, Integer> e:bitfieldHM.entrySet()) {
									if(e.getValue() == 0) {
										haveICompleted = false;
									}
									else {
										numberOfPiecesIHave++;
									}
								}
								logFileObj.log_downloading_a_piece(sourcePeerId, peerId, indexOfPieceRecvd, numberOfPiecesIHave);
								//check if I have completed the full file, if yes increment peersWithEntireFile
								if(haveICompleted && !completed_peers.contains(sourcePeerId)) {
									completed_peers.add(sourcePeerId);
									peersWithEntireFile.incrementAndGet(); //check if I have completed the download of file fully 
									System.out.println(sourcePeerId +" (I) have completed downloading");
									complete_file = true;
									logFileObj.log_completion_of_download(sourcePeerId);
									TimeUnit.SECONDS.sleep(2);
									utilObj.joinChunksintoFile(sourcePeerId, configFileObj);
									//broadcast 'bitfield' message to all the peers	so that they can update their bitfield copy of ur bitfield 			
									//									for(Map.Entry<Integer, NeighborPeerInteraction> entry:neighborPeerConnections.entrySet()) {
									//										NeighborPeerInteraction npiObjAdjacentPeer = entry.getValue();
									//										npiObjAdjacentPeer.sendBitField();
									//									}
								}
								else if(!completed_peers.contains(sourcePeerId)){
									sendRequestMsg();
								}
							}				

							//broadcast 'have' message to all the peers	so that they can update their bitfield copy of ur bitfield and request this piece if they don't have it			
							for(Map.Entry<Integer, NeighborPeerInteraction> entry:neighborPeerConnections.entrySet()) {
								NeighborPeerInteraction npiObjAdjacentPeer = entry.getValue();
								npiObjAdjacentPeer.sendHaveMsg(indexOfPieceRecvd);
							}
						}
						//update neighbour peerbitfield, check if the peer has completed the file and update peersWithEntireFile accordingly
						else if(type == PeerConstants.messageType.HAVE.getValue()) {
							byte[] index = new byte[4];
							for(int i=0;i<4;i++) {
								inputStream.read(index, i, 1);
							}
							int havePieceIndex = ByteBuffer.wrap(index).getInt();

							if(havePieceIndex > -1) {
								logFileObj.log_receiving_have_message(sourcePeerId, peerId, havePieceIndex);
								updateNeighbourPeerBitfield(havePieceIndex);
								checkForCompleteFile();
							}

						}
						else if(type == PeerConstants.messageType.COMPLETE.getValue()) {
							logFileObj.log_completion_of_process();
							System.exit(0);
						}

					}
					System.out.println(peerId +" Thread finished");
					System.out.println("Total peers with Full file = "+peersWithEntireFile.get());
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
			List<Map.Entry<Integer, Integer> > sortedPeersList = new LinkedList<Map.Entry<Integer, Integer> >(downloadRate.entrySet()); 
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

			int unchokeInterval = configFileObj.getUnChokingInterval();
			try {
				while(peersWithEntireFile.get() < totalPeers) {

					int interestedPeersSize = interested_peers.size();
					if(interestedPeersSize > 0) {
						int preferredNeighbors = configFileObj.getNoOfNeighbors();

						if(interestedPeersSize < preferredNeighbors) {
							Iterator<Integer> itr = interested_peers.iterator();
							while(itr.hasNext()) {
								int peerId = itr.next();
								NeighborPeerInteraction npiObj = neighborPeerConnections.get(peerId);
								if(!npiObj.unchoked) {//Don't send unchoked if it is already unchoked
									npiObj.sendUnChokeMsg(false);
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
							List<Integer> tempPeersList = new ArrayList<>(interested_peers);
							int[] preferredPeers = new int[preferredNeighbors];
							//select preferred Neighbors and unchoke them
							for(int i=0;i<preferredNeighbors;i++) {
								int randomIdx = rand.nextInt(tempPeersList.size());
								int peerId = tempPeersList.get(randomIdx);
								preferredPeers[i] = peerId;
								NeighborPeerInteraction npiObj = neighborPeerConnections.get(peerId);
								//Don't send unchoked if it is already unchoked
								if(!npiObj.unchoked) {
									npiObj.sendUnChokeMsg(false);
								}							
								tempPeersList.remove(randomIdx);
							}

							logFileObj.log_change_of_preferred_neighbors(sourcePeerId, preferredPeers);

							//choke the peers who have not been selected
							Iterator<Integer> itr = tempPeersList.iterator();
							while(itr.hasNext()) {
								int peerId = itr.next();
								NeighborPeerInteraction npiObj = neighborPeerConnections.get(peerId);
								npiObj.sendChokeMsg();
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
				while(peersWithEntireFile.get() < totalPeers) {
					if(interested_peers.size() > 0) {
						int optimisticSleepingInterval = configFileObj.getOptUnChokingInterval();
						int interestedPeersSize = interested_peers.size();
						Random rand = new Random();
						int random = rand.nextInt(interestedPeersSize); 
						int peerId = interested_peers.get(random);
						optimisticallyUnchokedPeer.set(peerId);
						NeighborPeerInteraction npiObj = neighborPeerConnections.get(peerId);
						npiObj.sendUnChokeMsg(true);
						logFileObj.log_change_of_optimistically_unchoked_neighbor(sourcePeerId, peerId);
						TimeUnit.SECONDS.sleep(optimisticSleepingInterval);
						optimisticallyUnchokedPeer.set(-1);

						//choke the peer if it is only optimistically unchoked and not an unchoked peer 
						if(!npiObj.unchoked) {
							npiObj.sendChokeMsg();
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
			Iterator<Entry<Integer, NeighbourPeerNode>> itr = neighborPeers.entrySet().iterator();

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
					byte[] handShakeHeader = utilObj.getHandshakePacket(sourcePeerId);
					outputStream.write(handShakeHeader);

					//Receive handshake
					byte[] receivedHandshake = new byte[handShakeHeader.length];
					inputStream.readFully(receivedHandshake);
					int receivedPeerId = Integer.parseInt(new String(Arrays.copyOfRange(receivedHandshake,28,32)));

					//Add the socket to connection established 
					if(receivedPeerId == peerId) {		
						//System.out.println(receivedPeerId);
						NeighborPeerInteraction npi = new NeighborPeerInteraction(socket,peerObj);
						npi.startInteraction();
						neighborPeerConnections.put(peerId, npi);
						logFileObj.log_tcp_connection_to(sourcePeerId, peerId);
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
				listener = new ServerSocket(sourcePortNumber);
				while(index < totalPeers-1) {
					Socket socket = listener.accept();
					DataInputStream inputStream = new DataInputStream(socket.getInputStream());
					DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

					//Receive handshake
					byte[] handshakePacket = new byte[32];
					inputStream.readFully(handshakePacket);
					int peerId = Integer.parseInt(new String(Arrays.copyOfRange(handshakePacket,28,32)));
					// System.out.println(peerId);

					//send Handshake
					byte[] sendHandshake = utilObj.getHandshakePacket(sourcePeerId);
					outputStream.write(sendHandshake);

					NeighbourPeerNode peerObj = neighborPeers.get(peerId);
					NeighborPeerInteraction npi = new NeighborPeerInteraction(socket,peerObj);
					npi.startInteraction();
					neighborPeerConnections.put(peerId, npi);
					logFileObj.log_tcp_connection_from(sourcePeerId, peerId);
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
		totalPeers = 0;

		for(String row:peerRows) {
			int peerId = Integer.parseInt(row.split(" ")[0]);
			if(peerId == sourcePeerId) {
				currentPeerIndex = totalPeers;
				sourcePortNumber = Integer.parseInt(row.split(" ")[2]);
				complete_file = Integer.parseInt(row.split(" ")[3]) == 1?true:false;

			}
			else {
				NeighbourPeerNode pnObj = NeighbourPeerNode.getPeerNodeObject(row);	
				neighborPeers.put(peerId,pnObj);
			}
			totalPeers++;
		}
		totalPeers = peerRows.size();

	}

	public static void main(String[] args) throws Exception {

		sourcePeerId = Integer.parseInt(args[0]);
		//Read Common.cfg and set the ConfigFile object

		rfObj = ReadFiles.getReadFilesObj();
		List<String> configRows = rfObj.parseTheFile("Common.cfg");
		configFileObj = ConfigFile.getConfigFileObject(configRows);
		totalChunks = configFileObj.getNoOfChunks();
		utilObj = new PeerCommonUtil();
		//	System.out.println(configFileReader.getNoOfNeighbors());

		//Read PeerInfo.cfg and set the PeerNode.java 
		List<String> peerRows = rfObj.parseTheFile("PeerInfo.cfg");
		setPeerNodes(peerRows);

		//make peer directory
		File logFile = utilObj.makePeerAndLogDirectory(sourcePeerId);
		logFileObj = new Logs(logFile);
		logFileObj.log_readCommonFile(sourcePeerId, configFileObj);

		//current peer has file, set bitfield as true for all bits and split the file into chunks
		int bit = 0;
		if(complete_file && !completed_peers.contains(sourcePeerId)) {
			completed_peers.add(sourcePeerId);
			peersWithEntireFile.incrementAndGet(); //if I have the file initially
			System.out.println(sourcePeerId +" (I) have the full file");
			bit = 1;
			utilObj.splitFileintoChunks(""+sourcePeerId, configFileObj);
			utilObj.joinChunksintoFile(sourcePeerId, configFileObj);
		}

		for(int i = 0;i<totalChunks;i++) {
			bitfieldHM.put(i, bit);
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
		System.out.println("Total Peers "+totalPeers);

		while(true) {
			TimeUnit.SECONDS.sleep(10);
			if(peersWithEntireFile.get() >= totalPeers) {
				for(Map.Entry<Integer, NeighborPeerInteraction> entry:neighborPeerConnections.entrySet()) {
					NeighborPeerInteraction npiObjAdjacentPeer = entry.getValue();
					npiObjAdjacentPeer.sendCompleteMsg();
				}

				TimeUnit.SECONDS.sleep(10);
				System.out.println("Correctly exiting");
				logFileObj.log_completion_of_process();
				System.exit(0);
			}
			//			else {
			//				for(Map.Entry<Integer, NeighborPeerInteraction> entry:neighborPeerConnections.entrySet()) {
			//					NeighborPeerInteraction npiObjAdjacentPeer = entry.getValue();
			//					npiObjAdjacentPeer.sendBitField();
			//				}
			//			}
		}

	}

}