import flanagan.interpolation.CubicSpline;

import java.awt.geom.Point2D;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CSVReaderInterpolationEmre {

    public static void main(String[] args) {

        String line = "";
        String cvsSplitBy = ",";
		String outputPath = "output_original_interpolated/";
        
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
		            }


					// ----------------------------------
					// Interpolation (and extrapolation)
					// ----------------------------------

					double[] averages = new double[highest + lowest];
					var interpolationDic = new HashMap<String, PointList>();

					for (var entry : dictionary.entrySet()) {
						if (!interpolationDic.containsKey(entry.getKey()))
							interpolationDic.put(entry.getKey(), new PointList());
						if (!interpolationDic.containsKey(entry.getKey() + 1))
							interpolationDic.put(entry.getKey() + 1, new PointList());
						if (!interpolationDic.containsKey(entry.getKey() + 2))
							interpolationDic.put(entry.getKey() + 2, new PointList());
						if (!interpolationDic.containsKey(entry.getKey() + 3))
							interpolationDic.put(entry.getKey() + 3, new PointList());
						if (!interpolationDic.containsKey(entry.getKey() + 4))
							interpolationDic.put(entry.getKey() + 4, new PointList());
						if (!interpolationDic.containsKey(entry.getKey() + 5))
							interpolationDic.put(entry.getKey() + 5, new PointList());
					}

					for (int i = lowest; i <= highest; i++) {

						double average = 0;
						int averageN = 0;

						// calculate the average
						for (var entry : dictionary.entrySet()) {
							if (entry.getValue().containsKey(i - 1) &&
									!entry.getValue().get(i - 1)[6].equals("")) {
								average += Double.parseDouble(entry.getValue().get(i - 1)[6]);
								averageN++;
							}

							if (entry.getValue().containsKey(i - 2) &&
									!entry.getValue().get(i - 2)[7].equals("")) {
								average += Double.parseDouble(entry.getValue().get(i - 2)[7]);
								averageN++;
							}

							if (entry.getValue().containsKey(i - 3) &&
									!entry.getValue().get(i - 3)[8].equals("")) {
								average += Double.parseDouble(entry.getValue().get(i - 3)[8]);
								averageN++;
							}

							if (entry.getValue().containsKey(i - 4) &&
									!entry.getValue().get(i - 4)[9].equals("")) {
								average += Double.parseDouble(entry.getValue().get(i - 4)[9]);
								averageN++;
							}

							if (entry.getValue().containsKey(i - 5) &&
									!entry.getValue().get(i - 5)[10].equals("")) {
								average += Double.parseDouble(entry.getValue().get(i - 5)[10]);
								averageN++;
							}
						}

						// divide by instances
						averages[i] = average = average / averageN;

						for (var entry : dictionary.entrySet()) {
							if (entry.getValue().containsKey(i - 1) &&
									!entry.getValue().get(i - 1)[6].equals("")) {
								interpolationDic.get(entry.getKey())
										.add(new Point2D.Double(i, Double.parseDouble(entry.getValue().get(i - 1)[6]) - average));
							}

							if (entry.getValue().containsKey(i - 2) &&
									!entry.getValue().get(i - 2)[7].equals("")) {
								interpolationDic.get(entry.getKey() + 1)
										.add(new Point2D.Double(i, Double.parseDouble(entry.getValue().get(i - 2)[7]) - average));
							}

							if (entry.getValue().containsKey(i - 3) &&
									!entry.getValue().get(i - 3)[8].equals("")) {
								interpolationDic.get(entry.getKey() + 2)
										.add(new Point2D.Double(i, Double.parseDouble(entry.getValue().get(i - 3)[8]) - average));
							}

							if (entry.getValue().containsKey(i - 4) &&
									!entry.getValue().get(i - 4)[9].equals("")) {
								interpolationDic.get(entry.getKey() + 3)
										.add(new Point2D.Double(i, Double.parseDouble(entry.getValue().get(i - 4)[9]) - average));
							}

							if (entry.getValue().containsKey(i - 5) &&
									!entry.getValue().get(i - 5)[10].equals("")) {
								interpolationDic.get(entry.getKey() + 4)
										.add(new Point2D.Double(i, Double.parseDouble(entry.getValue().get(i - 5)[10]) - average));
							}
						}
					}

					// ----------------------------------
					// Writing output
					// ----------------------------------

					var output = new StringBuilder();
		            
		    		output.append("Old_Id," +
							"YEAR," +
							"QUARTER," +
							"ID," +
							"NDUSTRY," +
							"Previous_Quarter," +
							"End_of_Current_Quarter," +
							"Next_Quarter," +
							"Next_Quarter+1," +
							"Next_Quarter+2," +
							"Next_Quarter+3," +
							"Average_over_Current_year," +
							"Average_over_Next_year," +
							"Series," +
							"Latest_available_end_of_Current," +
							"Latest_available_Year," +
							"Latest_available_Quarter," +
							"Earliest_available_end_of_Current," +
							"Earliest_available_Year," +
							"Earliest_available_Quarter");
		    		output.append("\n");

		            for (int i = lowest; i <= highest; i++) {
		            	
		            	for (var entry : dictionary.entrySet()) {
							output.append(entry.getValue().entrySet().iterator().next().getValue()[0]).append(",");
							output.append(i / 4).append(",");
							output.append(i % 4 + 1).append(",");
							output.append(entry.getKey()).append(",");

							if (entry.getValue().containsKey(i)) {
								output.append(entry.getValue().get(i)[4]).append(",");
								output.append(entry.getValue().get(i)[5]).append(",");
								output.append(entry.getValue().get(i)[6]).append(",");
								output.append(entry.getValue().get(i)[7]).append(",");
								output.append(entry.getValue().get(i)[8]).append(",");
								output.append(entry.getValue().get(i)[9]).append(",");
								output.append(entry.getValue().get(i)[10]).append(",");
								output.append(entry.getValue().get(i)[11]).append(",");
								output.append(entry.getValue().get(i)[12]).append(",");
								output.append(entry.getValue().get(i)[13]).append(",");
								output.append(entry.getValue().get(i)[14]).append(",");
								output.append(entry.getValue().get(i)[15]).append(",");
								output.append(entry.getValue().get(i)[16]).append(",");
								output.append(entry.getValue().get(i)[17]).append(",");
								output.append(entry.getValue().get(i)[18]).append(",");
								output.append(entry.getValue().get(i)[19]);
							} else {
								output.append(",");
								addInterpolate(entry, i, output, interpolationDic, averages, 1);
								addInterpolate(entry, i, output, interpolationDic, averages, 2);
								addInterpolate(entry, i, output, interpolationDic, averages, 3);
								addInterpolate(entry, i, output, interpolationDic, averages, 4);
//								addInterpolate(entry, i, output, interpolationDic, averages, 5);
							}

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

    public static void addInterpolate(Map.Entry<String, HashMap<Integer, String[]>> entry, int timestep, StringBuilder output, HashMap<String, PointList> dic, double[] averages, int offset) {
		var points = dic.get(entry.getKey() + offset);

		CubicSpline spline = null;
		if (points.size() >= 3)
			spline = new CubicSpline(points.getX(), points.getY());

		if (spline != null && timestep > spline.getXmin() && timestep < spline.getXmax())
			output.append(spline.interpolate(timestep) + averages[timestep]);
		else // TODO add bias
			output.append(averages[timestep + 1]);
		output.append(",");
	}
}
