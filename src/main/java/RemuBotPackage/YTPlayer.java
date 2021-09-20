package RemuBotPackage;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;


import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static RemuBotPackage.EVListener.skiptime;
import static RemuBotPackage.Main.jda;


public class YTPlayer extends ListenerAdapter {


    private static final EVListener evListener = new EVListener();
    GuildMusicManager musicManager = null;

    public static String timeFormatter(long timeLong, long duration) {

        if(duration >= 3600000) {
            return String.format("%d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(timeLong),
                    TimeUnit.MILLISECONDS.toMinutes(timeLong) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.MILLISECONDS.toSeconds(timeLong) % TimeUnit.MINUTES.toSeconds(1));
        } else if(duration >= 60000) {
            return String.format("%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(timeLong),
                    TimeUnit.MILLISECONDS.toSeconds(timeLong) % TimeUnit.MINUTES.toSeconds(1));
        } else {
            return String.format("%02d",
                    TimeUnit.MILLISECONDS.toSeconds(timeLong));
        }
    }



    public void loadAndPlay(final VoiceChannel voiceChannel, final TextChannel channel, final String trackUrl) {


        evListener.playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                AudioTrack currentTrack = musicManager.player.getPlayingTrack();
                if(currentTrack == null) {
                    play(voiceChannel, channel.getGuild(), musicManager, track);
                    printNowPlaying(channel, musicManager);
                    if (musicManager.player.getPlayingTrack() != null) {
                        jda.getPresence().setActivity(Activity.playing(musicManager.player.getPlayingTrack().getInfo().title));
                    }


                } else {
                    musicManager.scheduler.queue(track);
                    channel.sendMessage("```노래 추가됨 : " + track.getInfo().title + "```").queue();
                }

            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {

                channel.sendMessage("```플레이리스트 추가 완료 : " + playlist.getName() + "```").queue();

                if(musicManager.player.getPlayingTrack() == null) {
                    connectToVoiceChannel(voiceChannel, channel.getGuild().getAudioManager());
                    musicManager.scheduler.nextTrack();
                }

                for (AudioTrack t : playlist.getTracks()) {
                    musicManager.scheduler.queue(t);
                }
                if (musicManager.player.getPlayingTrack() != null) {
                    jda.getPresence().setActivity(Activity.playing(musicManager.player.getPlayingTrack().getInfo().title));
                }




            }

            @Override
            public void noMatches() {
                channel.sendMessage("```노래를 찾을 수 없음 : \n" + trackUrl + "```").queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                channel.sendMessage("```노래 로딩 실패 : " + exception.getMessage() + "```").queue();
            }
        });
    }

    public void playCommand (VoiceChannel voiceChannel, TextChannel channel, String[] command, AudioTrack currentTrack) {
        if(voiceChannel == null) {
            channel.sendMessage("```먼저 음성채널에 접속하세요```").queue();
            return;
        }
        if(command.length == 2) {
            if(command[1].matches("^[0-9]*$")) {
                int index = Integer.parseInt(command[1]) - 1;

                int size = musicManager.scheduler.getQueue().size();
                if(index > size - 1 || index < 0) {
                    channel.sendMessage("```잘못된 인덱스. 현재 재생목록 수 : " + size + "```").queue();
                    return;
                }
                AudioTrack deleted = musicManager.scheduler.get(index);
                channel.sendMessage("```" + (index + 1) + "번째 노래 재생```").queue();
                musicManager.player.playTrack(deleted);
                if (musicManager.player.getPlayingTrack() != null) {
                    jda.getPresence().setActivity(Activity.playing(musicManager.player.getPlayingTrack().getInfo().title));
                }



            } else {
                loadAndPlay(voiceChannel, channel, command[1]);
            }
        } else if (command.length == 1) {
            if(musicManager.player.getPlayingTrack() == null) {
                Iterator<AudioTrack> iterator = musicManager.scheduler.getIterator();
                if(iterator.hasNext()) {
                    musicManager.scheduler.nextTrack();
                    printNowPlaying(channel, musicManager);
                } else {
                    channel.sendMessage("```재생할 노래가 없습니다.```").queue();
                }
            }
        }
    }

    public void delete (TextChannel channel, String[] command) {
        if(command.length == 2) {
            if(command[1].matches("^[0-9]*$")) {
                int index = Integer.parseInt(command[1]) - 1;
                int size = musicManager.scheduler.getQueue().size();
                if(index > size - 1 || index < 0) {
                    channel.sendMessage("```잘못된 인덱스. 현재 재생목록 수 : " + size + "```").queue();
                    return;
                }
                AudioTrack deleted = musicManager.scheduler.get(index);
                channel.sendMessage("```" + (index + 1) + "번째 노래 삭제됨\n" +
                        "삭제된 트랙 :\n" +
                        deleted.getInfo().title + " - <" +
                        timeFormatter(deleted.getInfo().length, deleted.getInfo().length)  +
                        ">```" ).queue();
            } else if(command[1].contains("-")) {
                String[] range = command[1].split("-");
                if(range.length != 2) {
                    channel.sendMessage("```잘못된 명령어. del (index / start-end) ```").queue();
                    return;
                } else {
                    int size = musicManager.scheduler.getQueue().size();
                    int start = Integer.parseInt(range[0]) - 1;
                    int end = Integer.parseInt(range[1]) - 1;
                    if(start > size - 1 || start < 0) {
                        channel.sendMessage("```잘못된 인덱스. 현재 재생목록 수 : " + size + "```").queue();
                        return;
                    }
                    if(end > size - 1 || end < 0) {
                        channel.sendMessage("```잘못된 인덱스. 현재 재생목록 수 : " + size + "```").queue();
                        return;
                    }
                    if(start > end) {
                        channel.sendMessage("```잘못된 인덱스. 현재 재생목록 수 : " + size + "```").queue();
                        return;
                    }

                    for(int i = start; i <= end; i++) {
                        musicManager.scheduler.get(start);
                    }
                    channel.sendMessage("```" + (start + 1) + " ~ " + (end + 1) + "번 곡이 삭제됨.\n" +
                        "삭제된 곡 수 : " + (end - start + 1) + "```").queue();
                }
            }


        } else {
            channel.sendMessage("```잘못된 명령어. del (index / start-end) ```").queue();
        }
    }


    public void play(VoiceChannel voiceChannel, Guild guild, GuildMusicManager musicManager, AudioTrack track) {
        connectToVoiceChannel(voiceChannel, guild.getAudioManager());

        musicManager.scheduler.queue(track);

    }

    public void printNowPlaying(TextChannel channel, GuildMusicManager musicManager) {
        AudioTrack currentTrack = musicManager.player.getPlayingTrack();

        if(currentTrack == null || currentTrack.getState().equals(AudioTrackState.STOPPING)) {
            channel.sendMessage("```현재 재생중이 아닙니다.```").queue();
            return;
        }
        AudioTrackInfo info = currentTrack.getInfo();
        StringBuilder msg = new StringBuilder("```현재 재생중 : \n");
        msg.append("제목 : ").append(info.title).append("\n")
                .append("By : ").append(info.author).append("\n");
        if(info.isStream) {
            msg.append("실시간 스트리밍\n");
        } else {
            msg.append(timeFormatter(currentTrack.getPosition(), currentTrack.getDuration()))
                    .append(" / ")
                    .append(timeFormatter(info.length, currentTrack.getDuration())).append("\n");
        }
        if(musicManager.player.isPaused())
            msg.append("*일시 중지됨*\n");
        channel.sendMessage(msg + "```").queue();
    }


    public void printQueue(TextChannel channel, GuildMusicManager musicManager, String[] command) {
        BlockingQueue<AudioTrack> que = musicManager.scheduler.getQueue();

        String[] strArr = new String[que.size()];
        AudioTrack currentTrack = musicManager.player.getPlayingTrack();

        Iterator<AudioTrack> trackIterator = musicManager.scheduler.getIterator();
        long sum = 0;
        String startStr;
        if(currentTrack == null || currentTrack.getState().equals(AudioTrackState.STOPPING)) {
            startStr = "```md\n현재 재생중 : 재생 중이 아님.\n\n";
        } else {
            startStr = "```md\n현재 재생중 : " + currentTrack.getInfo().title;
            if(currentTrack.getInfo().isStream) {
                startStr += " - <실시간 스트리밍>\n";
            } else {
                startStr += " - <"
                        + timeFormatter(currentTrack.getPosition(), currentTrack.getDuration()) + "> / <"
                        + timeFormatter(currentTrack.getDuration(), currentTrack.getDuration()) + ">\n";
            }
            if(musicManager.player.isPaused())
                startStr += "*일시 중지됨*";
            startStr += "\n";
            if(!(currentTrack.getInfo().isStream))
                sum += currentTrack.getDuration();
        }

        if(que.size() <= 20) {
            startStr += "현재 재생 목록 : \n\n";
        } else if(command.length == 1) {
            startStr += "현재 재생 목록 : \n(상위 20개만 표시, 전체 보기 : queue all)\n\n";
        } else if(command[1].equals("all")) {
            startStr += "현재 재생 목록 : \n\n";
        }

        int index = 0;
        while (trackIterator.hasNext()) {
            AudioTrack track = trackIterator.next();
            AudioTrackInfo trackInfo = track.getInfo();
            if(!(trackInfo.isStream))
                sum += trackInfo.length;
            strArr[index] = trackInfo.title + " - <";
            if(trackInfo.isStream) {
                strArr[index] += "실시간 스트리밍";
            } else {
                strArr[index] += timeFormatter(trackInfo.length, trackInfo.length);
            }
            strArr[index] += ">\n\n";
            index++;
        }



        String sumStr = "\n\n총 갯수(재생 중 제외) : " + (que.size())
                + "\n총 길이(재생 중 포함) : <" + timeFormatter(sum, sum) + ">\n```";

        String result;
        if(strArr.length <= 20) {
            result = startStr;
            for (int i = 0; i < strArr.length; i++) {
                result += (i + 1) + ". ";
                result += strArr[i];
            }
            result += sumStr;

            channel.sendMessage(result).queue();
        } else if(command.length == 1) {
            result = startStr;
            for (int i = 0; i < 20; i++) {
                result += i + 1 + ". ";
                result += strArr[i];
            }
            result += sumStr;

            channel.sendMessage(result).queue();
        }
        else {
            result = startStr;
            for (int i = 0; i < strArr.length; i++) {
                result += i + 1 + ". ";
                result += strArr[i];
                if(i == strArr.length - 1) {
                    result += sumStr;

                    channel.sendMessage(result).queue();
                    break;
                }
                if(i > 1 && (i + 1) % 20 == 0) {
                    result += "```";

                    channel.sendMessage(result).queue();
                    result = "```md\n";
                }
            }
        }
    }


    public void skipTrack(TextChannel channel) {

        channel.sendMessage("```스킵```").queue();
        musicManager.scheduler.nextTrack();


    }

    public void jumper(TextChannel channel, String timeFormat) {

        AudioTrack currentTrack = musicManager.player.getPlayingTrack();
        long duration = currentTrack.getDuration();

        long time;

        if(!timeFormat.contains(":") && timeFormat.matches("^[0-9]*$")) {
            time = secondToLong(timeFormat);
            if(time >= duration) {
                channel.sendMessage("```오류 : 올바른 시간을 선택해 주세요. \n*음악 길이 : " + timeFormatter(currentTrack.getDuration(), currentTrack.getDuration()) + "*```" ).queue();
                return;
            }

            currentTrack.setPosition(time);

            channel.sendMessage("```" + timeFormat + "초로" + "\n"
                    + timeFormatter(time, currentTrack.getDuration()) + " / " +
                    timeFormatter(currentTrack.getDuration(), currentTrack.getDuration()) + "```").queue();

            System.out.println("Jump to " + timeFormatter(time, currentTrack.getDuration()));
            return;
        }

        String[] timeArr = timeFormat.split(":");

        //h[0]:m[1]:s[2]
        switch (timeArr.length) {

            case 2:
                time = minuteToLong(timeArr[0]) + secondToLong(timeArr[1]);
                if(time >= duration) {
                    channel.sendMessage("```오류 : 올바른 시간을 선택해 주세요. \n*음악 길이 : " + timeFormatter(currentTrack.getDuration(), currentTrack.getDuration()) + "*```").queue();
                    return;
                }
                currentTrack.setPosition(time);

                channel.sendMessage("```" + timeArr[0] + "분 " + timeArr[1] + "초로"  + "\n"
                        + timeFormatter(time, currentTrack.getDuration()) + " / " +
                        timeFormatter(currentTrack.getDuration(), currentTrack.getDuration()) + "```").queue();

                System.out.println("Jump to " + timeFormatter(time, currentTrack.getDuration()));
                return;

            case 3:
                time = hourToLong(timeArr[0]) + minuteToLong(timeArr[1]) + secondToLong(timeArr[2]);
                if(time >= duration) {
                    channel.sendMessage("```오류 : 올바른 시간을 선택해 주세요. \n*음악 길이 : " + timeFormatter(currentTrack.getDuration(), currentTrack.getDuration()) + "*```").queue();
                    return;
                }
                currentTrack.setPosition(time);

                channel.sendMessage("```" + timeArr[0] + "시간 " + timeArr[1] + "분" + timeArr[2] + "초로" + "\n"
                        + timeFormatter(time, currentTrack.getDuration()) + " / " +
                        timeFormatter(currentTrack.getDuration(), currentTrack.getDuration()) + "```").queue();
                System.out.println("Seek to " + timeFormatter(time, currentTrack.getDuration()));
                return;
            default:
                channel.sendMessage("```시간 형식 잘못됨. *시:분:초*```").queue();
        }

    }


    public void skipper(TextChannel channel, String timeFormat) {


        AudioTrack currentTrack = musicManager.player.getPlayingTrack();
        long duration = currentTrack.getDuration();

        long time;

        if(!timeFormat.contains(":") && timeFormat.matches("^[0-9]*$")) {
            time = currentTrack.getPosition() + secondToLong(timeFormat);
            currentTrack.setPosition(time);
            if(time >= duration) {
                channel.sendMessage("```재생 끝```").queue();
                return;
            }
            channel.sendMessage("```" + timeFormat + "초 뒤로"  + "\n"
                    + timeFormatter(time, currentTrack.getDuration()) + " / " +
                    timeFormatter(currentTrack.getDuration(), currentTrack.getDuration()) + "```").queue();
            System.out.println("Seek to " + timeFormatter(time, currentTrack.getDuration()));
            return;
        }

        String[] timeArr = timeFormat.split(":");

        //h[0]:m[1]:s[2]
        switch (timeArr.length) {

            case 2:
                time = currentTrack.getPosition() + minuteToLong(timeArr[0]) + secondToLong(timeArr[1]);
                currentTrack.setPosition(time);
                if(time >= duration) {
                    channel.sendMessage("```재생 끝```").queue();
                    return;
                }
                channel.sendMessage("```" + timeArr[0] + "분 " + timeArr[1] + "초 뒤로" + "\n"
                        + timeFormatter(time, currentTrack.getDuration()) + " / " +
                        timeFormatter(currentTrack.getDuration(), currentTrack.getDuration()) + "```").queue();;

                System.out.println("Seek to " + timeFormatter(time, currentTrack.getDuration()));
                return;

            case 3:
                time = currentTrack.getPosition() + hourToLong(timeArr[0]) + minuteToLong(timeArr[1]) + secondToLong(timeArr[2]);
                currentTrack.setPosition(time);
                if(time >= duration) {
                    channel.sendMessage("```재생 끝```").queue();
                    return;
                }
                channel.sendMessage("```" +timeArr[0] + "시간 " + timeArr[1] + "분 " + timeArr[2] + "초 뒤로" + "\n"
                        + timeFormatter(time, currentTrack.getDuration()) + " / " +
                        timeFormatter(currentTrack.getDuration(), currentTrack.getDuration()) + "```").queue();;

                System.out.println("Seek to " + timeFormatter(time, currentTrack.getDuration()));
                return;
            default:
                channel.sendMessage("```시간 형식 잘못됨. *시:분:초*```").queue();
        }

    }


    public void skipBySkiptime(TextChannel channel) {

        AudioTrack currentTrack = musicManager.player.getPlayingTrack();
        long duration = currentTrack.getDuration();

        long time = currentTrack.getPosition() + TimeUnit.SECONDS.toMillis(skiptime);
        currentTrack.setPosition(time);


        if(time >= duration) {
            channel.sendMessage("```재생 끝```").queue();
            return;
        }

        channel.sendMessage("```" + skiptime + "초 뒤로" + "\n"
                + timeFormatter(time, currentTrack.getDuration()) + " / " +
                timeFormatter(currentTrack.getDuration(), currentTrack.getDuration()) + "```").queue();

        System.out.println("Seek to " + timeFormatter(time, currentTrack.getDuration()));

    }

    public long secondToLong(String secondStr) {

        return TimeUnit.SECONDS.toMillis(Long.parseLong(secondStr));
    }

    public long minuteToLong (String minuteStr) {

        return TimeUnit.MINUTES.toMillis(Long.parseLong(minuteStr));
    }

    public long hourToLong(String hourStr) {

        return TimeUnit.HOURS.toMillis(Long.parseLong(hourStr));
    }


    public static void connectToVoiceChannel(VoiceChannel voiceChannel, AudioManager audioManager) {
        audioManager.openAudioConnection(voiceChannel);
    }
}
