package RemuBotPackage;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;


import java.awt.*;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static RemuBotPackage.EVListener.skiptime;
import static RemuBotPackage.Main.jda;



public class YTPlayer extends ListenerAdapter {

    private static EmbedBuilder eb = new EmbedBuilder().setColor(new Color(0x95BDF8));


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


    // 유튜브 링크(또는 플레이리스트 링크) 플레이
    public void loadAndPlay(final VoiceChannel voiceChannel, final TextChannel channel, final String trackUrl) {


        evListener.playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                AudioTrack currentTrack = musicManager.player.getPlayingTrack();
                if(currentTrack == null) {
                    play(voiceChannel, channel.getGuild(), musicManager, track);
                    printNowPlaying(channel, musicManager);
                    if (musicManager.player.getPlayingTrack() != null) {
                        jda.getPresence().setActivity(Activity.listening(musicManager.player.getPlayingTrack().getInfo().title));
                    }


                } else {
                    musicManager.scheduler.queue(track);
                    AudioTrackInfo info = track.getInfo();
                    eb.setTitle("큐에 추가됨");
                    eb.addField("추가된 트랙",hyperLink(info.title, info.uri), false);
                    eb.addField("채널", info.author, false);
                    sending(channel, "f", true);
                }

            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {

                eb.setTitle("플레이리스트 추가됨");
                eb.addField("추가된 플레이리스트", hyperLink(playlist.getName(),trackUrl) , false);
                sending(channel, "f", true);

                if(musicManager.player.getPlayingTrack() == null) {
                    connectToVoiceChannel(voiceChannel, channel.getGuild().getAudioManager());
                    musicManager.scheduler.nextTrack();
                }

                for (AudioTrack t : playlist.getTracks()) {
                    musicManager.scheduler.queue(t);
                }
                if (musicManager.player.getPlayingTrack() != null) {
                    jda.getPresence().setActivity(Activity.listening(musicManager.player.getPlayingTrack().getInfo().title));
                }




            }

            @Override
            public void noMatches() {
                eb.addField("노래를 찾을 수 없음", trackUrl, false);
                sending(channel, "f", false);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                eb.addField("노래 로딩 실패", exception.getMessage(), false);
                sending(channel, "f", false);
            }
        });
    }



    public void playCommand (GuildMessageReceivedEvent event, String[] command) {
        // 명령어를 보낸 텍스트 채널
        TextChannel channel = event.getChannel();
        // 보낸 유저가 접속중인 보이스 채널
        VoiceChannel voiceChannel = event.getMember().getVoiceState().getChannel();






        // 보이스 채널에 미접속 중일 시 실행되지 않음.
        if(voiceChannel == null) {
            eb.setDescription("먼저 음성채널에 접속하세요");
            sending(channel, "d", false);
            return;
        }

        // play ~~
        if(command.length == 2) {
            // play 트랙번호
            if(command[1].matches("^[0-9]*$")) {
                int index = Integer.parseInt(command[1]) - 1;

                int size = musicManager.scheduler.getQueue().size();
                if(index > size - 1 || index < 0) {
                    eb.setDescription("잘못된 인덱스. 현재 재생목록 수 : " + size);
                    sending(channel, "d", false);
                    return;
                }
                AudioTrack selected = musicManager.scheduler.get(index);
                eb.setDescription((index + 1) + "번째 노래 재생");
                sending(channel, "d", false);
                musicManager.player.playTrack(selected);
                if (musicManager.player.getPlayingTrack() != null) {
                    jda.getPresence().setActivity(Activity.listening(musicManager.player.getPlayingTrack().getInfo().title));
                }


            // play 유튜브 링크
            } else {
                loadAndPlay(voiceChannel, channel, command[1]);
            }
        // play (none)
        } else if (command.length == 1) {
            if(musicManager.player.getPlayingTrack() == null) {
                musicManager.scheduler.nextTrack();
                if (musicManager.player.getPlayingTrack() != null) {
                    printNowPlaying(channel, musicManager);
                } else {
                    eb.setDescription("재생할 노래가 없습니다.");
                    sending(channel, "d", false);
                }

            }
        }
    }

    public String hyperLink(String content, String url) {
        return "[" + content + "](<" + url + ">)";
    }

    public String italic(String content) {
        return "*" + content + "*";
    }

    public String trackInfo(AudioTrack track) {
        AudioTrackInfo info = track.getInfo();
        String text = info.title + " - " + italic(timeFormatter(info.length, info.length));
        return hyperLink(text, info.uri);
    }

    public void sending(TextChannel channel, String type, boolean hasTitle) {

        switch (type) {
            // description
            case "d":
                channel.sendMessageEmbeds(eb.build()).queue();
                eb.setDescription(null);
                break;
            // field
            case "f":
                channel.sendMessageEmbeds(eb.build()).queue();
                eb.clearFields();
                break;
        }

        if(hasTitle) {
            eb.setTitle(null);
        }
    }

    public void delete (TextChannel channel, String[] command) {
        // delete ~~
        if(command.length == 2) {
            // delete number
            if(command[1].matches("^[0-9]*$")) {
                int index = Integer.parseInt(command[1]) - 1;
                int size = musicManager.scheduler.getQueue().size();

                // if index is wrong
                if(index > size - 1 || index < 0) {
                    eb.addField("잘못된 인덱스", "현재 재생목록 수 : " + size, false);
                    sending(channel, "f", false);
                    return;
                }

                AudioTrack deleted = musicManager.scheduler.get(index);

                eb.setTitle((index + 1) + "번째 노래 삭제됨");
                eb.addField("삭제된 트랙", trackInfo(deleted), false);
                sending(channel, "f", true);

            // delete index-index
            } else if(command[1].contains("-")) {
                String[] range = command[1].split("-");

                // if index is wrong
                if(range.length != 2) {
                    eb.setDescription("잘못된 명령어. del (index / start-end)");
                    sending(channel, "d", false);
                } else {
                    int size = musicManager.scheduler.getQueue().size();
                    int start = Integer.parseInt(range[0]) - 1;
                    int end = Integer.parseInt(range[1]) - 1;
                    if(start > size - 1 || start < 0) {
                        eb.setDescription("잘못된 인덱스. 현재 재생목록 수 : " + size);
                        sending(channel, "d", false);
                        return;
                    }
                    if(end > size - 1 || end < 0) {
                        eb.setDescription("잘못된 인덱스. 현재 재생목록 수 : " + size);
                        sending(channel, "d", false);
                        return;
                    }
                    if(start > end) {
                        eb.setDescription("잘못된 인덱스. 현재 재생목록 수 : " + size);
                        sending(channel, "d", false);
                        return;
                    }

                    for(int i = start; i <= end; i++) {
                        musicManager.scheduler.get(start);
                    }
                    eb.setTitle((start + 1) + " ~ " + (end + 1) + "번 곡이 삭제됨.");
                    eb.addField("삭제된 곡 수", "" + (end - start + 1), false);
                    sending(channel, "f", true);
                }
            }


        } else {
            eb.setDescription("잘못된 명령어. del (index / start-end)");
            sending(channel, "d", false);
        }
    }


    public void play(VoiceChannel voiceChannel, Guild guild, GuildMusicManager musicManager, AudioTrack track) {
        connectToVoiceChannel(voiceChannel, guild.getAudioManager());

        musicManager.scheduler.queue(track);

    }

    // print playing queue
    public void printNowPlaying(TextChannel channel, GuildMusicManager musicManager) {
        AudioTrack currentTrack = musicManager.player.getPlayingTrack();

        if(currentTrack == null || currentTrack.getState().equals(AudioTrackState.STOPPING)) {
            eb.setDescription("현재 재생중이 아닙니다.");
            sending(channel, "d", false);
            return;
        }

        AudioTrackInfo info = currentTrack.getInfo();

        String title = hyperLink(info.title, info.uri);
        String time = "";



        if(info.isStream) {
            time = "**실시간 스트리밍**";
        } else {
            time = timeFormatter(currentTrack.getPosition(), currentTrack.getDuration())
                    + " / "
                    + timeFormatter(info.length, currentTrack.getDuration());
        }
        if(musicManager.player.isPaused())
            time += italic("\n일시 중지됨");

        eb.setTitle("현재 재생중");
        eb.addField("트랙 명", title, false);
        eb.addField("재생 시간", time, false);
        sending(channel, "f", true);
    }


    public void printQueue(TextChannel channel, GuildMusicManager musicManager, String[] command) {
        eb.setTitle("현재 큐 목록");

        BlockingQueue<AudioTrack> que = musicManager.scheduler.getQueue();

        String[] strArr = new String[que.size()];
        AudioTrack currentTrack = musicManager.player.getPlayingTrack();

        Iterator<AudioTrack> trackIterator = musicManager.scheduler.getIterator();
        long sum = 0;
        String currentInfo;


        if(currentTrack == null || currentTrack.getState().equals(AudioTrackState.STOPPING)) {
            currentInfo = "재생 중이 아님.";
        } else {
            AudioTrackInfo info = currentTrack.getInfo();
            currentInfo = hyperLink(info.title, info.uri);
            if(currentTrack.getInfo().isStream) {
                currentInfo += "\n**실시간 스트리밍**";
            } else {
                currentInfo += "\n"
                        + timeFormatter(currentTrack.getPosition(), currentTrack.getDuration()) + " / "
                        + timeFormatter(currentTrack.getDuration(), currentTrack.getDuration());
            }
            if(musicManager.player.isPaused())
                currentInfo += "\n*일시 중지됨*";

            if(!(currentTrack.getInfo().isStream))
                sum += currentTrack.getDuration();
        }
        eb.addField("현재 재생중", currentInfo, false);

        if(que.size() <= 10) {
            eb.addField("현재 재생 목록",italic("전체 표시"),false);
        } else if(command.length == 1) {
            eb.addField("현재 재생 목록",italic("상위 10개만 표시, 전체 보기 : queue all"), false);
        } else if(command[1].equals("all")) {
            eb.addField("현재 재생 목록",italic("전체 표시"),false);
        }

        int index = 0;
        while (trackIterator.hasNext()) {
            AudioTrack track = trackIterator.next();
            AudioTrackInfo trackInfo = track.getInfo();
            if(!(trackInfo.isStream))
                sum += trackInfo.length;
            strArr[index] = hyperLink(trackInfo.title, trackInfo.uri) + " - ";
            if(trackInfo.isStream) {
                strArr[index] += italic("실시간 스트리밍");
            } else {
                strArr[index] += italic(timeFormatter(trackInfo.length, trackInfo.length));
            }
            index++;
        }


        String sumStr = "\n\n**총 갯수(재생 중 제외) : \n**" + (que.size())
                + "\n\n**총 길이(재생 중 포함) : \n**" + timeFormatter(sum, sum);

        String result;
        if(strArr.length <= 10) {
            result = "";
            for (int i = 0; i < strArr.length; i++) {
                result += (i + 1) + ". ";
                result += strArr[i] += "\n\n\n";
            }
            result += sumStr;

            eb.addField("\n\n", result, false);
            sending(channel, "f", true);

        } else if(command.length == 1) {
            result = "";
            for (int i = 0; i < 10; i++) {
                result += (i + 1) + ". ";
                result += strArr[i] += "\n\n\n";
            }
            result += sumStr;

            eb.addField("\n\n", result, false);
            sending(channel, "f", true);
        }
        else {
            result = "";
            for (int i = 0; i < strArr.length; i++) {
                result += i + 1 + ". ";
                result += strArr[i] += "\n\n\n";
                if(i == strArr.length - 1) {
                    result += sumStr;
                    eb.addField("\n\n", result, false);
                    sending(channel, "f", true);
                    break;
                }
                if(i > 1 && (i + 1) % 7 == 0) {
                    eb.addField("\n\n", result, false);
                    sending(channel, "f", true);
                    result = "";
                }
            }
        }
    }

    // TODO : Make code pretty under

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
