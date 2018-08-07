import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ChecksumOrganizer {

	private final static String MD5_EXT = "md5";
	private final static String SYS_FILE_BEGIN = "!slv-" + MD5_EXT + "-";
	private final static byte[] UTF8_BOM = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
	private static int readedFile = 0;

	private ChecksumOrganizer() {
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			System.err.println("args[0] -- path to scan '" + MD5_EXT + "'");
			return;
		}

		final String dir = args[0];
		final List<Path> arrPaths = FindFile.findFilesByExt(dir, SYS_FILE_BEGIN, MD5_EXT);
		if (arrPaths.isEmpty()) {
			return;
		}

		final String fileName = SYS_FILE_BEGIN
				+ new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss'." + MD5_EXT + "'").format(new Date());
		final Path target = Paths.get(dir, fileName);
		try (OutputStream writer = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
			writer.write(UTF8_BOM);
			arrPaths.stream().forEach(path -> transferToFile(writer, path));
		} catch (IOException x) {
			System.err.format("IOException: %s%n", x);
		}

		System.out.println("File found: " + arrPaths.size());
		System.out.println("File read: " + readedFile);
	}

	private static void transferToFile(final OutputStream target, final Path source) {
		try {
			transferToFile2Try(target, source);
			++readedFile;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void transferToFile2Try(final OutputStream target, final Path source) throws IOException {
		boolean isNeedUtf8 = true;
		try {
			transferToFile(target, source, Charset.forName("windows-1251"));
			isNeedUtf8 = false;
		} catch (CharacterCodingException ex) {
		}

		if (isNeedUtf8) {
			transferToFile(target, source, StandardCharsets.UTF_8);
		}
	}

	private static void transferToFile(final OutputStream target, final Path source, final Charset charset)
			throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(source, charset)) {
			String line = null;
			final StringBuilder sb = new StringBuilder(1024);
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (!line.isEmpty()) {
					sb.append(line);
					sb.append("\n");
				}
			}

			final String out = sb.toString();
			target.write(out.getBytes(StandardCharsets.UTF_8));
		}
	}
}
