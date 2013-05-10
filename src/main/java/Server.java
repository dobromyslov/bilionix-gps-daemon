import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

/**
 * Сервер TCP/IP.
 *
 * Принимает соединения на заданном порту и передаёт их на обработку в ClientHandler.
 */
public class Server {
    private static ServerSocket serverSocket = null;

    /**
     * Конфигурация сервера.
     */
    private static Properties config = new Properties();

    /**
     * Загружает конфигурацию из файла config.properties.
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
     * Точка входа.
     * @param args параметры запуска приложения
     */
    public static void main(String[] args) {
        // Загрузка свойств
        initConfig();

        // Запуск сервера на указанном порту
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

        // Прием подключений
        System.out.print("Awaiting client connection ...\n");
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();

                // Запуск отдельного потока обработчика сообщений
                new ClientHandler(clientSocket, config.getProperty("handler.url"));
            }
            catch (IOException e) {
                System.err.println(e);
            }
        }
    }
}
