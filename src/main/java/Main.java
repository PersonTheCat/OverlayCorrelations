import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        final File f = getDocument(args);
        final List<String> lines = getContents(f);
        final List<Map<String, Double>> maps = getMaps(lines);
        final Map<String, Double> correlations = getCorrelations(maps);

        correlations.forEach((k, v) -> System.out.println(k + ": " + v));
    }

    /** Gets an existing file from program args or stdin. */
    private static File getDocument(String[] mainArgs) {
        Optional<File> f = Optional.empty();
        if (mainArgs.length > 0 && (f = tryGetFile(mainArgs[0])).isPresent()) {
            return f.get();
        }
        final Scanner s = new Scanner(System.in);
        System.out.print("Enter a path to the document: ");
        while (!f.isPresent()) {
            f = tryGetFile(s.nextLine().replace("\"", ""));
        }
        return f.get();
    }

    /** Attempts to load a file from at the input location. */
    private static Optional<File> tryGetFile(String path) {
        final File f = Paths.get(path).toFile();
        if (f.exists()) {
            return Optional.of(f);
        }
        System.err.println("Invalid path. Try again.");
        return Optional.empty();
    }

    /** Attempts to load all contents from the input file. */
    private static List<String> getContents(File f) {
        try {
            return Files.readAllLines(f.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Error reading contents: ", e);
        }
    }

    /** Maps values in each group of lines, separated by blank lines. */
    private static List<Map<String, Double>> getMaps(List<String> lines) {
        final List<Map<String, Double>> maps = new ArrayList<>();
        Map<String, Double> current = new HashMap<>();
        int lineNum = 0;

        for (String line : lines) {
            final String[] split = line.split(":");
            lineNum++;

            if (split.length == 0 || line.isEmpty()) { // line is blank  -> new set
                if (current.size() > 0) {
                    maps.add(current);
                }
                current = new HashMap<>();
            } else if (split.length == 1) {            // no keys.       -> ignore
                continue;
            } else if (split.length == 2) {            // valid syntax   -> parse
                parseValues(current, split);
            } else {                                   // invalid syntax -> quit
                throw new RuntimeException("Invalid syntax @" + lineNum);
            }
        }
        return maps;
    }

    /** Parses a key and value from the array and adds them to the map. */
    private static void parseValues(Map<String, Double> current, String[] split) {
        try {
            current.put(split[0].trim(), Double.parseDouble(split[1].trim()));
        } catch (NumberFormatException ignored) { /* Assume no value exists. */ }
    }

    private static Map<String, double[]> flatten(List<Map<String, Double>> maps) {
        final Map<String, double[]> flat = new HashMap<>();
        final Set<String> keys = getKeys(maps);
        keys.forEach(k -> flat.put(k, getAll(maps, k)));
        return flat;
    }

    /** Gets all of the unique keys in the entire map. */
    private static Set<String> getKeys(List<Map<String, Double>> maps) {
        final Set<String> keys = new HashSet<>();
        maps.forEach(map -> map.forEach((key, value) -> keys.add(key)));
        return keys;
    }

    /** Gets a list of every value belonging to the input key. */
    private static double[] getAll(List<Map<String, Double>> maps, String key) {
        final List<Double> values = new ArrayList<>();
        maps.forEach(map -> {
            if (map.containsKey(key)) {
                values.add(map.get(key));
            }
        });
        return cast(values);
    }

    private static double[] cast(List<Double> values) {
        final double[] doubles = new double[values.size()];
        for (int i = 0; i < doubles.length; i++) {
            doubles[i] = values.get(i);
        }
        return doubles;
    }

    /** Calculates a Pearson Correlation coefficient for each unique pair of values. */
    private static Map<String, Double> getCorrelations(List<Map<String, Double>> maps) {
        final Map<String, Double> correlations = new HashMap<>();
        final Set<String> keys = getKeys(maps);
        for (String k1 : keys) {
            for (String k2: keys) {
                final List<Double> v1 = new ArrayList<>();
                final List<Double> v2 = new ArrayList<>();
                for (Map<String, Double> map : maps) {
                    if (map.containsKey(k1) && map.containsKey(k2)) {
                        v1.add(map.get(k1));
                        v2.add(map.get(k2));
                    }
                }
                final double r = new PearsonsCorrelation().correlation(cast(v1), cast(v2));
                correlations.put(k1 + " -> " + k2, r);
            }
        }
        return correlations;
    }

    // Use this instead to only calculate correlations where all values are present.
    /** Calculates a Pearson Correlation coefficient for each unique pair of values. */
    private static Map<String, Double> getCorrelations(Map<String, double[]> map) {
        final Map<String, Double> correlations = new HashMap<>();
        map.forEach((key1, values1) ->
            map.forEach((key2, values2) -> {
                if (values1.length == values2.length) {
                    final double r = new PearsonsCorrelation().correlation(values1, values2);
                    correlations.put(key1 + " -> " + key2, r);
                }
            })
        );
        return correlations;
    }
}