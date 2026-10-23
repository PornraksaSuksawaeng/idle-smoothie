package com.idlesmoothie.view;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javafx.scene.image.Image;
import javafx.scene.text.Font;

/**
 * The images and fonts of the game, each read from the classpath once and kept.
 * <p>
 * The loading screen fills it off the JavaFX thread; the views then find everything ready.
 * Anything not preloaded is still read the first time it is asked for. A file that does not exist
 * is remembered as missing, and the caller draws its placeholder.
 */
public final class Assets {

    private static final Map<String, Optional<Image>> IMAGES = new ConcurrentHashMap<>();
    private static final Map<String, Optional<Font>> FONTS = new ConcurrentHashMap<>();

    private Assets() {
    }

    /**
     * @param path classpath path of the image, such as {@code "/images/mine/bar.png"}
     * @return the image at its full size, or {@code null} if the file does not exist
     */
    public static Image image(String path) {
        return IMAGES.computeIfAbsent(path, key -> read(path, 0)).orElse(null);
    }

    /**
     * @param path   classpath path of the image
     * @param height height to scale the image to, keeping its proportions, smoothly
     * @return the scaled image, or {@code null} if the file does not exist
     */
    public static Image image(String path, double height) {
        return IMAGES.computeIfAbsent(path + "@" + height, key -> read(path, height)).orElse(null);
    }

    /**
     * Loads the font, which also makes its family available to the stylesheets.
     *
     * @param path classpath path of the font file
     * @param size size of the font, in points
     * @return the font, or {@code null} if the file does not exist or cannot be read
     */
    public static Font font(String path, double size) {
        return FONTS.computeIfAbsent(path + "@" + size, key -> {
            try (InputStream stream = Assets.class.getResourceAsStream(path)) {
                return Optional.ofNullable(stream == null ? null : Font.loadFont(stream, size));
            } catch (IOException e) {
                return Optional.empty();
            }
        }).orElse(null);
    }

    /**
     * Reads every PNG and JPEG image of a classpath folder at full size, so that the views find them ready.
     *
     * @param folder classpath folder, such as {@code "/images/mine"}
     * @return the number of images read
     * @throws IOException if the folder cannot be listed
     */
    public static int preloadFolder(String folder) throws IOException {
        List<String> paths = listImages(folder);
        for (String path : paths) {
            image(path);
        }
        return paths.size();
    }

    private static Optional<Image> read(String path, double height) {
        try (InputStream stream = Assets.class.getResourceAsStream(path)) {
            if (stream == null) {
                return Optional.empty();
            }
            Image image = height > 0 ? new Image(stream, 0, height, true, true) : new Image(stream);
            return image.isError() ? Optional.empty() : Optional.of(image);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * Lists the images of a classpath folder, whether the game runs from the build folders or from a jar.
     */
    private static List<String> listImages(String folder) throws IOException {
        URL url = Assets.class.getResource(folder);
        if (url == null) {
            return List.of();
        }
        try {
            URI uri = url.toURI();
            if ("jar".equals(uri.getScheme())) {
                try (FileSystem jar = FileSystems.newFileSystem(uri, Map.of())) {
                    return listImages(jar.getPath(folder), folder);
                } catch (FileSystemAlreadyExistsException e) {
                    return listImages(FileSystems.getFileSystem(uri).getPath(folder), folder);
                }
            }
            return listImages(Path.of(uri), folder);
        } catch (URISyntaxException e) {
            throw new IOException("Cannot list " + folder, e);
        }
    }

    private static List<String> listImages(Path directory, String folder) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files.map(file -> file.getFileName().toString())
                    .filter(name -> name.endsWith(".png") || name.endsWith(".jpg"))
                    .sorted()
                    .map(name -> folder + "/" + name)
                    .toList();
        }
    }
}
