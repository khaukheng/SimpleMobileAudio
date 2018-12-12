package my.edu.um.mama.audioplayer;

public interface PlayerAdapter {

    void loadMedia();
    void setMediaList(int[] resourcesIdList);
    void release();
    boolean isPlaying();
    void play();
    void reset();
    void pause();
    void next();
    void initializeProgressCallback();
    void seekTo(int position);
}
