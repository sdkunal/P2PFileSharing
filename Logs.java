import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Logs{
	BufferedWriter logWriter = null;
	File logFile = null;
	public Logs() {}

	public Logs(File logFile) {
		try {
			this.logFile = logFile;
			logWriter = new BufferedWriter(new FileWriter(logFile.getAbsolutePath(),true));
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public synchronized void log_readCommonFile(int id1, ConfigFile cfg) {
		String timeStamp = new SimpleDateFormat("y-M-d 'at' h:m:s a z").format(Calendar.getInstance().getTime());
		StringBuffer log_entry = new StringBuffer();
		log_entry.append(timeStamp + ": Peer ["+id1+"] read Common.cfg file. \n Variables set: Preferred Neighbours = "+cfg.getNoOfNeighbors() + " UnchokingInterval = "+cfg.getUnChokingInterval()+" Optimistic UnchokingInterval = "+cfg.getOptUnChokingInterval()+" File name= " + cfg.getFileName() + " File size = " + cfg.getFileSize()+ " Chunk size = " + cfg.getChunkSize());
		try {
			logWriter.write(log_entry.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
			
		}
	}
	
	public synchronized void log_bitfield_received(int id1, int id2) {
		String timeStamp = new SimpleDateFormat("y-M-d 'at' h:m:s a z").format(Calendar.getInstance().getTime());
		StringBuffer log_entry = new StringBuffer();
		log_entry.append(timeStamp + ": Peer [" + id1 + "] received bitfield from Peer [" + id2 +"]");
		try {
			logWriter.write(log_entry.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e){

		}
	}
	
	public synchronized void log_bitfield_sent(int id1, int id2) {
		String timeStamp = new SimpleDateFormat("y-M-d 'at' h:m:s a z").format(Calendar.getInstance().getTime());
		StringBuffer log_entry = new StringBuffer();
		log_entry.append(timeStamp + ": Peer [" + id1 + "] sent bitfield to Peer [" + id2 +"]");
		try {
			logWriter.write(log_entry.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e){

		}
	}

	public synchronized void log_tcp_connection_to(int id1, int id2){

		String timeStamp = new SimpleDateFormat("y-M-d 'at' h:m:s a z").format(Calendar.getInstance().getTime());

		StringBuffer log_entry = new StringBuffer();

		log_entry.append(timeStamp + ": Peer [" + id1 + "] makes a connection to Peer [" + id2 + "].");
		try {
			logWriter.write(log_entry.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e){

		}
	}

	public synchronized void log_tcp_connection_from(int id1, int id2){

		String timeStamp = new SimpleDateFormat("y-M-d 'at' h:m:s a z").format(Calendar.getInstance().getTime());

		StringBuffer log_entry = new StringBuffer();

		log_entry.append(timeStamp + ": Peer [" + id1 + "] is connected from Peer [" + id2 + "].");
		try{
			logWriter.write(log_entry.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}
	}


	public synchronized void log_change_of_preferred_neighbors(int id, int[] id_list){

		String timeStamp = new SimpleDateFormat("y-M-d 'at' h:m:s a z").format(Calendar.getInstance().getTime());

		StringBuffer log_entry = new StringBuffer();

		log_entry.append(timeStamp +": Peer [" + id + "] has the preferred neighbors [" );

		String result_string = "";
		StringBuilder sb = new StringBuilder();
		String s;
		for (int i : id_list) {
			s = Integer.toString(i);
			sb.append(s).append(",");
		}
		result_string = sb.deleteCharAt(sb.length() - 1).toString();

		log_entry.append(result_string);

		log_entry.append("].");

		try{
			logWriter.write(log_entry.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}

	}

	public synchronized void log_change_of_optimistically_unchoked_neighbor(int id1, int id2){

		String timeStamp = new SimpleDateFormat("y-M-d 'at' h:m:s a z").format(Calendar.getInstance().getTime());

		StringBuffer log_entry = new StringBuffer();

		log_entry.append(timeStamp + ": Peer [" +id1+ "] has the optimistically unchoked neighbor [" + id2 + "].");


		try{
			logWriter.write(log_entry.toString());

			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}

	}

	public synchronized void log_unchoking(int id1, int id2){

		String timeStamp = new SimpleDateFormat("y-M-d 'at' h:m:s a z").format(Calendar.getInstance().getTime());

		StringBuffer log_entry = new StringBuffer();

		log_entry.append(timeStamp + ": Peer [" + id1 + "] is unchoked by [" + id2 + "].");
		try{
			logWriter.write(log_entry.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}

	}

	public synchronized void log_choking( int id1, int id2){

		String timeStamp = new SimpleDateFormat("y-M-d 'at' h:m:s a z").format(Calendar.getInstance().getTime());

		StringBuffer log_entry = new StringBuffer();

		log_entry.append(timeStamp + ": Peer [" + id1  +"] is choked by ["+ id2 +"].");
		try{
			logWriter.write(log_entry.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}

	}

	public synchronized void log_send_have_message( int id1, int id2, int index){
		String timeStamp = new SimpleDateFormat("y-M-d 'at' h:m:s a z").format(Calendar.getInstance().getTime());
		StringBuffer log_entry = new StringBuffer();
		log_entry.append(timeStamp + ": Peer [" + id1 +"] sent 'have' message to [" + id2+ "] for the piece: " + index + ".");

		try{
			logWriter.write(log_entry.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}
	}
	
	public synchronized void log_send_request_message( int id1, int id2, int index){
		String timeStamp = new SimpleDateFormat("y-M-d 'at' h:m:s a z").format(Calendar.getInstance().getTime());
		StringBuffer log_entry = new StringBuffer();
		log_entry.append(timeStamp + ": Peer [" + id1 +"] sent 'request' message to [" + id2+ "] for the piece: " + index + ".");

		try{
			logWriter.write(log_entry.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}
	}
	
	public synchronized void log_send_piece_message( int id1, int id2, int index){
		String timeStamp = new SimpleDateFormat("y-M-d 'at' h:m:s a z").format(Calendar.getInstance().getTime());
		StringBuffer log_entry = new StringBuffer();
		log_entry.append(timeStamp + ": Peer [" + id1 +"] sent the 'piece' "+ index + " to Peer [" + id2+ "]. " );

		try{
			logWriter.write(log_entry.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}
	}
	
	public synchronized void log_send_interested_message( int id1, int id2){

		String timeStamp = new SimpleDateFormat("y-M-d 'at' h:m:s a z").format(Calendar.getInstance().getTime());

		StringBuffer log_entry = new StringBuffer();

		log_entry.append(timeStamp + ": Peer [" + id1 + "] sent 'interested' message to [" + id2 + "]." );
		try{
			logWriter.write(log_entry.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}

	}
	
	public synchronized void log_send_not_interested_message( int id1, int id2){

		String timeStamp = new SimpleDateFormat("y-M-d 'at' h:m:s a z").format(Calendar.getInstance().getTime());

		StringBuffer log_entry = new StringBuffer();

		log_entry.append(timeStamp + ": Peer [" + id1 + "] sent 'not interested' message to [" + id2 + "]." );
		try{
			logWriter.write(log_entry.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}

	}
	
	public synchronized void log_receiving_have_message( int id1, int id2, int index){
		String timeStamp = new SimpleDateFormat("y-M-d 'at' h:m:s a z").format(Calendar.getInstance().getTime());
		StringBuffer log_entry = new StringBuffer();
		log_entry.append(timeStamp + ": Peer [" + id1 +"] received 'have' message from [" + id2+ "] for the piece: " + index + ".");

		try{
			logWriter.write(log_entry.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}
	}

	public synchronized void log_receiving_interested_message( int id1, int id2){

		String timeStamp = new SimpleDateFormat("y-M-d 'at' h:m:s a z").format(Calendar.getInstance().getTime());

		StringBuffer log_entry = new StringBuffer();

		log_entry.append(timeStamp + ": Peer [" + id1 + "] received the 'interested' message from [" + id2 + "]." );
		try{
			logWriter.write(log_entry.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}

	}

	public synchronized void log_receiving_not_interested_message( int id1, int id2){

		String timeStamp = new SimpleDateFormat("y-M-d 'at' h:m:s a z").format(Calendar.getInstance().getTime());

		StringBuffer log_entry = new StringBuffer();

		log_entry.append(timeStamp + ": Peer [" +id1 + "] received the 'not interested' message from [" + id2 + "]." );

		try{
			logWriter.write(log_entry.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}

	}
	
	public synchronized void log_receiving_request_message( int id1, int id2,int index){

		String timeStamp = new SimpleDateFormat("y-M-d 'at' h:m:s a z").format(Calendar.getInstance().getTime());

		StringBuffer log_entry = new StringBuffer();

		log_entry.append(timeStamp + ": Peer [" +id1 + "] received the 'request' message from [" + id2 + "] for the piece " + index +" ." );

		try{
			logWriter.write(log_entry.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}

	}


	public synchronized void log_downloading_a_piece( int id1, int id2, int index, int number_of_pieces){

		String timeStamp = new SimpleDateFormat("y-M-d 'at' h:m:s a z").format(Calendar.getInstance().getTime());

		StringBuffer log_entry = new StringBuffer();

		log_entry.append(timeStamp + ": Peer [" + id1 + "] has downloaded the piece " + index + " from [" + id2 + "]. " +"Now the number of pieces it has is : "+ number_of_pieces + ".");

		try{
			logWriter.write(log_entry.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}

	}


	public synchronized void log_completion_of_download(int id){

		String timeStamp = new SimpleDateFormat("y-M-d 'at' h:m:s a z").format(Calendar.getInstance().getTime());

		StringBuffer log_entry = new StringBuffer();

		log_entry.append(timeStamp + ": Peer [" + id + "] has downloaded the complete file.");

		try{
			logWriter.write(log_entry.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}
	}
	
	public synchronized void log_completion_of_process(){

		String timeStamp = new SimpleDateFormat("y-M-d 'at' h:m:s a z").format(Calendar.getInstance().getTime());

		StringBuffer log_entry = new StringBuffer();

		log_entry.append(timeStamp + ": All peers have finished downloading. So stopping the service");

		try{
			logWriter.write(log_entry.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}
	}
	

}