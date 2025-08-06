package az.company.filescanner.service;

import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

@Service
public class ClamAVClientService {

    private static final String CLAMAV_HOST = "localhost";
    private static final int CLAMAV_PORT = 3310;

    public boolean isFileClean(InputStream inputStream) {
        try (Socket socket = new Socket(CLAMAV_HOST, CLAMAV_PORT)) {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // zINSTREAM command
            out.write("zINSTREAM\0".getBytes());

            byte[] buffer = new byte[2048];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                out.write(intToByteArray(bytesRead));
                out.write(buffer, 0, bytesRead);
            }

            // End of stream
            out.write(new byte[]{0, 0, 0, 0});
            out.flush();

            // Read ClamAV response
            StringBuilder response = new StringBuilder();
            int ch;
            while ((ch = in.read()) != -1) {
                response.append((char) ch);
                if (response.toString().endsWith("\n")) break;
            }

            String result = response.toString();
            return result.contains("OK"); //stream: OK or stream: Eicar-Test-Signature FOUND
        } catch (Exception e) {
            throw new RuntimeException("ClamAV scan failed", e);
        }
    }

    private byte[] intToByteArray(int value) {
        return new byte[]{
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value
        };
    }
}
