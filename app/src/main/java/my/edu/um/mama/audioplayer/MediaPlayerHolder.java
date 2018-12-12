package my.edu.um.mama.audioplayer;

import android.content.Context;
import android.media.MediaPlayer;
import android.content.res.AssetFileDescriptor;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class MediaPlayerHolder implements PlayerAdapter {
    public int currentMedia=0;
    public static final int PLAYBACK_POSITION_REFRESH_INTERVAL_MS = 1000;

    private final Context mContext;
    private MediaPlayer mMediaPlayer;
    private int mResourceId;
    private PlaybackInfoListener mPlaybackInfoListener;
    private ScheduledExecutorService mExecutor;
    private Runnable mSeekbarPositionUpdateTask;
    private ArrayList<Integer> mResourceList = new ArrayList<>();

    public MediaPlayerHolder(Context context){
        mContext = context.getApplicationContext();
    }
    
    private void initializeMediaPlayer(){
        if(mMediaPlayer == null){
            mMediaPlayer=new MediaPlayer();
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
                @Override
                public void onCompletion(MediaPlayer mediaPlayer){
                    stopUpdatingCallbackWithPosition(true);
                    logToUI("MediaPlayer playback completed");
                    if(mPlaybackInfoListener != null){
                        mPlaybackInfoListener.onStateChanged(PlaybackInfoListener.State.COMPLETED);
                        mPlaybackInfoListener.onPlaybackCompleted();
                    }
                }
            });
            logToUI("mMediaPlayer = new MediaPlayer()");
        }
    }
    
    public void setPlaybackInfoListener(PlaybackInfoListener listener){
        mPlaybackInfoListener = listener;
    }

    @Override
    public void setMediaList(int[] resourcesIdList){
        mResourceList.clear();
        for(int resource : resourcesIdList){
            mResourceList.add(resource);
        }
    }
    
    @Override
    public void loadMedia() {
        mResourceId = mResourceList.get(currentMedia%mResourceList.size());

        initializeMediaPlayer();
        
        AssetFileDescriptor assetFileDescriptor = mContext.getResources().openRawResourceFd(mResourceId);
        try{
            logToUI("load() {1. setDataSource}");
            mMediaPlayer.setDataSource(assetFileDescriptor);
        }catch(Exception e){
            logToUI(e.toString());
        }
        
        try{
            logToUI("load() {1. setDataSource}");
            mMediaPlayer.prepare();
        }catch(Exception e){
            logToUI(e.toString());
        }
        
        initializeProgressCallback();
        logToUI("initializeProgressCallback()");
    }

    @Override
    public void release() {
        if(mMediaPlayer != null){
            logToUI("release() and mMediaPlayer = null");
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    public boolean isPlaying() {
        if(mMediaPlayer != null){
            return mMediaPlayer.isPlaying();
        }
        return false;
    }

    @Override
    public void play() {
        if(mMediaPlayer != null && !mMediaPlayer.isPlaying()){
            logToUI(String.format("playbackStart() %s",mContext.getResources().getResourceEntryName(mResourceId)));
            mMediaPlayer.start();
            if(mPlaybackInfoListener != null){
                mPlaybackInfoListener.onStateChanged(PlaybackInfoListener.State.PLAYING);
            }
            startUpdatingCallbackWithPosition();
        }
    }

    @Override
    public void next(){
        if(mMediaPlayer != null){
            logToUI(String.format("nextSong"));
            currentMedia++;
            reset();
            play();
        }
    }

    @Override
    public void reset() {
        if(mMediaPlayer != null){
            logToUI("playbackReset()");
            mMediaPlayer.reset();
            loadMedia();
            if(mPlaybackInfoListener!=null){
                mPlaybackInfoListener.onStateChanged(PlaybackInfoListener.State.RESET);
            }
            stopUpdatingCallbackWithPosition(true);
        }
    }

    @Override
    public void pause() {
        if(mMediaPlayer != null && mMediaPlayer.isPlaying()){
            mMediaPlayer.pause();
            if(mPlaybackInfoListener != null){
                mPlaybackInfoListener.onStateChanged(PlaybackInfoListener.State.PAUSED);
            }
            logToUI("playbackPause()");
        }
    }

    @Override
    public void seekTo(int position) {
        if(mMediaPlayer!=null){
            logToUI(String.format("seekTo() %d ms", position));
            mMediaPlayer.seekTo(position);
        }
    }

    private void startUpdatingCallbackWithPosition(){
        if(mExecutor == null){
            mExecutor = Executors.newSingleThreadScheduledExecutor();
        }
        if(mSeekbarPositionUpdateTask == null){
            mSeekbarPositionUpdateTask = new Runnable(){
                @Override
                public void run(){
                    updateProgressCallbackTask();
                }
            };
        }
        mExecutor.scheduleAtFixedRate(
                mSeekbarPositionUpdateTask,
                0,
                PLAYBACK_POSITION_REFRESH_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    private void stopUpdatingCallbackWithPosition(boolean resetUIPlaybackPosition){
        if(mExecutor!=null){
            mExecutor.shutdownNow();
            mExecutor = null;
            mSeekbarPositionUpdateTask = null;
            if(resetUIPlaybackPosition && mPlaybackInfoListener != null){
                mPlaybackInfoListener.onPositionChanged(0);
            }
        }
    }

    private void updateProgressCallbackTask(){
        if(mMediaPlayer != null && mMediaPlayer.isPlaying()){
            int currentPosition = mMediaPlayer.getCurrentPosition();
            if(mPlaybackInfoListener != null){
                mPlaybackInfoListener.onPositionChanged(currentPosition);
            }
        }
    }

    @Override
    public void initializeProgressCallback() {
        final int duration = mMediaPlayer.getDuration();
        if(mPlaybackInfoListener != null){
            mPlaybackInfoListener.onDurationChanged(duration);
            mPlaybackInfoListener.onPositionChanged(0);
            logToUI(String.format("firing setPlaybackDuration(%d sec)",TimeUnit.MILLISECONDS.toSeconds(duration)));
            logToUI("firing setPlaybackPosition(0)");
        }
    }

    private void logToUI(String message){
        if(mPlaybackInfoListener != null){
            mPlaybackInfoListener.onLogUpdated(message);
        }
    }
}
