import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChecksumOrganizer {

	private final static String[] EXTENSIONS = {"sfv", "md5", "sha1"};
	private final static String SYS_FILE_BEGIN = "!slv-textdb-";
	private final static byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
	private static int readedFile = 0;

	private ChecksumOrganizer() {
	}

	public static void main(final String[] args) {
		if (args.length == 0) {
			final String COMMA = ", ";
			String listExt = "";
			for (String ext : EXTENSIONS) {
				listExt += ext + COMMA;
			}
			listExt = listExt.substring(0, listExt.length() - COMMA.length());
			System.err.println("args[0] -- path to find checksum's files (" + listExt + ")");
			return;
		}

		final String sourcePath = args[0];
		final String fileNamePart = SYS_FILE_BEGIN + LocalDateTime.now().
				format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss"));

		for (String ext : EXTENSIONS) {
			findChecksumFiles(sourcePath, fileNamePart, ext);
		}
	}

	private static void findChecksumFiles(final String sourcePath, final String fileNamePart, final String extension) {
		final var arrPaths = FindFile.findFilesByOneExtWithoutSysFiles(sourcePath, SYS_FILE_BEGIN, extension);
		if (arrPaths.isEmpty()) {
			return;
		}

		final String fileName = fileNamePart + "." + extension;
		final Path target = Paths.get(sourcePath, fileName);
		try (var writer = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
			writer.write(UTF8_BOM);

			for (Path path : arrPaths) {
				transferToFile(writer, path);
			}
		} catch (IOException x) {
			System.err.format("IOException: %s%n", x);
		}

		System.out.println("===\t" + extension + "\nFile found: " + arrPaths.size());
		System.out.println("File read: " + readedFile);
	}

	private static void transferToFile(final OutputStream target, final Path source) {
		try {
			transferToFileWith2Attempts(target, source);
			++readedFile;
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private static void transferToFileWith2Attempts(final OutputStream target, final Path source) throws IOException {
		try {
			transferToFile(target, source, Charset.forName("windows-1251"));
			return;
		} catch (CharacterCodingException ex) {
		}

		transferToFile(target, source, StandardCharsets.UTF_8);
	}

	private static void transferToFile(final OutputStream target, final Path source, final Charset charset)
			throws IOException {
		try (var reader = Files.newBufferedReader(source, charset)) {
			String line = null;
			final StringBuilder sb = new StringBuilder(4096);
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
