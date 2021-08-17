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
import java.util.function.Function;

public final class ChecksumOrganizer {
	private final static String EXT_SFV = "sfv";
	private final static String[] EXTENSIONS = {EXT_SFV, "md5", "sha1"};
	private final static String SYS_FILE_BEGIN = "!slv-textdb-";
	private final static byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
	private final static Function<String, String> EMPTY_FUNC = Function.identity();
	private final static Function<String, String> CLEAN_FUNC = (x) -> x.replaceAll(";.*\\n?", "");

	private ChecksumOrganizer() {
	}

	public static void main(final String[] args) {
		if (tryShowHelp(args)) {
			return;
		}

		final var sourcePath = args[0];
		final var fileNamePart = SYS_FILE_BEGIN + LocalDateTime.now().
				format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss"));

		System.out.println("===\tSearching checksum files...");
		for (final String ext : EXTENSIONS) {
			searchChecksumFiles(sourcePath, fileNamePart, ext);
		}
	}

	private static boolean tryShowHelp(final String[] args) {
		if (args.length == 0) {
			final var COMMA = ", ";
			final var listExt = new StringBuilder(512);
			for (final var ext : EXTENSIONS) {
				listExt.append(ext)
						.append(COMMA);
			}
			final String strExt = listExt.substring(0, listExt.length() - COMMA.length());
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

		// remove comments
		Function<String, String> parseFunc = extension.equals(EXT_SFV) ? CLEAN_FUNC : EMPTY_FUNC;

		int readFile = 0;
		final var fileName = fileNamePart + "." + extension;
		final Path target = Paths.get(sourcePath, fileName);
		try (var writer = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
			writer.write(UTF8_BOM);

			for (final var path : arrPaths) {
				transferToSingleFile(writer, path, parseFunc);
				++readFile;
			}
		} catch (Exception ex) {
			System.err.format("Exception: %s%n", ex);
		}

		System.out.println("===\t" + extension + "\t\tFile found: " + arrPaths.size());
		System.out.println("File read: " + readFile);
	}

	private static void transferToSingleFile(final OutputStream target,
	                                         final Path source,
	                                         final Function<String, String> parseFunc) {
		try {
			transferToSingleFileWith2Attempts(target, source, parseFunc);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private static void transferToSingleFileWith2Attempts(final OutputStream target,
	                                                      final Path source,
	                                                      final Function<String, String> parseFunc) throws IOException {
		try {
			transferToSingleFile(target, source, StandardCharsets.UTF_8, parseFunc);
			return;
		} catch (CharacterCodingException ex) {
			// Do nothing
		}

		transferToSingleFile(target, source, Charset.forName("windows-1251"), parseFunc);
	}

	private static void transferToSingleFile(final OutputStream target,
	                                         final Path source,
	                                         final Charset charset,
	                                         final Function<String, String> parseFunc)
			throws IOException {
		try (var reader = Files.newBufferedReader(source, charset)) {
			String line;
			final var sb = new StringBuilder(12 * 1024);
			while ((line = reader.readLine()) != null) {
				line = line.strip();
				if (!line.isEmpty()) {
					sb.append(line);
					sb.append("\n");
				}
			}

			final var out = parseFunc.apply(sb.toString());
			target.write(out.getBytes(StandardCharsets.UTF_8));
		}
	}
}
