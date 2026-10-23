package com.idlesmoothie.view;

import java.net.URL;

import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;

/**
 * Plays the background music in a loop. The game runs silently if the music file is missing or cannot be played.
 */
public class MusicPlayer {

    private static final String MUSIC_PATH = "/audio/music.mp3";

    private final MediaPlayer player;

    /**
     * Loads the music, without starting it.
     */
    public MusicPlayer() {
        player = createPlayer();
    }

    /**
     * Starts or pauses the music. When it starts again, it goes on from where it was paused.
     *
     * @param playing whether the music plays
     */
    public void setPlaying(boolean playing) {
        if (player == null) {
            return;
        }
        if (playing) {
            player.play();
        } else {
            player.pause();
        }
    }

    /**
     * @param volume the volume of the music, from 0 (silent) to 1 (full volume)
     */
    public void setVolume(double volume) {
        if (player != null) {
            player.setVolume(volume);
        }
    }

    /**
     * @return the player looping the music, or {@code null} if there is no music to play
     */
    private MediaPlayer createPlayer() {
        URL url = getClass().getResource(MUSIC_PATH);
        if (url == null) {
            System.err.println("No music found at " + MUSIC_PATH + ", playing without music.");
            return null;
        }
        try {
            MediaPlayer music = new MediaPlayer(new Media(url.toExternalForm()));
            music.setCycleCount(MediaPlayer.INDEFINITE);
            music.setOnError(() -> System.err.println("Could not play the music: " + music.getError().getMessage()));
            return music;
        } catch (MediaException e) {
            System.err.println("Could not load the music: " + e.getMessage());
            return null;
        }
    }
}
