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

public final class ChecksumOrganizer {

	private final static String[] EXTENSIONS = {"sfv", "md5", "sha1"};
	private final static String SYS_FILE_BEGIN = "!slv-textdb-";
	private final static byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

	private ChecksumOrganizer() {
	}

	public static void main(final String[] args) {
		if (tryShowHelp(args)) {
			return;
		}

		final var sourcePath = args[0];
		final var fileNamePart = SYS_FILE_BEGIN + LocalDateTime.now().
				format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss"));

		for (String ext : EXTENSIONS) {
			searchChecksumFiles(sourcePath, fileNamePart, ext);
		}
	}

	private static boolean tryShowHelp(final String[] args) {
		if (args.length == 0) {
			final var COMMA = ", ";
			final var listExt = new StringBuilder(256);
			for (String ext : EXTENSIONS) {
				listExt.append(ext)
						.append(COMMA);
			}
			final var strExt = listExt.substring(0, listExt.length() - COMMA.length());
			System.err.println("args[0] -- path to find checksum files (" + strExt + ")");
			return true;
		}
		return false;
	}

	private static void searchChecksumFiles(final String sourcePath, final String fileNamePart, final String extension) {
		final var arrPaths = FindFile.findFilesByOneExtWithoutSysFiles(sourcePath, SYS_FILE_BEGIN, extension);
		if (arrPaths.isEmpty()) {
			return;
		}

		int readFile = 0;
		final var fileName = fileNamePart + "." + extension;
		final Path target = Paths.get(sourcePath, fileName);
		try (var writer = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
			writer.write(UTF8_BOM);

			for (Path path : arrPaths) {
				transferToFile(writer, path);
				++readFile;
			}
		} catch (Exception ex) {
			System.err.format("Exception: %s%n", ex);
		}

		System.out.println("===\t" + extension + "\t\tFile found: " + arrPaths.size());
		System.out.println("File read: " + readFile);
	}

	private static void transferToFile(final OutputStream target, final Path source) {
		try {
			transferToFileWith2Attempts(target, source);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private static void transferToFileWith2Attempts(final OutputStream target, final Path source) throws IOException {
		try {
			transferToFile(target, source, Charset.forName("windows-1251"));
			return;
		} catch (CharacterCodingException ex) {
			// Do nothing
		}

		transferToFile(target, source, StandardCharsets.UTF_8);
	}

	private static void transferToFile(final OutputStream target, final Path source, final Charset charset)
			throws IOException {
		try (var reader = Files.newBufferedReader(source, charset)) {
			String line;
			final StringBuilder sb = new StringBuilder(12 * 1024);
			while ((line = reader.readLine()) != null) {
				line = line.strip();
				if (!line.isEmpty()) {
					sb.append(line);
					sb.append("\n");
				}
			}

			final var out = sb.toString();
			target.write(out.getBytes(StandardCharsets.UTF_8));
		}
	}
}
