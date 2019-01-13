
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;

import flanagan.interpolation.CubicSpline;

public class CSVWriterInterpolation {

	public static void main(String[] args) {

		String line = "";
		String cvsSplitBy = ",";
		String outputPath = "output_interpolation/";

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

					// build model
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

						// write down with the key being predictor id
						dictionary.get(prediction[3]).put(timeStamp, prediction);
					}

					var output = new StringBuilder();

					output.append("\"Quarter Timestamp\",");

					var removalDic = new HashMap<String, boolean[]>();

					for (var entry : dictionary.entrySet()) {
						var hasInfo = new boolean[5];

						// removal of empty columns
						for (int i = lowest + 5; i <= highest; i++) {
							if (entry.getValue().containsKey(i - 1) && !entry.getValue().get(i - 1)[6].isEmpty())
								hasInfo[0] = true;
							if (entry.getValue().containsKey(i - 2) && !entry.getValue().get(i - 2)[7].isEmpty())
								hasInfo[1] = true;
							if (entry.getValue().containsKey(i - 3) && !entry.getValue().get(i - 3)[8].isEmpty())
								hasInfo[2] = true;
							if (entry.getValue().containsKey(i - 4) && !entry.getValue().get(i - 4)[9].isEmpty())
								hasInfo[3] = true;
							if (entry.getValue().containsKey(i - 5) && !entry.getValue().get(i - 5)[10].isEmpty())
								hasInfo[4] = true;
						}

						if (hasInfo[0])
							output.append("\"Estimated This Quarter" + entry.getKey() + "\",");
						if (hasInfo[1])
							output.append("\"Estimated Last Quarter" + entry.getKey() + "\",");
						if (hasInfo[2])
							output.append("\"Estimated Half Year Ago" + entry.getKey() + "\",");
						if (hasInfo[3])
							output.append("\"Estimated Three Quarters Ago" + entry.getKey() + "\",");
						if (hasInfo[4])
							output.append("\"Estimated One Year Ago" + entry.getKey() + "\",");

						removalDic.put(entry.getKey(), hasInfo);
					}

					output.append("\"Average\",");
					output.append("\"Ground Truth\"");
					output.append("\r\n");
					
					// ----------------------------------
					// Interpolation (and extrapolation)
					// ----------------------------------
					
					double[] averages = new double[highest + lowest];
					var interpolationDic = new HashMap<String, PointList>();
					
					for (int i = lowest + 5; i <= highest; i++) {
						
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
							// TODO move this part
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
					
					for (int i = lowest + 5; i <= highest; i++) {

						var truthList = new ArrayList<String>();

						// skipping condition
						for (var entry : dictionary.entrySet()) {
							if (entry.getValue().containsKey(i))
								truthList.add(entry.getValue().get(i)[5]);
						}

						var commonTruth = mostCommon(truthList);

						if (commonTruth == null)
							continue;
						
						output.append(i).append(",");

						for (var entry : dictionary.entrySet()) {
							var removed = removalDic.get(entry.getKey());

							if (removed[0]) {
								if (entry.getValue().containsKey(i - 1))
									output.append(entry.getValue().get(i - 1)[6]);
								else {
									var points = interpolationDic.get(entry.getKey());

									CubicSpline spline = null;
									if (points.size() >= 3)
										spline = new CubicSpline(points.getX(), points.getY());
									
									if (spline != null && i > spline.getXmin() && i < spline.getXmax())
										output.append(spline.interpolate(i) + averages[i]);
									else
										output.append(points.average() + averages[i]);
								}
								output.append(",");
							}

							if (removed[1]) {
								if (entry.getValue().containsKey(i - 2))
									output.append(entry.getValue().get(i - 2)[7]);
								else {
									var points = interpolationDic.get(entry.getKey() + 1);

									CubicSpline spline = null;
									if (points.size() >= 3)
										spline = new CubicSpline(points.getX(), points.getY());
									
									if (spline != null && i > spline.getXmin() && i < spline.getXmax())
										output.append(spline.interpolate(i) + averages[i]);
									else
										output.append(points.average() + averages[i]);
								}
								output.append(",");
							}

							if (removed[2]) {
								if (entry.getValue().containsKey(i - 3))
									output.append(entry.getValue().get(i - 3)[8]);
								else {
									var points = interpolationDic.get(entry.getKey() + 2);

									CubicSpline spline = null;
									if (points.size() >= 3)
										spline = new CubicSpline(points.getX(), points.getY());
									
									if (spline != null && i > spline.getXmin() && i < spline.getXmax())
										output.append(spline.interpolate(i) + averages[i]);
									else
										output.append(points.average() + averages[i]);
								}
								output.append(",");
							}

							if (removed[3]) {
								if (entry.getValue().containsKey(i - 4))
									output.append(entry.getValue().get(i - 4)[9]);
								else {
									var points = interpolationDic.get(entry.getKey() + 3);

									CubicSpline spline = null;
									if (points.size() >= 3)
										spline = new CubicSpline(points.getX(), points.getY());
									
									if (spline != null && i > spline.getXmin() && i < spline.getXmax())
										output.append(spline.interpolate(i) + averages[i]);
									else
										output.append(points.average() + averages[i]);
								}
								output.append(",");
							}

							if (removed[4]) {
								if (entry.getValue().containsKey(i - 5))
									output.append(entry.getValue().get(i - 5)[10]);
								else {
									var points = interpolationDic.get(entry.getKey() + 4);

									CubicSpline spline = null;
									if (points.size() >= 3)
										spline = new CubicSpline(points.getX(), points.getY());
									
									if (spline != null && i > spline.getXmin() && i < spline.getXmax())
										output.append(spline.interpolate(i) + averages[i]);
									else if (points.average() != Double.NaN)
										output.append(points.average() + averages[i]);
								}
								output.append(",");
							}
						}

						output.append(averages[i]).append(",");;
						output.append(commonTruth).append("\r\n");
					}

//		            System.out.println(output);
					var filePath = outputPath + listOfFiles[j].getName();
					System.out.println(filePath);
//					BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8));
		            
					writer.write(output.toString());
					writer.close();

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static <T> T mostCommon(List<T> list) {
		Map<T, Integer> map = new HashMap<>();

		if (list.isEmpty())
			return null;

		for (T t : list) {
			Integer val = map.get(t);
			map.put(t, val == null ? 1 : val + 1);
		}

		Entry<T, Integer> max = null;

		for (Entry<T, Integer> e : map.entrySet()) {
			if (max == null || e.getValue() > max.getValue())
				max = e;
		}

		return max.getKey();
	}
}
