import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class FindFile {

	private FindFile() {
	}

	private static boolean innerSearchFunc(final Path path, final String sysBeginPart, final String extension) {
		// System.out.println("===\tPath: " + path.toString());

		if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
			final var filename = path.getFileName().toString().toLowerCase();
			return filename.endsWith(extension) && !filename.startsWith(sysBeginPart);
		}
		return false;
	}

	public static List<Path> findFilesByOneExtWithoutSysFiles(final String location,
	                                                          final String sysBeginPart,
	                                                          final String extension) {
		final var arr = new ArrayList<Path>();
		final var extension0 = "." + extension;

		try (var dirStream = Files.newDirectoryStream(Paths.get(location),
				path -> innerSearchFunc(path, sysBeginPart, extension0))) {
			for (final var path : dirStream) {
				arr.add(path);
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		return arr;
	}
}
