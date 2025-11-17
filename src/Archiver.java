import java.io.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class Archiver {
    private final static int BLOCKSIZE = 16_384;
    private final static int FILENAMEBLOCKSIZE = 256;
    private final static int LONGSIZE = 8;
    private final File target;
    private final List<File> sources;

    public Archiver(String path) {
        if (path == null)
            throw new IllegalArgumentException("Path is null");

        target = new File(path);

        File parent = target.getParentFile();
        if (parent != null) {
            if (!parent.exists())
                throw new IllegalArgumentException("Parent directory does not exist: " + parent.getPath());
            if (!parent.canWrite())
                throw new IllegalArgumentException("Cannot write to parent directory: " + parent.getPath());
        }

        sources = new ArrayList<>();
    }

    public void add(String path) {
        if (path == null)
            throw new IllegalArgumentException("Path is null");

        File file = new File(path);
        if (!file.exists())
            throw new IllegalArgumentException("File does not exist: " + path);
        if (!file.isFile())
            throw new IllegalArgumentException("Path does not describe a file: " + path);
        if (!file.canRead())
            throw new IllegalArgumentException("File is not readable: " + path);

        sources.add(file);
    }

    public void write() {
        if (sources.isEmpty())
            return;

        MessageDigest memoryDigest;
        try {
            memoryDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        try (FileOutputStream outputStream = new FileOutputStream(target)) {
            byte[] header = constructHeader(sources.toArray(new File[0]));
            memoryDigest.update(header);
            outputStream.write(header);

            for (File file : sources) {
                FileInputStream inputStream = new FileInputStream(file);
                int read;
                byte[] buffer = new byte[BLOCKSIZE];
                while ((read = inputStream.read(buffer)) != -1) {
                    memoryDigest.update(buffer, 0, read);
                    outputStream.write(buffer, 0, read);
                }
                inputStream.close();
            }

            outputStream.flush();
            outputStream.getFD().sync();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] memoryHash = memoryDigest.digest();
        byte[] diskHash = digestFile(target);

        if (!arrayEquals(memoryHash, diskHash))
            throw new IllegalStateException("Integrity check failed");
    }

    private boolean arrayEquals(byte[] a, byte[] b) {
        if (a == null || b == null)
            throw new IllegalArgumentException("Input array is null");

        if (a.length != b.length)
            return false;

        for (int i = 0; i < a.length; i++)
            if (a[i] != b[i])
                return false;

        return true;
    }

    private byte[] digestFile(File file) {
        if (file == null)
            throw new IllegalArgumentException("File is null");

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        try (FileInputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[BLOCKSIZE];
            int read;
            while ((read = inputStream.read(buffer)) != -1)
                digest.update(buffer, 0, read);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return digest.digest();
    }

    private byte[] constructHeader(File[] files) {
        if (files == null)
            throw new IllegalArgumentException("File array is null");

        ByteBuffer buffer = ByteBuffer.allocate(LONGSIZE + LONGSIZE * files.length);
        buffer.putLong(files.length);
        for (File file : files)
            buffer.putLong(file.length());

        return buffer.array();
    }

    private byte[] fileMetadata(File file) {
        if (file == null)
            throw new IllegalArgumentException("File is null");

        ByteBuffer buffer = ByteBuffer.allocate(LONGSIZE + FILENAMEBLOCKSIZE);
    }
}