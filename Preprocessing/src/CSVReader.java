
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class CSVReader {

    public static void main(String[] args) {

        String line = "";
        String cvsSplitBy = ",";
		String outputPath = "output_original/";
        
        int lowest = Integer.MAX_VALUE;
        int highest = 0;
        
		if (!new File(outputPath).exists())
			new File(outputPath).mkdirs();
        
        File folder = new File("input");
        File[] listOfFiles = folder.listFiles();

		for (int j = 0; j < listOfFiles.length; j++) {
			if (listOfFiles[j].isFile()) {
				System.out.println("File " + listOfFiles[j].getName());
		        try (BufferedReader br = new BufferedReader(new FileReader(listOfFiles[j]))) {

		            var dictionary = new HashMap<String, HashMap<Integer, String[]>>();
		            
		        	// skip first
		            br.readLine();

		            while ((line = br.readLine()) != null) {

		                // use comma as separator
		                String[] prediction = line.split(cvsSplitBy);

		                
		                if (!dictionary.containsKey(prediction[3]))
		                	dictionary.put(prediction[3], new HashMap<Integer, String[]>());
		                
		                int timeStamp = Integer.parseInt(prediction[1]) * 4 + Integer.parseInt(prediction[2]) - 1;
		                if (timeStamp < lowest)
		                	lowest = timeStamp;
		                if (timeStamp > highest)
		                	highest = timeStamp;
		                dictionary.get(prediction[3]).put(timeStamp, prediction);
		                
//		                System.out.println("Measurement [companyId= " + prediction[3] + " , key=" + timeStamp + "]");
		            }

		            var output = new StringBuilder();
		            
		    		output.append("CompanyId,");
		    		output.append("Quarter Timestamp,");
		    		output.append("Estimated This Quarter,");
		    		output.append("Estimated Last Quarter,");
		    		output.append("Estimated Half Year Ago,");
		    		output.append("Estimated Three Quarters Ago,");
		    		output.append("Estimated One Year Ago,");
		    		output.append("Ground Truth");
		    		output.append("\n");
		    		
		            for (int i = lowest + 5; i <= highest; i++) {
		            	
		            	for (var entry : dictionary.entrySet()) {
		            		
		            		// skipping condition
		            		// TODO fix ground truth
		            		if (!entry.getValue().containsKey(i) ||
		            			(!entry.getValue().containsKey(i - 1) &&
		            			!entry.getValue().containsKey(i - 2) &&
		            			!entry.getValue().containsKey(i - 3) &&
		            			!entry.getValue().containsKey(i - 4) &&
		            			!entry.getValue().containsKey(i - 5)))
		            			continue;
		            		
		            		output.append(entry.getKey()).append(",");
		            		output.append(i).append(",");
		            		
		            		if (entry.getValue().containsKey(i - 1))
		            			output.append(entry.getValue().get(i - 1)[6]);
		            		output.append(",");

		            		if (entry.getValue().containsKey(i - 2))
		            			output.append(entry.getValue().get(i - 2)[7]);
		            		output.append(",");

		            		if (entry.getValue().containsKey(i - 3))
		            			output.append(entry.getValue().get(i - 3)[8]);
		            		output.append(",");

		            		if (entry.getValue().containsKey(i - 4))
		            			output.append(entry.getValue().get(i - 4)[9]);
		            		output.append(",");

		            		if (entry.getValue().containsKey(i - 5))
		            			output.append(entry.getValue().get(i - 5)[10]);
		            		output.append(",");
			            	
		            		if (entry.getValue().containsKey(i))
		            			output.append(entry.getValue().get(i)[5]);
		            		
		            		output.append("\n");
						}
		            }
		            
//		            System.out.println(output);
					var filePath = outputPath + listOfFiles[j].getName();
					System.out.println(filePath);
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8));
		            
		            writer.write(output.toString());
		            writer.close();
		            
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
			}
		}
    }

    
    
}
