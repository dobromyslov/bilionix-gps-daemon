import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

/**
 * TCP/IP Server.
 *
 * Listens port, accepts client connections and forwards them to ClientHandler in separate thread.
 */
public class Server {
    /**
     * Master server socket.
     */
    private static ServerSocket serverSocket = null;

    /**
     * Server configuration.
     */
    private static Properties config = new Properties();

    /**
     * Loads configuration from file 'config.properties'.
     * First it checks user defined 'config.properties' file in the current working directory.
     * If file does not exist it loads predefined file from server classpath.
     */
    private static void initConfig() {
        System.out.print("Loading user config ... ");
        try {
            config.load(new FileInputStream("config.properties"));
            System.out.print("done\n");
        }
        catch (IOException e) {
            System.out.print("error:\n");
            System.out.println(e);
            System.out.print("Loading default config ... ");
            try {
                config.load(Server.class.getClassLoader().getResourceAsStream("config.properties"));
            }
            catch (IOException e1) {
                System.err.println(e1);
                System.exit(1);
            }
            System.out.print("done\n");
        }

        config.list(System.out);
        System.out.println();
    }

    /**
     * Main entry point.
     * @param args application command line parameters
     */
    public static void main(String[] args) {
        // Load configuration
        initConfig();

        // Run server on the selected port
        int port = Integer.parseInt(config.getProperty("server.port"));
        System.out.print("Starting server on port " + port + " ... ");
        try {
            serverSocket = new ServerSocket(port);
        }
        catch (IOException e) {
            System.out.println("Could not listen on port. Seems it's already busy.");
            System.exit(1);
        }
        System.out.print("done\n");

        // Accept client connections
        System.out.print("Awaiting client connection ...\n");
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();

                // Run separate thread for client handler
                new ClientHandler(clientSocket, config.getProperty("handler.url"));
            }
            catch (IOException e) {
                System.err.println(e);
            }
        }
    }
}
