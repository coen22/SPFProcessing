
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class CSVWriter2 {

	public static void main(String[] args) {

		String line = "";
		String cvsSplitBy = ",";
		String outputPath = "output/";

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

					output.append("Quarter Timestamp,");

					var removalDic = new HashMap<String, boolean[]>();

					for (var entry : dictionary.entrySet()) {
						var hasInfo = new boolean[5];

						// removal of empty columns
						for (int i = lowest + 5; i <= highest; i++) {
							if (entry.getValue().containsKey(i - 1))
								hasInfo[0] = true;
							if (entry.getValue().containsKey(i - 2))
								hasInfo[1] = true;
							if (entry.getValue().containsKey(i - 3))
								hasInfo[2] = true;
							if (entry.getValue().containsKey(i - 4))
								hasInfo[3] = true;
							if (entry.getValue().containsKey(i - 5))
								hasInfo[4] = true;
						}

						if (hasInfo[0])
							output.append("Estimated This Quarter" + entry.getKey() + ",");
						if (hasInfo[1])
							output.append("Estimated Last Quarter" + entry.getKey() + ",");
						if (hasInfo[2])
							output.append("Estimated Half Year Ago" + entry.getKey() + ",");
						if (hasInfo[3])
							output.append("Estimated Three Quarters Ago" + entry.getKey() + ",");
						if (hasInfo[4])
							output.append("Estimated One Year Ago" + entry.getKey() + ",");

						removalDic.put(entry.getKey(), hasInfo);
					}

					output.append("Ground Truth");
					output.append("\n");

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
								output.append(",");
							}

							if (removed[1]) {
								if (entry.getValue().containsKey(i - 2))
									output.append(entry.getValue().get(i - 2)[7]);
								output.append(",");
							}

							if (removed[2]) {
								if (entry.getValue().containsKey(i - 3))
									output.append(entry.getValue().get(i - 3)[8]);
								output.append(",");
							}

							if (removed[3]) {
								if (entry.getValue().containsKey(i - 4))
									output.append(entry.getValue().get(i - 4)[9]);
								output.append(",");
							}

							if (removed[4]) {
								if (entry.getValue().containsKey(i - 5))
									output.append(entry.getValue().get(i - 5)[10]);
								output.append(",");
							}
						}

						output.append(commonTruth);
						output.append("\n");
					}

//		            System.out.println(output);
					var filePath = outputPath + listOfFiles[j].getName();
					System.out.println(filePath);
					BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
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
