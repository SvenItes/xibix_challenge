import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class xibix_challenge_serverless implements RequestHandler<String[], String> {

    @Override
    public String handleRequest(String[] inputs, Context context) {
        String meshFile = inputs[0];
        int n = Integer.parseInt(inputs[1]);
        List<Map<Integer, Double>> listViewpoints = null;
        try {
            listViewpoints = findViewpoints(meshFile, n);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String listViewpointsJson = gson.toJson(listViewpoints);

        return listViewpointsJson;
    }


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
