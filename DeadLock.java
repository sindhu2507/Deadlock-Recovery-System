import java.io.*;
import java.util.*;
import com.google.gson.Gson;

public class DeadLock {

    public static void main(String[] args) {
        int count = 0;
        int numRe = 0;
        char[][] matrix = new char[1000][1000];

        if (args.length < 1) {
            System.out.println("Usage: java DeadLock <input_file_path>");
            return;
        }

        String fileName = args[0];

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] mat = line.split(",");
                for (int i = 0; i < mat.length; i++) {
                    matrix[count][i] = mat[i].charAt(0);
                }
                count++;
                numRe = mat.length;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        int sizeE = 0;
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < numRe; j++) {
                if (matrix[i][j] != '0') {
                    sizeE++;
                }
            }
        }

        GraphData graphData = createItem(matrix, count, numRe, sizeE);
        DeadlockReport report = checkDeadLock(graphData.nodes, graphData.edges);
        Gson gson = new Gson();
        System.out.println(gson.toJson(report));
    }

    static class GraphData {
        String[] nodes;
        String[] edges;

        GraphData(String[] nodes, String[] edges) {
            this.nodes = nodes;
            this.edges = edges;
        }
    }

    static class RecoveryStep {
        String action;
        String detail;

        RecoveryStep(String action, String detail) {
            this.action = action;
            this.detail = detail;
        }
    }

    static class DeadlockReport {
        boolean deadlockDetected;
        List<String> cycle;
        List<RecoveryStep> recoverySteps;
    }

    public static GraphData createItem(char[][] matrix, int sizeP, int sizeR, int sizeE) {
        String[] node = new String[sizeP + sizeR];
        int count = 0;
        for (int i = 0; i < sizeP; i++)
            node[count++] = "P" + i;
        for (int j = 0; j < sizeR; j++)
            node[count++] = "R" + j;

        String[] edge = new String[sizeE];
        int countE = 0;
        for (int i = 0; i < sizeP; i++) {
            for (int j = 0; j < sizeR; j++) {
                if (matrix[i][j] == '1') {
                    edge[countE++] = node[i] + "-->" + node[sizeP + j];
                } else if (matrix[i][j] == '2') {
                    edge[countE++] = node[sizeP + j] + "-->" + node[i];
                }
            }
        }
        return new GraphData(node, edge);
    }

    public static DeadlockReport checkDeadLock(String[] node, String[] e) {
        String N;
        boolean check = false;
        boolean deadend;
        HashMap<String, String> edge = new HashMap<>();
        List<String> L = new ArrayList<>();
        String endLoop = "";

        for (String edgeStr : e) {
            edge.put(edgeStr, "0");
        }

        for (int i = 0; i < node.length && !check; i++) {
            N = node[i];
            L.clear();

            while (true) {
                String dest = "";
                if (!L.contains(N)) {
                    deadend = true;
                    L.add(N);

                    for (Map.Entry<String, String> entry : edge.entrySet()) {
                        String[] parts = entry.getKey().split("-->");
                        if (parts[0].equals(N) && entry.getValue().equals("0")) {
                            dest = parts[1];
                            edge.put(entry.getKey(), "1");
                            deadend = false;
                            break;
                        }
                    }
                } else {
                    check = true;
                    endLoop = N;
                    L.add(endLoop);
                    break;
                }

                if (!deadend) {
                    N = dest;
                } else {
                    if (L.size() >= 2) {
                        N = L.get(L.size() - 2);
                        L.remove(L.size() - 1);
                        L.remove(L.size() - 1);
                    } else {
                        L.clear();
                        break;
                    }
                }
            }

            for (String edgeStr : e) {
                edge.put(edgeStr, "0");
            }
        }

        DeadlockReport report = new DeadlockReport();
        if (check) {
            report.deadlockDetected = true;
            report.cycle = L.subList(L.indexOf(endLoop), L.size());
            report.recoverySteps = generateRecoverySteps(report.cycle);
        } else {
            report.deadlockDetected = false;
            report.cycle = Collections.emptyList();
            report.recoverySteps = Collections.emptyList();
        }

        return report;
    }

    public static List<RecoveryStep> generateRecoverySteps(List<String> cycle) {
        List<RecoveryStep> steps = new ArrayList<>();
        String lowestPriorityProcess = null;

        for (String node : cycle) {
            if (node.startsWith("P")) {
                lowestPriorityProcess = node; // assume last process in cycle has lowest priority
            }
        }

        if (lowestPriorityProcess != null) {
            steps.add(new RecoveryStep("Terminate", "Terminate " + lowestPriorityProcess + " to break the cycle"));
            steps.add(new RecoveryStep("Preempt Resource", "Release all resources held by " + lowestPriorityProcess));
            steps.add(new RecoveryStep("Adjust Priority", "Increase priority of other processes to resolve wait"));
        }

        return steps;
    }
}
