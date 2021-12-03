import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ReadFiles {

	private ReadFiles() {}

	public static ReadFiles getReadFilesObj() {
		return new ReadFiles();
	}

	public List<String> parseTheFile(String path){
		List<String> fileContent = new ArrayList<>();
		if (path != null) {

			String dir = System.getProperty("user.dir")+File.separator+path;
			//System.out.println(dir);
			try {
				FileReader fr = new FileReader(dir);
				BufferedReader br = new BufferedReader(fr);
				String line = "";
				while((line =br.readLine()) != null) {
					fileContent.add(line);
				}
				br.close();
			}catch(IOException e) {
				e.printStackTrace();
			}

		}	
		return fileContent;
	}
}
