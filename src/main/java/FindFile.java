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

	public static List<Path> findFilesByExt(final String location, final String sysBeginPart, final String extension) {
		final var arr = new ArrayList<Path>();
		final String extension0 = "." + extension;

		try {
			Files.newDirectoryStream(Paths.get(location), path -> {
				if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
					return path.toString().endsWith(extension0)
							&& !path.getFileName().toString().startsWith(sysBeginPart);
				}
				return false;
			}).forEach(path -> arr.add(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return arr;
	}
}
