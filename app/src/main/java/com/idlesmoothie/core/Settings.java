package com.idlesmoothie.core;

/**
 * The player's preferences. They are saved apart from the game, so resetting the progress keeps them.
 * A setting missing from the file keeps its default value.
 */
public final class Settings {

    private boolean managerAura = true;
    private boolean music = true;
    private double musicVolume = 0.5;
    private NumberNotation notation = NumberNotation.SUFFIXES;

    /**
     * @return whether a glow of the rarity color is drawn behind hired managers
     */
    public boolean isManagerAura() {
        return managerAura;
    }

    /**
     * @param managerAura whether a glow of the rarity color is drawn behind hired managers
     */
    public void setManagerAura(boolean managerAura) {
        this.managerAura = managerAura;
    }

    /**
     * @return whether the background music plays
     */
    public boolean isMusic() {
        return music;
    }

    /**
     * @param music whether the background music plays
     */
    public void setMusic(boolean music) {
        this.music = music;
    }

    /**
     * @return the volume of the music, from 0 (silent) to 1 (full volume), 0.5 by default
     */
    public double getMusicVolume() {
        return Math.clamp(musicVolume, 0, 1);
    }

    /**
     * @param musicVolume the volume of the music, from 0 (silent) to 1 (full volume)
     * @throws IllegalArgumentException if {@code musicVolume} is not between 0 and 1
     */
    public void setMusicVolume(double musicVolume) {
        if (!(musicVolume >= 0 && musicVolume <= 1)) {
            throw new IllegalArgumentException("Music volume must be between 0 and 1");
        }
        this.musicVolume = musicVolume;
    }

    /**
     * @return how large numbers are written, {@link NumberNotation#SUFFIXES} by default
     */
    public NumberNotation getNotation() {
        return notation == null ? NumberNotation.SUFFIXES : notation;
    }

    /**
     * @param notation how large numbers are written
     * @throws IllegalArgumentException if {@code notation} is {@code null}
     */
    public void setNotation(NumberNotation notation) {
        if (notation == null) {
            throw new IllegalArgumentException("Notation must not be null");
        }
        this.notation = notation;
    }
}
