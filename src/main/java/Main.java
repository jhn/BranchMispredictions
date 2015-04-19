import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        String queryFile   = args[0];
        String configFile  = args[1];

        Properties costs = new Properties();
        costs.load(new FileInputStream(configFile));

        List<List<Double>> selectivityList = parseFileToDoubleList(Files.readAllLines(Paths.get(queryFile), Charset.defaultCharset()));

        List<Callable<String>> tasks = selectivityList
                .stream()
                .map(selectivities -> new Optimizer(selectivities, costs))
                .collect(Collectors.toList());

        ExecutorService executor = Executors.newFixedThreadPool(selectivityList.size());
        List<Future<String>> results = executor.invokeAll(tasks);
        results.forEach(System.out::println);
        executor.shutdown();
    }

    public static List<Double> parseLineToDoubleList(String line) {
        return Arrays.asList(line.split("\\s+"))
                .stream()
                .map(Double::parseDouble)
                .collect(Collectors.toList());
    }

    public static List<List<Double>> parseFileToDoubleList(List<String> fileLines) {
        return fileLines
                .stream()
                .map(Main::parseLineToDoubleList)
                .collect(Collectors.toList());
    }
}
