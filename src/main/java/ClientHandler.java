
import java.io.*;
import java.net.*;

/**
 * Обработчик подключения клиента.
 *
 * Запускает обработку в отдельном потоке.
 * Принимает сообщение от GPS-трэкера и передаёт его на web-сервер.
 */
public class ClientHandler implements Runnable {
    /**
     * Сокет клиента.
     */
    private Socket clientSocket;

    /**
     * URL web-сервера.
     */
    private String url;

    /**
     * Входной буфер клиентского сокета.
     */
    private BufferedReader in;

    /**
     * Выходной буфер клиентского сокета.
     */
    private PrintWriter out;

    /**
     * Поток обработчика.
     */
    private Thread runningThread;

    /**
     * Конструктор.
     * @param clientSocket сокет клиента.
     * @param url          URL web-сервера.
     */
    public ClientHandler(Socket clientSocket, String url) {
        this.clientSocket = clientSocket;
        this.url = url;

        System.out.println("Client connected with Address " + clientSocket.getInetAddress().toString() + " on port: " + clientSocket.getPort() + "\n");

        try {
            System.out.print("Initializing socket buffers size ... ");
            clientSocket.setReceiveBufferSize(2);
            clientSocket.setSendBufferSize(2);
            System.out.print("done\n");

            System.out.print("Initializing input buffer ... ");
            in  = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            System.out.print("done\n");

            System.out.print("Initializing output buffer ... ");
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            System.out.print("done\n");

            System.out.print("Starting handler thread ... ");
            runningThread = new Thread(this);
            runningThread.start();
            System.out.print("done\n");
        }
        catch (Exception e) {
            System.err.println(e);
            disconnect();
        }
    }

    /**
     * Прерывает поток обработчика, закрывает буферы и отключает сокет клиента.
     */
    private void disconnect() {
        System.out.print("Disconnecting ... ");
        if (runningThread != null) {
            runningThread.interrupt();
            runningThread = null;
        }

        if (in != null) {
            try {
                in.close();
            } catch (Exception e){
                System.err.println(e);
            }
            in = null;
        }

        if (out != null) {
            try {
                out.close();
            } catch(Exception e){
                System.err.println(e);
            }
            out = null;
        }

        try {
            clientSocket.close();
        } catch(Exception e){
            System.err.println(e);
        }
        clientSocket = null;

        System.out.print("done\n");
    }

    /**
     * Запускает обработку клиентского соединения.
     */
    @Override
    public void run() {
        System.out.print("Reading input buffer ... ");
        String line;
        String message = "";
        try {
            while ((line = in.readLine()) != null) {
                message += line + "\n";
            }
        }
        catch (IOException e) {
            System.err.println(e);
        }
        System.out.print("done\n");

        if (!message.isEmpty()) {
            System.out.println("Received from GPS: " + message + "\n");
            System.out.println("Sending to web server ... ");
            sendToWebServer(message);
        }
        else {
            System.out.println("Message is empty\n");
        }
        disconnect();
    }

    /**
     * Отправляет сообщение на web-сервер.
     * @param message сообщение от GPS-трэкера.
     */
    private void sendToWebServer(String message) {
        try {
            // Кодирование сообщения в URLEncoded формат
            String urlParameters = "message=" + URLEncoder.encode(message, "UTF-8");

            URL webServerUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection)webServerUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            // Отправка сообщения
            DataOutputStream out = new DataOutputStream (connection.getOutputStream());
            out.writeBytes(urlParameters);
            out.flush();
            out.close();

            // Чтение ответа сервера
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            String result = "";
            while ((inputLine = in.readLine()) != null) {
                result += inputLine + "\n";
            }
            in.close();
            System.out.print("HTTP request sent with result: " + result);

            connection.disconnect();
        }
        catch (Exception e) {
            System.err.println(e);
        }
    }
}
