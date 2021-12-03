import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Logs{
	BufferedWriter logWriter = null;
	File log = null;
	public Logs() {}

	public Logs(File logFile) {
		try {
			this.log = logFile;
			logWriter = new BufferedWriter(new FileWriter(log.getAbsolutePath(),true));
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public synchronized void log_readCommonFile(int id1, CommonConfig cfg) {
		String timeStamp = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(Calendar.getInstance().getTime());
		StringBuffer write_log = new StringBuffer();
		write_log.append(timeStamp +" "+id1+" has read Common.cfg file.");
		try {
			logWriter.write(write_log.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
			
		}
	}
	
	public synchronized void log_bitfield_received(int id1, int id2) {
		String timeStamp = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(Calendar.getInstance().getTime());
		StringBuffer write_log = new StringBuffer();
		write_log.append(timeStamp + " " + id1 + " has received bitfield from " + id2 );
		try {
			logWriter.write(write_log.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e){

		}
	}
	
	public synchronized void log_bitfield_sent(int id1, int id2) {
		String timeStamp = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(Calendar.getInstance().getTime());
		StringBuffer write_log = new StringBuffer();
		write_log.append(timeStamp + " " + id1 + " sent bitfield to " + id2 );
		try {
			logWriter.write(write_log.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e){

		}
	}

	public synchronized void log_tcp_connection_to(int id1, int id2){

		String timeStamp = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(Calendar.getInstance().getTime());

		StringBuffer write_log = new StringBuffer();

		write_log.append(timeStamp + " " + id1 + " is now connected to " + id2 );
		try {
			logWriter.write(write_log.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e){

		}
	}

	public synchronized void log_tcp_connection_from(int id1, int id2){

		String timeStamp = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(Calendar.getInstance().getTime());

		StringBuffer write_log = new StringBuffer();

		write_log.append(timeStamp + " " + id1 + "is now connected to " + id2 );
		try{
			logWriter.write(write_log.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}
	}


	public synchronized void log_change_of_preferred_neighbors(int id1, int[] id_list){

		String timeStamp = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(Calendar.getInstance().getTime());

		StringBuffer write_log = new StringBuffer();

		write_log.append(timeStamp +" " + id1 + " has the preferred neighbors" );

		String result_string = "";
		StringBuilder sb = new StringBuilder();
		String s;
		for (int i : id_list) {
			s = Integer.toString(i);
			sb.append(s).append(",");
		}
		result_string = sb.deleteCharAt(sb.length() - 1).toString();

		write_log.append(result_string);

		write_log.append("].");

		try{
			logWriter.write(write_log.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}

	}

	public synchronized void log_change_of_optimistically_unchoked_neighbor(int id1, int id2){

		String timeStamp = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(Calendar.getInstance().getTime());

		StringBuffer write_log = new StringBuffer();

		write_log.append(timeStamp + " " +id1+ " has the optimistically unchoked neighbor " + id2);


		try{
			logWriter.write(write_log.toString());

			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}

	}

	public synchronized void log_unchoking(int id1, int id2){

		String timeStamp = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(Calendar.getInstance().getTime());

		StringBuffer write_log = new StringBuffer();

		write_log.append(timeStamp + " " + id1 + " is now unchoked by " + id2);
		try{
			logWriter.write(write_log.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}

	}

	public synchronized void log_choking( int id1, int id2){

		String timeStamp = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(Calendar.getInstance().getTime());

		StringBuffer write_log = new StringBuffer();

		write_log.append(timeStamp + " " + id1  +" is now choked by "+ id2 );
		try{
			logWriter.write(write_log.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}

	}

	public synchronized void log_send_have_message( int id1, int id2, int index){
		String timeStamp = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(Calendar.getInstance().getTime());
		StringBuffer write_log = new StringBuffer();
		write_log.append(timeStamp + " " + id1 +" sent 'have' message to " + id2+ " for the piece - " + index);

		try{
			logWriter.write(write_log.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}
	}
	
	public synchronized void log_send_request_message( int id1, int id2, int index){
		String timeStamp = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(Calendar.getInstance().getTime());
		StringBuffer write_log = new StringBuffer();
		write_log.append(timeStamp + " " + id1 +" sent 'request' message to " + id2+ " for the piece - " + index);

		try{
			logWriter.write(write_log.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}
	}
	
	public synchronized void log_send_piece_message( int id1, int id2, int index){
		String timeStamp = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(Calendar.getInstance().getTime());
		StringBuffer write_log = new StringBuffer();
		write_log.append(timeStamp + " " + id1 +" sent the 'piece' "+ index + " to " + id2);

		try{
			logWriter.write(write_log.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}
	}
	
	public synchronized void log_send_interested_message( int id1, int id2){

		String timeStamp = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(Calendar.getInstance().getTime());

		StringBuffer write_log = new StringBuffer();

		write_log.append(timeStamp + " " + id1 + " sent 'interested' message to " + id2 );
		try{
			logWriter.write(write_log.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}

	}
	
	public synchronized void log_send_not_interested_message( int id1, int id2){

		String timeStamp = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(Calendar.getInstance().getTime());

		StringBuffer write_log = new StringBuffer();

		write_log.append(timeStamp + " " + id1 + " sent 'not interested' message to " + id2);
		try{
			logWriter.write(write_log.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}

	}
	
	public synchronized void log_receiving_have_message( int id1, int id2, int index){
		String timeStamp = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(Calendar.getInstance().getTime());
		StringBuffer write_log = new StringBuffer();
		write_log.append(timeStamp + " " + id1 +" received 'have' message from " + id2+ " for the piece: " + index);

		try{
			logWriter.write(write_log.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}
	}

	public synchronized void log_receiving_interested_message( int id1, int id2){

		String timeStamp = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(Calendar.getInstance().getTime());

		StringBuffer write_log = new StringBuffer();

		write_log.append(timeStamp + " " + id1 + " received the 'interested' message from " + id2);
		try{
			logWriter.write(write_log.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}

	}

	public synchronized void log_receiving_not_interested_message( int id1, int id2){

		String timeStamp = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(Calendar.getInstance().getTime());

		StringBuffer write_log = new StringBuffer();

		write_log.append(timeStamp + " " +id1 + "received the 'not interested' message from " + id2);

		try{
			logWriter.write(write_log.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}

	}
	
	public synchronized void log_receiving_request_message( int id1, int id2,int index){

		String timeStamp = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(Calendar.getInstance().getTime());

		StringBuffer write_log = new StringBuffer();

		write_log.append(timeStamp + " " +id1 + "received the 'request' message from" + id2 + "for the piece " + index);

		try{
			logWriter.write(write_log.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}

	}


	public synchronized void log_downloading_a_piece( int id1, int id2, int index, int number_of_pieces){

		String timeStamp = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(Calendar.getInstance().getTime());

		StringBuffer write_log = new StringBuffer();

		write_log.append(timeStamp + " " + id1 + " has downloaded the piece " + index + " from " + id2 +"Now the number of pieces it has is : "+ number_of_pieces);

		try{
			logWriter.write(write_log.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}

	}


	public synchronized void log_completion_of_download(int id){

		String timeStamp = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(Calendar.getInstance().getTime());

		StringBuffer write_log = new StringBuffer();

		write_log.append(timeStamp + " " + id + " has downloaded the complete file.");

		try{
			logWriter.write(write_log.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}
	}
	
	public synchronized void log_completion_of_process(){

		String timeStamp = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(Calendar.getInstance().getTime());

		StringBuffer write_log = new StringBuffer();

		write_log.append(timeStamp + ": All peers now have the file. Stopping the service");

		try{
			logWriter.write(write_log.toString());
			logWriter.newLine();
			logWriter.flush();
		}catch(Exception e) {
		}
	}
	

}
