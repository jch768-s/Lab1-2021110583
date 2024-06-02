import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.Viewer.CloseFramePolicy;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class TextToGraph {
    private Map<String, Map<String, Integer>> graph = new HashMap<>();

    public static void main(String[] args) {
        String filePath = args.length > 0 ? args[0] : getFilePathFromUser();
        TextToGraph textToGraph = new TextToGraph();
        textToGraph.readFileAndBuildGraph(filePath);
        textToGraph.interactiveMenu();
    }

    private static String getFilePathFromUser() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the file path: ");
        return scanner.nextLine();
    }

    private void readFileAndBuildGraph(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(filePath)))) {
            String line;
            String prevWord = null;
            while ((line = reader.readLine()) != null) {
                String[] words = line.replaceAll("[^a-zA-Z\\s]", " ")
                        .toLowerCase()
                        .split("\\s+");
                for (String word : words) {
                    if (!word.isEmpty()) {
                        if (prevWord != null) {
                            addEdge(prevWord, word);
                        }
                        prevWord = word;
                    }
                }
                prevWord = null; // Reset at the end of each line to handle cross-line word pairs
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addEdge(String from, String to) {
        graph.putIfAbsent(from, new HashMap<>());
        Map<String, Integer> edges = graph.get(from);
        edges.put(to, edges.getOrDefault(to, 0) + 1);
    }

    private void interactiveMenu() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\nSelect an option:");
            System.out.println("1. Show directed graph");
            System.out.println("2. Query bridge words");
            System.out.println("3. Generate new text with bridge words");
            System.out.println("4. Find shortest path between two words");
            System.out.println("5. Random walk");
            System.out.println("6. Exit");
            if(!scanner.hasNextInt()) {
                System.out.println("Invalid choice. Try again.");
                scanner.nextLine();
                continue;
            }
            int choice = scanner.nextInt();
            scanner.nextLine();  // Consume newline

            switch (choice) {
                case 1:
                    showDirectedGraph();
                    break;
                case 2:
                    queryBridgeWords(scanner);
                    break;
                case 3:
                    generateNewText(scanner);
                    break;
                case 4:
                    findShortestPath(scanner);
                    break;
                case 5:
                    randomWalk();
                    break;
                case 6:
                    return;
                default:
                    System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private void showDirectedGraph() {
        // 设置 UI 包
        System.setProperty("org.graphstream.ui", "swing");

        Graph gsGraph = new SingleGraph("TextGraph");

        // 添加 CSS 样式
        String css = "node { text-size: 40px; } edge { text-size: 40px; }";
        gsGraph.setAttribute("ui.stylesheet", css);

        for (String node : graph.keySet()) {
            gsGraph.addNode(node);
        }

        for (Map.Entry<String, Map<String, Integer>> entry : graph.entrySet()) {
            String from = entry.getKey();
            for (Map.Entry<String, Integer> edge : entry.getValue().entrySet()) {
                String to = edge.getKey();
                int weight = edge.getValue();
                String edgeId = from + "->" + to;
                if (gsGraph.getNode(from) == null) {
                    gsGraph.addNode(from);
                }
                if (gsGraph.getNode(to) == null) {
                    gsGraph.addNode(to);
                }
                if (gsGraph.getEdge(edgeId) == null) {
                    Edge e = gsGraph.addEdge(edgeId, from, to, true);
                    e.setAttribute("weight", weight);
                }
            }
        }

        for (Node node : gsGraph) {
            node.setAttribute("ui.label", node.getId());
        }

        gsGraph.edges().forEach(edge -> edge.setAttribute("ui.label", edge.getAttribute("weight")));

        // 显示图形并设置关闭策略
        Viewer viewer = gsGraph.display();
        viewer.setCloseFramePolicy(CloseFramePolicy.HIDE_ONLY);
    }

    
    private void queryBridgeWords(Scanner scanner) {
        System.out.println("Enter word1: ");
        String word1 = scanner.nextLine().toLowerCase();
        System.out.println("Enter word2: ");
        String word2 = scanner.nextLine().toLowerCase();

        String result = queryBridgeWords(word1, word2);
        System.out.println(result);
    }

    public String queryBridgeWords(String word1, String word2) {
        word1 = word1.toLowerCase();
        word2 = word2.toLowerCase();
    
        if (!graph.containsKey(word1) || !graph.containsKey(word2)) {
            return "No " + (!graph.containsKey(word1) ? word1 : word2) + " in the graph!";
        }
    
        Set<String> bridgeWords = new HashSet<>();
        Map<String, Integer> word1Edges = graph.get(word1);
    
        for (String potentialBridge : word1Edges.keySet()) {
            if (graph.containsKey(potentialBridge) && graph.get(potentialBridge).containsKey(word2)) {
                bridgeWords.add(potentialBridge);
            }
        }
    
        if (bridgeWords.isEmpty()) {
            return "No bridge words from " + word1 + " to " + word2 + "!";
        } else {
            String result = "The bridge words from " + word1 + " to " + word2 + " are: ";
            result += bridgeWords.stream().collect(Collectors.joining(", ", "", "."));
            return result;
        }
    }
    
    private void generateNewText(Scanner scanner) {
        System.out.println("Enter new text: ");
        String inputText = scanner.nextLine().toLowerCase();

        String result = generateNewText(inputText);
        System.out.println("Generated text: " + result);
    }

    public String generateNewText(String inputText) {
        String[] words = inputText.replaceAll("[^a-zA-Z\\s]", " ")
                .split("\\s+");

        List<String> newText = new ArrayList<>();
        for (int i = 0; i < words.length - 1; i++) {
            String word1 = words[i];
            String word2 = words[i + 1];
            newText.add(word1);
            Set<String> bridgeWords = new HashSet<>();
            if (graph.containsKey(word1)) {
                Map<String, Integer> neighbors1 = graph.get(word1);
                for (String neighbor : neighbors1.keySet()) {
                    if (graph.get(neighbor) != null && graph.get(neighbor).containsKey(word2)) {
                        bridgeWords.add(neighbor);
                    }
                }
            }

            if (!bridgeWords.isEmpty()) {
                List<String> bridgeWordsList = new ArrayList<>(bridgeWords);
                Collections.shuffle(bridgeWordsList);
                newText.add(bridgeWordsList.get(0));
            }
        }
        newText.add(words[words.length - 1]);

        return String.join(" ", newText);
    }

    private void findShortestPath(Scanner scanner) {
        System.out.println("Enter start word: ");
        String start = scanner.nextLine().toLowerCase();
        System.out.println("Enter end word: ");
        String end = scanner.nextLine().toLowerCase();

        String result = calcShortestPath(start, end);
        System.out.println(result);
    }

    public String calcShortestPath(String word1, String word2) {
        word1 = word1.toLowerCase();
        word2 = word2.toLowerCase();
    
        if (!graph.containsKey(word1) || !graph.containsKey(word2)) {
            return "No " + word1 + " or " + word2 + " in the graph!";
        }
    
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        PriorityQueue<String> queue = new PriorityQueue<>(Comparator.comparingInt(node -> distances.getOrDefault(node, Integer.MAX_VALUE)));
    
        for (String word : graph.keySet()) {
            distances.put(word, Integer.MAX_VALUE);
            previous.put(word, null);
        }
        distances.put(word1, 0);
        queue.add(word1);
    
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(word2)) {
                break;
            }
    
            int currentDistance = distances.getOrDefault(current, Integer.MAX_VALUE);
            if (currentDistance == Integer.MAX_VALUE) {
                break;
            }
    
            Map<String, Integer> neighbors = graph.get(current);
            if (neighbors == null) continue;  // Skip if there are no neighbors
    
            for (Map.Entry<String, Integer> neighborEntry : neighbors.entrySet()) {
                String neighbor = neighborEntry.getKey();
                int weight = neighborEntry.getValue();
                int newDist = currentDistance + weight;
    
                if (newDist < distances.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    distances.put(neighbor, newDist);
                    previous.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }
    
        if (distances.getOrDefault(word2, Integer.MAX_VALUE) == Integer.MAX_VALUE) {
            return "No path from " + word1 + " to " + word2 + "!";
        }
    
        List<String> path = new ArrayList<>();
        for (String at = word2; at != null; at = previous.get(at)) {
            path.add(at);
        }
        Collections.reverse(path);
    
        return "Shortest path from " + word1 + " to " + word2 + ": " + String.join(" -> ", path) + "\nPath weight: " + distances.get(word2);
    }
    
    public void randomWalk() {
        List<String> nodes = new ArrayList<>(graph.keySet());
        String current = nodes.get(new Random().nextInt(nodes.size()));
        Set<String> visitedEdges = new HashSet<>();
        List<String> path = new ArrayList<>();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            path.add(current);
            System.out.println("Current node: " + current);
            if (!graph.containsKey(current) || graph.get(current).isEmpty()) {
                break;
            }

            Map<String, Integer> neighbors = graph.get(current);
            List<String> possibleNextNodes = new ArrayList<>(neighbors.keySet());
            Collections.shuffle(possibleNextNodes);
            String next = possibleNextNodes.get(0);
            String edge = current + " -> " + next;

            if (visitedEdges.contains(edge)) {
                break;
            }

            visitedEdges.add(edge);
            current = next;

            System.out.println("Press Enter to continue or type 'stop' to end the walk.");
            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("stop")) {
                break;
            }
        }

        String result = String.join(" -> ", path);
        System.out.println("Random walk path: " + result);
        writeToFile(result, "random_walk.txt");
    }

    private void writeToFile(String content, String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(content);
            System.out.println("Random walk path written to " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
