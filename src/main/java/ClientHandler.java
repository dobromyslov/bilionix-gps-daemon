
import java.io.*;
import java.net.*;

/**
 * TCP/IP client connection handler.
 *
 * Runs in separate thread.
 * Accepts message from the GPS-tracker via TCP/IP and forwards it to the Web-server via HTTP.
 */
public class ClientHandler implements Runnable {
    /**
     * Client socket.
     */
    private Socket clientSocket;

    /**
     * Web-server URL.
     */
    private String url;

    /**
     * Client socket IN-buffer.
     */
    private BufferedReader in;

    /**
     * Client socket OUT-buffer.
     */
    private PrintWriter out;

    /**
     * Handler thread.
     */
    private Thread runningThread;

    /**
     * Construct.
     * @param clientSocket client socket.
     * @param url          Web-server URL.
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
     * Disconnect client socket.
     * Interrupts handler thread, closes buffers and disconnects client.
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
     * Runs client socket handler.
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
     * Sends message to Web-server via HTTP.
     * @param message RAW message from the GPS-tracker.
     */
    private void sendToWebServer(String message) {
        try {
            // Encode the message to URLEncoded format
            String urlParameters = "message=" + URLEncoder.encode(message, "UTF-8");

            URL webServerUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection)webServerUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            // Send the message to the server
            DataOutputStream out = new DataOutputStream (connection.getOutputStream());
            out.writeBytes(urlParameters);
            out.flush();
            out.close();

            // Read response from the server
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
