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
		String outputPath = "output_original_interpolated_emre/";
        
        int lowest = Integer.MAX_VALUE;
        int highest = 0;
        
		if (!new File(outputPath).exists())
			new File(outputPath).mkdirs();
        
        File folder = new File("input");
		File[] listOfFiles = folder.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				if (name.contains(".csv"))
					return true;
					
				return false;
			}
		});

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
						if (!interpolationDic.containsKey(entry.getKey() + 0))
							interpolationDic.put(entry.getKey() + 0, new PointList());
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
						if (!interpolationDic.containsKey(entry.getKey() + 6))
							interpolationDic.put(entry.getKey() + 6, new PointList());
						if (!interpolationDic.containsKey(entry.getKey() + 7))
							interpolationDic.put(entry.getKey() + 7, new PointList());
					}

					for (int i = lowest; i <= highest; i++) {

						double average = 0;
						int averageN = 0;

						// calculate the average
						for (var entry : dictionary.entrySet()) {
							if (entry.getValue().containsKey(i) &&
									!entry.getValue().get(i)[5].equals("")) {
								average += Double.parseDouble(entry.getValue().get(i)[5]);
								averageN++;
							}
							
							if (entry.getValue().containsKey(i) &&
									!entry.getValue().get(i)[6].equals("")) {
								average += Double.parseDouble(entry.getValue().get(i)[6]);
								averageN++;
							}

							if (entry.getValue().containsKey(i) &&
									!entry.getValue().get(i)[7].equals("")) {
								average += Double.parseDouble(entry.getValue().get(i)[7]);
								averageN++;
							}

							if (entry.getValue().containsKey(i) &&
									!entry.getValue().get(i)[8].equals("")) {
								average += Double.parseDouble(entry.getValue().get(i)[8]);
								averageN++;
							}

							if (entry.getValue().containsKey(i) &&
									!entry.getValue().get(i)[9].equals("")) {
								average += Double.parseDouble(entry.getValue().get(i)[9]);
								averageN++;
							}

							if (entry.getValue().containsKey(i) &&
									!entry.getValue().get(i)[10].equals("")) {
								average += Double.parseDouble(entry.getValue().get(i)[10]);
								averageN++;
							}
						}

						// divide by instances
						averages[i] = average = average / averageN;

						for (var entry : dictionary.entrySet()) {
							if (entry.getValue().containsKey(i) &&
									!entry.getValue().get(i)[5].equals("")) {
								interpolationDic.get(entry.getKey() + 0)
										.add(new Point2D.Double(i, Double.parseDouble(entry.getValue().get(i)[5]) / average));
							}
							
							if (entry.getValue().containsKey(i) &&
									!entry.getValue().get(i)[6].equals("")) {
								interpolationDic.get(entry.getKey() + 1)
										.add(new Point2D.Double(i, Double.parseDouble(entry.getValue().get(i)[6]) / average));
							}

							if (entry.getValue().containsKey(i) &&
									!entry.getValue().get(i)[7].equals("")) {
								interpolationDic.get(entry.getKey() + 2)
										.add(new Point2D.Double(i, Double.parseDouble(entry.getValue().get(i)[7]) / average));
							}

							if (entry.getValue().containsKey(i) &&
									!entry.getValue().get(i)[8].equals("")) {
								interpolationDic.get(entry.getKey() + 3)
										.add(new Point2D.Double(i, Double.parseDouble(entry.getValue().get(i)[8]) / average));
							}

							if (entry.getValue().containsKey(i) &&
									!entry.getValue().get(i)[9].equals("")) {
								interpolationDic.get(entry.getKey() + 4)
										.add(new Point2D.Double(i, Double.parseDouble(entry.getValue().get(i)[9]) / average));
							}

							if (entry.getValue().containsKey(i) &&
									!entry.getValue().get(i)[10].equals("")) {
								interpolationDic.get(entry.getKey() + 5)
										.add(new Point2D.Double(i, Double.parseDouble(entry.getValue().get(i)[10]) / average));
							}
							
							if (entry.getValue().containsKey(i) &&
									!entry.getValue().get(i)[11].equals("")) {
								interpolationDic.get(entry.getKey() + 6)
										.add(new Point2D.Double(i, Double.parseDouble(entry.getValue().get(i)[11]) / average));
							}
							
							if (entry.getValue().containsKey(i) &&
									!entry.getValue().get(i)[12].equals("")) {
								interpolationDic.get(entry.getKey() + 7)
										.add(new Point2D.Double(i, Double.parseDouble(entry.getValue().get(i)[12]) / average));
							}
						}
					}

					// ----------------------------------
					// Writing output
					// ----------------------------------

					var output = new StringBuilder();
					
		    		output.append("YEAR,");
		    		output.append("QUARTER,");
		    		output.append("ID,");
		    		output.append("INDUSTRY,");
		    		output.append(listOfFiles[j].getName() + "1,");
		    		output.append(listOfFiles[j].getName() + "2,");
		    		output.append(listOfFiles[j].getName() + "3,");
		    		output.append(listOfFiles[j].getName() + "4,");
		    		output.append(listOfFiles[j].getName() + "5,");
		    		output.append(listOfFiles[j].getName() + "6,");
		    		output.append(listOfFiles[j].getName() + "GA,");
		    		output.append(listOfFiles[j].getName() + "GB,");
		    		output.append("\n");

		            for (int i = lowest; i <= highest; i++) {
		            	
		            	for (var entry : dictionary.entrySet()) {
							if (!entry.getValue().containsKey(i) &&
								!(interpolate(entry, i, interpolationDic, averages, 0) < Double.MAX_VALUE &&
								interpolate(entry, i, interpolationDic, averages, 1) < Double.MAX_VALUE &&
								interpolate(entry, i, interpolationDic, averages, 2) < Double.MAX_VALUE &&
								interpolate(entry, i, interpolationDic, averages, 3) < Double.MAX_VALUE &&
								interpolate(entry, i, interpolationDic, averages, 4) < Double.MAX_VALUE &&
								interpolate(entry, i, interpolationDic, averages, 5) < Double.MAX_VALUE &&
								interpolate(entry, i, interpolationDic, averages, 6) < Double.MAX_VALUE &&
								interpolate(entry, i, interpolationDic, averages, 7) < Double.MAX_VALUE))
								continue;
		            		
							output.append(i / 4).append(",");
							output.append(i % 4 + 1).append(",");
							output.append(entry.getKey()).append(",");
							output.append(entry.getValue().entrySet().iterator().next().getValue()[4]).append(",");

							if (entry.getValue().containsKey(i)) {
								output.append(entry.getValue().get(i)[5]).append(",");
								output.append(entry.getValue().get(i)[6]).append(",");
								output.append(entry.getValue().get(i)[7]).append(",");
								output.append(entry.getValue().get(i)[8]).append(",");
								output.append(entry.getValue().get(i)[9]).append(",");
								output.append(entry.getValue().get(i)[10]).append(",");
								output.append(entry.getValue().get(i)[11]).append(",");
								output.append(entry.getValue().get(i)[12]).append(",");
							} else {
								if (interpolate(entry, i, interpolationDic, averages, 0) < Double.MAX_VALUE)
									output.append(interpolate(entry, i, interpolationDic, averages, 0));
								output.append(",");
								
								if (interpolate(entry, i, interpolationDic, averages, 1) < Double.MAX_VALUE)
									output.append(interpolate(entry, i, interpolationDic, averages, 1));
								output.append(",");
								
								if (interpolate(entry, i, interpolationDic, averages, 2) < Double.MAX_VALUE)
									output.append(interpolate(entry, i, interpolationDic, averages, 2));
								output.append(",");
								
								if (interpolate(entry, i, interpolationDic, averages, 3) < Double.MAX_VALUE)
									output.append(interpolate(entry, i, interpolationDic, averages, 3));
								output.append(",");
								
								if (interpolate(entry, i, interpolationDic, averages, 4) < Double.MAX_VALUE)
									output.append(interpolate(entry, i, interpolationDic, averages, 4));
								output.append(",");
								
								if (interpolate(entry, i, interpolationDic, averages, 5) < Double.MAX_VALUE)
									output.append(interpolate(entry, i, interpolationDic, averages, 5));
								output.append(",");
								
								if (interpolate(entry, i, interpolationDic, averages, 6) < Double.MAX_VALUE)
									output.append(interpolate(entry, i, interpolationDic, averages, 6));
								output.append(",");
								
								if (interpolate(entry, i, interpolationDic, averages, 7) < Double.MAX_VALUE)
									output.append(interpolate(entry, i, interpolationDic, averages, 7));
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

    public static double interpolate(Map.Entry<String, HashMap<Integer, String[]>> entry, int timestep, HashMap<String, PointList> dic, double[] averages, int offset) {
    	var points = dic.get(entry.getKey() + offset);

		CubicSpline spline = null;
		if (points.size() >= 3)
			spline = new CubicSpline(points.getX(), points.getY());

		
		if (averages[timestep] == Double.NaN)
			return Double.NaN;
		else if (spline != null && timestep > spline.getXmin() && timestep < spline.getXmax() && spline.interpolate(timestep) != Double.NaN)
			return spline.interpolate(timestep) * averages[timestep];
		else
			return points.average() * averages[timestep];
	}
}
