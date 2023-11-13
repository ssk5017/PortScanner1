import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import java.util.TreeMap;

public class PortScanner {
    public static void main(String[] args) {

        String targetHost = "localhost"; // THIS A HOST YOU WANT TO TARGET FOR A SCAN
        int minPort = 1;
        int maxPort = 65535;

        // TreeMap made to store port numbers and descriptions
        TreeMap<Integer, String> portData = new TreeMap<>();

        // Reading the file(CSV) TO THE LOOP OUTSIDE
        String pathToCsv = "service-names-port-numbers.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(pathToCsv))) {
            String line;
            while ((line = br.readLine()) != null) {
                // leave the lines that don't have the data layout
                if (!line.contains(",")) {
                    continue;
                }

                // Use comma as a separator.
                String[] columns = line.split(",");

                // Check if the line has the expected # of columns.
                if (columns.length >= 4) {
                    try {
                        // Extract the port number (column 1) and the description (column 3).
                        int portNumber = Integer.parseInt(columns[1].trim());
                        String description = columns[3].trim();

                        // Savew the data in the TreeMap if the description is not empty.
                        if (!description.isEmpty()) {
                            portData.put(portNumber, description);
                        }
                    } catch (NumberFormatException e) {
                        // Handle the parsing errors when there is no print.
                    }
                } else {
                    // Handle lines that don't have the number of columns that are expected (no print here)
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Scan ports to start Redis
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            for (int port = minPort; port <= maxPort; port++) {
                try {
                    Socket socket = new Socket(targetHost, port);

                    // Make sure to see if the port is in the TreeMap and has an empty description.
                    if (portData.containsKey(port)) {
                        String description = portData.get(port);

                        // Print and create (key-value pair) in Redis if the description is unavailable. 
                        if (!description.isEmpty()) {
                            System.out.println("Port " + port + ": " + description);
                            jedis.set(String.valueOf(port), description);

                            // Read from Redis to get value from the key. 
                            String value = jedis.get(String.valueOf(port));
                            // System.out.println(value);
                        }
                    }
                    socket.close();
                } catch (IOException e) {
                    // Port is unreachable or not there.
                }
            }
        } catch (JedisConnectionException e) {
            System.out.println("Could not connect to Redis: " + e.getMessage());
        }
    }
}
