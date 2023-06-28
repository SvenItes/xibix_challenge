import org.json.JSONArray;
import org.json.JSONObject;
import com.google.gson.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class xibix_challenge {
    public static void main(String[] args) throws IOException {
        String meshFile = args[0];
        int n = Integer.parseInt(args[1]);
        List<Map<Integer, Double>> listViewpoints = findViewpoints(meshFile, n);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String listViewpointsJson = gson.toJson(listViewpoints);
        System.out.println(listViewpointsJson);
    }

    /**
     * The path of the mesh file and the desired number of viewpoints, which are passed as arguments from the
     * command line, are assigned to the variables meshFile and n, respectively. Then the JSON file of the mesh is
     * assigned to the variable mesh. The variables meshElements and meshValues are used for a better subdivision of
     * the areas of the JSON file. The Hashmap mapViewpoints stores the resulting highest viewpoints and the
     * variable idxIgnore stores the indices of the elements that have a higher neighbor and can thus be ignored in
     * the iterations of the subsequent FOR loops.
     * In the first FOR loop, each element e of the mesh is iteratively examined. If mapViewpoints contains the desired
     * number of viewpoints, the loop is exited. If the current element is already contained in mapViewpoints or the
     * ID of the element is contained in idxIgnore, the element is skipped. In the rest of the loop, the set of nodes,
     * the nodes of the current highest neighbors, the height value of the current element, the ID of the current
     * highest element, and the height value of the current highest element are assigned to the corresponding
     * variables eNodes, tempHighestNeighborsNodes, valueE, tempHighestID, and tempHighestValue, respectively.
     * In the next FOR loop, the current element is iteratively compared with the remaining elements compE of the mesh.
     * If the element compE has the same ID as the element e or the ID of compE is contained in idxIgnore, the
     * current compE is skipped. In the further course of the loop, the ID and the set of nodes of compE are assigned
     * to the respective variables compEId and compENodes. If element compE is a neighbor of element e or the
     * nodes of compE are contained in tempHighestNeighborsNodes, height value of compE is assigned to variable
     * valueCompE. If the height value of compe is higher than tempHighestValue, the corresponding temporary
     * variables are updated and the index of compE is added to idxIgnore. If the value is lower than
     * tempHighestValue, the index of compE is added to idxIgnore. Finally, in the second FOR loop,
     * tempHighestID is added to mapViewpoints with the corresponding tempHighestValue. After all elements from
     * the mesh have been examined in the first FOR loop or the loop has been exited early, mapViewpoints is sorted
     * and returned as a list.
     * @param meshFile JSON-File, which contains the mesh
     * @param n Number of desired view points
     * @return List of n view points with ID as key and height as value ordered by highest to lowest value
     * @throws IOException
     */
    public static List<Map<Integer, Double>> findViewpoints(String meshFile, int n) throws IOException {
        String meshContent = new String(Files.readAllBytes(Paths.get(meshFile)));
        JSONObject mesh = new JSONObject(meshContent);
        JSONArray meshElements = mesh.getJSONArray("elements");
        JSONArray meshValues = mesh.getJSONArray("values");
        Map<Integer, Double> mapViewpoints = new HashMap<>();
        Set<Integer> idxIgnore = new HashSet<>();

        for (int idxE = 0; idxE < meshElements.length(); idxE++) {
            if (mapViewpoints.size() == n) {
                break;
            }
            JSONObject e = meshElements.getJSONObject(idxE);
            int id = e.getInt("id");
            if (mapViewpoints.containsKey(id) || idxIgnore.contains(id)) {
                continue;
            }

            Set<Integer> eNodes = new HashSet<>(Arrays.asList(e.getJSONArray("nodes").toList().toArray(new Integer[0])));
            Set<Integer> tempHighestNeighborsNodes = new HashSet<>(eNodes);
            double valueE = meshValues.getJSONObject(idxE).getDouble("value");
            int tempHighestID = id;
            double tempHighestValue = valueE;

            for (int idxCompE = 0; idxCompE < meshElements.length(); idxCompE++) {
                JSONObject compE = meshElements.getJSONObject(idxCompE);
                int compEId = compE.getInt("id");
                if (compEId == id || idxIgnore.contains(compEId)) {
                    continue;
                }

                Set<Integer> compENodes = new HashSet<>(Arrays.asList(compE.getJSONArray("nodes").toList().toArray(new Integer[0])));

                if (!Collections.disjoint(eNodes, compENodes) || !Collections.disjoint(compENodes, tempHighestNeighborsNodes)) {
                    double valueCompE = meshValues.getJSONObject(idxCompE).getDouble("value");

                    if (valueCompE > tempHighestValue) {
                        tempHighestValue = valueCompE;
                        tempHighestID = compEId;
                        tempHighestNeighborsNodes = new HashSet<>(compENodes);
                        idxIgnore.add(idxCompE);
                    } else {
                        idxIgnore.add(idxCompE);
                    }
                }
            }

            mapViewpoints.put(tempHighestID, tempHighestValue);
        }

        List<Map<Integer, Double>> finalViewList = new ArrayList<>();
        Map<Integer, Double> sortedViewpoints = new TreeMap<>(Comparator.comparingDouble(mapViewpoints::get).reversed());
        sortedViewpoints.putAll(mapViewpoints);

        for (Map.Entry<Integer, Double> entry : sortedViewpoints.entrySet()) {
            Map<Integer, Double> tempHashmap = new HashMap<>();
            tempHashmap.put(entry.getKey(), entry.getValue());
            finalViewList.add(tempHashmap);
        }

        return finalViewList;
    }
}
