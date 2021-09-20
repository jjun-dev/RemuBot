package RemuBotPackage;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.api.entities.Activity;

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


import static RemuBotPackage.Main.jda;


/**
 * This class schedules tracks for the audio player. It contains the queue of tracks.
 */
public class TrackScheduler extends AudioEventAdapter {
    private final AudioPlayer player;
    private final BlockingQueue<AudioTrack> queue;
    private final YTPlayer ytPlayer = EVListener.ytPlayer;



    /**
     * @param player The audio player this scheduler uses
     */
    public TrackScheduler(AudioPlayer player) {
        this.player = player;
        this.queue = new LinkedBlockingQueue<>();

    }

    public Iterator<AudioTrack> getIterator() {
        return queue.iterator();
    }

    public BlockingQueue<AudioTrack> getQueue() {
        return queue;
    }

    /**
     * Add the next track to queue or play right away if nothing is in the queue.
     *
     * @param track The track to play or add to queue.
     */
    public void queue(AudioTrack track) {
        // Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
        // something is playing, it returns false and does nothing. In that case the player was already playing so this
        // track goes to the queue instead.
        if (!player.startTrack(track, true)) {
            queue.offer(track);

        }
    }

    public void pause() {
        player.setPaused(true);
    }

    public void resume() {
        player.setPaused(false);
    }

    public AudioTrack get(int index) {
        synchronized (queue) {

            int size = queue.size();
            if (index < 0 || size < index + 1) {
                return null;
            }

            AudioTrack element = null;
            for (int i = 0; i < size; i++) {
                if (i == index) {
                    element = queue.remove();
                } else {
                    queue.add(queue.remove());
                }
            }

            return element;
        }
    }

    /**
     * Start the next track, stopping the current one if it is playing.
     */
    public void nextTrack() {
        // Start the next track, regardless of if something is already playing or not. In case queue was empty, we are
        // giving null to startTrack, which is a valid argument and will simply stop the player.
        try {
            player.startTrack(queue.poll(), false);
        } catch (IllegalStateException e) {
            player.playTrack(player.getPlayingTrack().makeClone());
        }

        if (ytPlayer.musicManager.player.getPlayingTrack() != null) {
            jda.getPresence().setActivity(Activity.listening(player.getPlayingTrack().getInfo().title));
        } else {
            jda.getPresence().setActivity(Activity.playing("-help"));
        }

    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        // Only start the next track if the end reason is suitable for it (FINISHED or LOAD_FAILED)
        if (endReason.mayStartNext) {
            nextTrack();
        }

    }


}