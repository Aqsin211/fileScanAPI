package az.company.filescanner.service;

import az.company.filescanner.dao.entity.ScanResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.Socket;
import java.time.LocalDateTime;

import static az.company.filescanner.model.enums.StatusEnums.CLEAN;
import static az.company.filescanner.model.enums.StatusEnums.ERROR;
import static az.company.filescanner.model.enums.StatusEnums.INFECTED;
import static az.company.filescanner.model.enums.StatusEnums.UNKNOWN;
import static az.company.filescanner.model.enums.StatusEnums.UNRECOGNIZED_RESPONSE;

@Slf4j
@Service
public class ClamAVClientService {

    private static final String CLAMAV_HOST = "localhost";
    private static final int CLAMAV_PORT = 3310;

    public ScanResult scanFile(InputStream fileStream, String filename) {
        try (Socket socket = new Socket(CLAMAV_HOST, CLAMAV_PORT)) {
            socket.getOutputStream().write("zINSTREAM\0".getBytes());

            byte[] buffer = new byte[2048];
            int read;
            while ((read = fileStream.read(buffer)) >= 0) {
                socket.getOutputStream().write(intToBytes(read));
                socket.getOutputStream().write(buffer, 0, read);
            }

            socket.getOutputStream().write(intToBytes(0));

            byte[] responseBuffer = new byte[1024];
            int len = socket.getInputStream().read(responseBuffer);
            String response = new String(responseBuffer, 0, len).trim();

            log.info("ClamAV response: {}", response);

            ScanResult.ScanResultBuilder builder = ScanResult.builder()
                    .filename(filename)
                    .scannedAt(LocalDateTime.now());

            if (response.contains("OK")) {
                builder.status(CLEAN.getMessage()).virusName(null);
            } else if (response.contains("FOUND")) {
                int start = response.indexOf(": ") + 2;
                int end = response.indexOf(" FOUND");
                String virusName = (start >= 2 && end > start) ? response.substring(start, end) : UNKNOWN.getMessage();
                builder.status(INFECTED.getMessage()).virusName(virusName);
            } else {
                builder.status(ERROR.getMessage()).virusName(UNRECOGNIZED_RESPONSE.getMessage());
            }

            return builder.build();

        } catch (Exception e) {
            return ScanResult.builder()
                    .filename(filename)
                    .status(ERROR.getMessage())
                    .virusName(e.getMessage())
                    .scannedAt(LocalDateTime.now())
                    .build();
        }
    }

    private byte[] intToBytes(int value) {
        return new byte[]{
                (byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) value
        };
    }
}
