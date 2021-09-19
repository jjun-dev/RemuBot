package RemuBotPackage;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;

import static RemuBotPackage.YTPlayer.timeFormatter;

public class SavedQueue {

    private final String memo;
    private final BlockingQueue<AudioTrack> queue;

    public SavedQueue(String memo, BlockingQueue<AudioTrack> queue) {
        this.memo = memo;
        this.queue = queue;
    }
    public SavedQueue(BlockingQueue<AudioTrack> queue) {
        this.memo = null;
        this.queue = queue;
    }

    public String getMemo() {
        return memo;
    }

    public BlockingQueue<AudioTrack> getQueue() {
        return queue;
    }

    public void printQueue (TextChannel channel, int index, boolean isAll) {

        String[] strArr = new String[queue.size()];
        long sum = 0;
        Iterator<AudioTrack> iterator = queue.iterator();
        String output = "```md\n" + (index+1) + ". ";

        if (memo == null) {
            output += "메모 없음\n\n";
        } else {
            output += memo + "\n\n";
        }

        output += "재생 목록 : \n";


        int i = 0;
        while(iterator.hasNext()) {
            AudioTrack track = iterator.next();
            AudioTrackInfo trackInfo = track.getInfo();
            if (!(trackInfo.isStream)) {
                sum += trackInfo.length;
            }
            strArr[i] = (i+1) + ". "
                    + trackInfo.title + " - <";
            if(trackInfo.isStream) {
                strArr[i] += "실시간 스트리밍";
            } else {
                strArr[i] += timeFormatter(trackInfo.length, trackInfo.length);
            }
            strArr[i] += ">\n";
            i++;
        }

        String sumStr = "\n\n총 갯수 : " + (queue.size())
                + "\n총 길이 : <" + timeFormatter(sum, sum) + ">\n```";


        if(queue.size() > 20) {
            if(!isAll) {
                output += "(상위 20개만 표시, 전체 보기 : show (index) all)\n\n";
                for(int j = 0; j < 20; j++) {
                    output += strArr[j] + "\n";
                }
                output += sumStr;
                channel.sendMessage(output).queue();
            } else {
                output += "\n";

                for(int j = 0; j < strArr.length; j++) {
                    output += strArr[j] + "\n";

                    if (j == strArr.length - 1) {
                        output += sumStr;

                        channel.sendMessage(output).queue();
                        break;
                    }
                    if(j > 1 && j % 20 == 0) {
                        output += "```";

                        channel.sendMessage(output).queue();
                        output = "```md\n";
                    }
                }
            }
        } else {
            output += "\n";
            for(int j = 0; j < strArr.length; j++) {
                output += strArr[j] + "\n";
            }
            output += sumStr;
            channel.sendMessage(output).queue();
        }
    }
}
