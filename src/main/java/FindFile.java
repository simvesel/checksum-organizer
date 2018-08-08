import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FindFile {

	private FindFile() {
	}

	private static boolean innerSearchFunc(final Path path, final String sysBeginPart, final String extension) {
		if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
			return path.toString().endsWith(extension) && !path.getFileName().toString().startsWith(sysBeginPart);
		}
		return false;
	}

	public static List<Path> findFilesByOneExtWithoutSysfiles(final String location, final String sysBeginPart,
			final String extension) {
		final var arr = new ArrayList<Path>();
		final String extension0 = "." + extension;

		try (var dirStream = Files.newDirectoryStream(Paths.get(location),
				path -> innerSearchFunc(path, sysBeginPart, extension0))) {
			dirStream.forEach(path -> arr.add(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return arr;
	}
}
