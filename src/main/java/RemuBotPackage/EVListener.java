package RemuBotPackage;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static RemuBotPackage.Main.jda;


public class EVListener extends ListenerAdapter {

    private static char prefix = '-';
    public static long skiptime = 10;

    public static final YTPlayer ytPlayer = new YTPlayer();

    public final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;

    public static TextChannel channel;


    private static final HashMap<User, ArrayList<SavedQueue>> savedPlaylists = new HashMap<>();


    public EVListener() {
        this.musicManagers = new HashMap<>();

        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
    }



    public synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
        long guildId = Long.parseLong(guild.getId());
        GuildMusicManager musicManager = musicManagers.get(guildId);

        if (musicManager == null) {
            musicManager = new GuildMusicManager(playerManager);
            musicManagers.put(guildId, musicManager);
        }

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        channel = event.getChannel();
        Guild guild = channel.getGuild();
        Member member = event.getMember();
        User user = member.getUser();
        VoiceChannel voiceChannel = member.getVoiceState().getChannel();
        AudioManager audioManager = guild.getAudioManager();
        String content = event.getMessage().getContentRaw();
        ytPlayer.musicManager = getGuildAudioPlayer(guild);
        GuildMusicManager musicManager = ytPlayer.musicManager;
        AudioTrack currentTrack = musicManager.player.getPlayingTrack();



        if(member.getUser().isBot())
            return;
        if(content.length() == 0)
            return;

        if(content.charAt(0) != prefix)
            return;

        String[] command = content.substring(1).split(" ");

        System.out.println(content);

        switch (command[0]) {
            case "p" :
            case "play" :
                //ytPlayer.playCommand(voiceChannel, channel, command);
                ytPlayer.playCommand(event, command);
                break;
            case "s":
            case "skip" :
                ytPlayer.skipTrack(event.getChannel());
                break;
            case "q":
            case "queue":
                if(!((command.length == 1) || (command.length == 2 && (command[1].equals("all") || command[1].equals("clear"))))) {
                    channel.sendMessage("```잘못된 명령어.\n" +
                            "queue (none / all / clear)```").queue();
                    break;
                } else if (command.length == 2 && command[1].equals("clear")) {
                    musicManager.scheduler.getQueue().clear();
                    channel.sendMessage("```재생목록 초기화 완료```").queue();
                }
                ytPlayer.printQueue(channel, musicManager, command);
                break;
            case "nowplaying":
            case "np":
                ytPlayer.printNowPlaying(channel, musicManager);
                break;

            case "join" :
                if(voiceChannel == null) {
                    channel.sendMessage("```먼저 음성채널에 접속하세요```").queue();
                    break;
                }
                YTPlayer.connectToVoiceChannel(voiceChannel, audioManager);
                break;
            case "quit" :
                if(currentTrack != null)
                    musicManager.player.stopTrack();
                jda.getPresence().setActivity(Activity.playing("-help"));
                audioManager.closeAudioConnection();
                break;
            case "set":
                if(!(command.length == 3)) {
                    channel.sendMessage("```잘못된 명령어, help set 참조```").queue();
                    return;
                }
                else
                    setter(channel, command[1], command[2]);
                break;
            case "ss":
                if(currentTrack == null) {
                    ytPlayer.printNowPlaying(channel, musicManager);
                    break;
                }
                if(currentTrack.getInfo().isStream) {
                    channel.sendMessage("```스트리밍 재생시 사용 불가```").queue();
                    break;
                }
                if(musicManager.player.isPaused()) {
                    channel.sendMessage("```일시중지 중에는 사용 불가```").queue();
                    ytPlayer.printNowPlaying(channel, musicManager);
                    break;
                }
                if(command.length == 1) {
                    ytPlayer.skipBySkiptime(channel);
                    break;
                }
                else if(command.length == 2) {
                    ytPlayer.skipper(channel, command[1]);
                }
                break;
            case "jump":
            case "j":
                if(currentTrack == null) {
                    ytPlayer.printNowPlaying(channel, musicManager);
                    break;
                }
                if(currentTrack.getInfo().isStream) {
                    channel.sendMessage("```스트리밍 재생시 사용 불가```").queue();
                    break;
                }
                if(musicManager.player.isPaused()) {
                    channel.sendMessage("```일시중지 중에는 사용 불가```").queue();
                    ytPlayer.printNowPlaying(channel, musicManager);
                    break;
                }
                if(command.length == 2) {
                    ytPlayer.jumper(channel, command[1]);
                } else {
                    channel.sendMessage("```잘못된 명령어\n*" + prefix + "jump / j *시:분:초*```").queue();
                }
                break;
            case "stop":
            case "st":
                musicManager.player.stopTrack();
                jda.getPresence().setActivity(Activity.playing("-help"));
                System.out.println(currentTrack.getState());
                channel.sendMessage("```음악 중지```").queue();
                break;
            case "help":
                if(command.length == 1)
                    channel.sendMessage(Help.getHelpText()).queue();
                else if(command.length == 2 && command[1].equals("set"))
                    channel.sendMessage(Help.getSetText()).queue();
                else
                    channel.sendMessage("```잘못된 명령어\n*help / help set*```").queue();
                break;
            case "pp":
            case "pause":
                musicManager.scheduler.pause();
                ytPlayer.printNowPlaying(channel, musicManager);
                break;
            case "r":
            case "rr":
            case "resume":
                musicManager.scheduler.resume();
                ytPlayer.printNowPlaying(channel, musicManager);
                break;
            case "d":
            case "del":
                ytPlayer.delete(channel, command);
                break;
            case "get":
            case "save":
            case "dpl":
            case "delplaylist":
                saver(user, channel, command, musicManager);
                break;
            case "show":
                if (!(savedPlaylists.containsKey(user))) {
                    channel.sendMessage("```오류 : 저장된 재생목록 없음```").queue();
                    break;
                }
                ArrayList<SavedQueue> myQueues = savedPlaylists.get(user);
                printSavedQueue(myQueues, channel, command);
                break;
            default:
                channel.sendMessage("```잘못된 명령어. 접두사(기본값 '-') + help 참조```").queue();
                break;

        }

        super.onGuildMessageReceived(event);
    }

    public void printSavedQueue(ArrayList<SavedQueue> myQueues, TextChannel channel, String[] commend) {

        if (commend.length == 2) {
            if (!(commend[1].matches("^[0-9]*$"))) {
                channel.sendMessage("```잘못된 명령어. show (index) (none / all)```").queue();

            } else if(Integer.parseInt(commend[1]) > myQueues.size() || Integer.parseInt(commend[1]) < 1) {
                channel.sendMessage("```오류 : " + Integer.parseInt(commend[1]) +
                        "번째 재생 목록은 존재하지 않습니다.\n" +
                        "저장된 재생목록 수 : " + myQueues.size() + "```").queue();

            } else {
                int index = Integer.parseInt(commend[1]) - 1;
                SavedQueue myQueue = myQueues.get(index);

                myQueue.printQueue(channel, index, false);
            }
        } else if (commend.length == 3 && commend[2].equals("all")) {
            if (!(commend[1].matches("^[0-9]*$"))) {
                channel.sendMessage("```잘못된 명령어. show (index) (none / all)```").queue();

            } else if(Integer.parseInt(commend[1]) > myQueues.size() || Integer.parseInt(commend[1]) > myQueues.size()) {
                channel.sendMessage("```오류 : " + Integer.parseInt(commend[1]) +
                        "번째 재생 목록은 존재하지 않습니다.\n" +
                        "저장된 재생목록 수 : " + myQueues.size() + "```").queue();

            } else {
                int index = Integer.parseInt(commend[1]) - 1;
                SavedQueue myQueue = myQueues.get(index);

                myQueue.printQueue(channel, index, true);
            }
        } else {
            channel.sendMessage("```잘못된 명령어. show (index) (none / all)```").queue();
        }

    }




    public void saver(User user, TextChannel channel, String[] command, GuildMusicManager musicManager) {
        if(!(savedPlaylists.containsKey(user)))
            savedPlaylists.put(user, new ArrayList<>());
        ArrayList<SavedQueue> savedQueues = savedPlaylists.get(user);

        if(command.length == 2 && command[0].equals("get")) {
            if(command[1].equals("list")) {
                if (savedQueues.size() == 0) {
                    channel.sendMessage("```저장된 재생목록 없음```").queue();
                    return;
                }


                String output = "```\n";
                for (int i = 0; i < savedQueues.size(); i++) {
                    output += (i + 1) + ". ";
                    if (savedQueues.get(i).getMemo() != null)
                        output += savedQueues.get(i).getMemo();
                    else
                        output += "메모 없음";
                    output += " - <총 " + savedQueues.get(i).getQueue().size() + " 곡>\n";
                }
                output += "\n\n" +
                        "총 재생목록 수 : " + savedQueues.size() + "\n" +
                        "재생목록 사용 : get (index)\n" +
                        "```";
                channel.sendMessage(output).queue();
            } else if (command[1].matches("^[0-9]*$")) {
                int index = Integer.parseInt(command[1]) - 1;
                System.out.println(savedQueues.size());
                if ((index + 1) > savedQueues.size() || (index + 1) < 1) {
                    String output = "```인덱스 오류. 현재 재생목록 수 : " + savedQueues.size() + "```";
                    channel.sendMessage(output).queue();

                } else {
                    musicManager.player.stopTrack();
                    BlockingQueue<AudioTrack> currentQueue = musicManager.scheduler.getQueue();
                    BlockingQueue<AudioTrack> myQueue = savedQueues.get(index).getQueue();
                    currentQueue.clear();

                    currentQueue.addAll(myQueue);


                    String output = "```" + (index + 1) + "번째 재생목록으로 변경됨.\n";
                    if (savedQueues.get(index).getMemo() != null) {
                        output += "재생목록 메모 : " + savedQueues.get(index).getMemo();
                    }
                    output += "```";
                    channel.sendMessage(output).queue();

                    try {
                        musicManager.scheduler.nextTrack();

                    } catch (IllegalStateException e) {
                        AudioTrack clone = musicManager.player.getPlayingTrack().makeClone();
                        musicManager.player.playTrack(clone);
                        if (musicManager.player.getPlayingTrack() != null) {
                            jda.getPresence().setActivity(Activity.listening(musicManager.player.getPlayingTrack().getInfo().title));
                        }
                    }
                    ytPlayer.printNowPlaying(channel, ytPlayer.musicManager);

                }
            } else {
                channel.sendMessage("```잘못된 명령어. get ( list / (index) )```").queue();
            }
        } else if(command.length >= 2 && command[0].equals("save")){
            if(musicManager.scheduler.getQueue().size() == 0) {
                channel.sendMessage("```재생 목록 저장 오류 : 현재 재생 목록에 곡이 없습니다.```").queue();
                return;
            }
            String memo = "";
            for(int i = 1; i < command.length; i++) {
                memo += command[i] += " ";
            }
            BlockingQueue<AudioTrack> myQueue = new LinkedBlockingQueue<>();
            if(musicManager.player.getPlayingTrack() != null)
                myQueue.add(musicManager.player.getPlayingTrack().makeClone());
            Iterator<AudioTrack> currentQueueIterator = musicManager.scheduler.getIterator();
            while(currentQueueIterator.hasNext())
                myQueue.add(currentQueueIterator.next());
            savedQueues.add(new SavedQueue(memo, myQueue));
            channel.sendMessage("```재생목록 저장 완료```").queue();
        } else if(command.length == 1 && command[0].equals("save")) {
            if(musicManager.scheduler.getQueue().size() == 0) {
                channel.sendMessage("```재생 목록 저장 오류 : 현재 재생 목록에 곡이 없습니다.```").queue();
                return;
            }
            BlockingQueue<AudioTrack> myQueue = new LinkedBlockingQueue<>();
            myQueue.add(musicManager.player.getPlayingTrack().makeClone());
            Iterator<AudioTrack> currentQueueIterator = musicManager.scheduler.getIterator();
            while(currentQueueIterator.hasNext())
                myQueue.add(currentQueueIterator.next());
            savedQueues.add(new SavedQueue(myQueue));
            channel.sendMessage("```재생목록 저장 완료```").queue();
        } else if(command.length == 2 && (command[0].equals("dpl") || command[0].equals("delplaylist"))) {
            if (command[1].matches("^[0-9]*$")) {
                int index = Integer.parseInt(command[1]) - 1;
                ArrayList<SavedQueue> myQueues = savedPlaylists.get(user);
                if((index + 1) > myQueues.size() || (index + 1) < 1) {
                    channel.sendMessage("```잘못된 인덱스. 현재 재생목록 수 : " + myQueues.size() + "```").queue();
                    return;
                }
                SavedQueue deleted =  myQueues.remove(index);
                String output = "```" + (index + 1) + "번째 재생목록 삭제 완료\n\n" +
                        "삭제된 재생목록 :\n";
                if(deleted.getMemo() == null)
                    output += "메모 없음";
                else
                    output += deleted.getMemo();
                output += " - <총 " + deleted.getQueue().size() + " 곡>\n```";
                channel.sendMessage(output).queue();
            } else if (command[1].equals("all")) {
                ArrayList<SavedQueue> myQueues = savedPlaylists.get(user);
                if(myQueues.size() == 0) {
                    channel.sendMessage("```삭제할 재생목록 없음.```").queue();
                    return;
                }
                myQueues.clear();
                channel.sendMessage("```모든 재생목록 삭제 완료.```").queue();
            } else {
                channel.sendMessage("```잘못된 명령어. dpl, delplaylist (index)```").queue();
            }
        } else {
            channel.sendMessage("```잘못된 명령어. \n1. get ( list / (index) )\n2. save(none / 'memo')\n3. dpl, delplaylist ( (index) / all )\n```").queue();
        }


    }



    public void setter(TextChannel channel, String command, String value) {
        switch (command) {
            case "prefix":
                if(value.length() != 1)
                    channel.sendMessage("```잘못된 접두사 지정```").queue();
                prefix = value.charAt(0);
                channel.sendMessage("```Prefix = " + prefix + "```").queue();
                break;
            case "skiptime":
                if(value.equals("default"))
                    skiptime = 10;
                else
                    try {
                        skiptime = Long.parseLong(value);
                    } catch (Exception e) {
                        channel.sendMessage("```값 잘못됨. set skiptime (default / second)```").queue();
                        break;
                    }
                channel.sendMessage("```Skiptime =" + skiptime + "```").queue();
        }
    }



}
