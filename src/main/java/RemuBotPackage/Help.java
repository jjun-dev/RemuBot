package RemuBotPackage;

public class Help {
    private static String helpText = "```" +
            "기본 접두사 : '-'\n\n" +
            "p, play ( 유튜브 링크 / 플레이리스트 링크 / 큐 인덱스(숫자) ) :\n" +
            "유튜브 영상 / 플레이 리스트를 큐에 추가\n" +
            "또는 큐에서 지정한 번호의 트랙을 재생" +
            "\n\n" +
            "s, skip :\n" +
            "노래 스킵" +
            "\n\n" +
            "q, queue (none / all / clear):\n" +
            "none : 현재 큐 상태 출력, 20개 이상일경우 20개만 출력\n" +
            "all : 현재 큐 상태 모두 출력\n" +
            "clear : 큐 초기화" +
            "\n\n" +
            "d, del (index / start-end) :\n" +
            "index : 지정한 번호의 트랙을 큐에서 삭제\n" +
            "start-end : start번 트랙부터 end번 트랙까지 큐에서 삭제" +
            "\n\n" +
            "np, nowplaying :\n" +
            "현재 재생중인 노래 출력" +
            "\n\n" +
            "join :\n" +
            "명령한 유저가 접속한 음성채널에 접속" +
            "\n\n" +
            "quit :\n" +
            "봇이 음성채널에서 퇴장, 재생중이라면 재생 중단 후 퇴장" +
            "\n\n" +
            "ss (none / 초 / 분:초 / 시:분:초) :\n" +
            "none - skiptime 만큼 노래 뒤로 이동(기본 skiptime = 10초)\n" +
            "이외 - 지정한 시간 만큼 노래 뒤로 이동" +
            "\n\n" +
            "j, jump (초 / 분:초 / 시:분:초) :\n" +
            "지정한 시간으로 노래 이동" +
            "\n\n" +
            "st, stop :\n" +
            "재생 중단" +
            "\n\n" +
            "pp, pause :\n" +
            "일시 중지" +
            "\n\n" +
            "r, rr, resume :\n" +
            "재개" +
            "\n\n" +
            "save (none / '메모') :\n" +
            "현재 재생목록을 저장, 간단한 메모를 덧붙일 수 있음." +
            "\n\n" +
            "get ( list / (index) ) :\n" +
            "list : 내가 저장한 재생목록들을 가져옴\n" +
            "(index, 숫자) : 현재 재생목록을 index번째 재생목록으로 변경" +
            "\n\n" +
            "dpl, delplaylist ( (index) / all ) :\n" +
            "(index) : index번째 재생목록 삭제\n" +
            "all : 저장된 나의 모든 재생목록 삭제" +
            "\n\n" +
            "show (index) (none / all) :\n" +
            "index번째 재생목록을 출력.\n" +
            "담겨있는 곡이 20개를 초과할 경우 20개만 출력.\n" +
            "all : index번째 재생목록 전체 출력." +
            "\n\n" +
            "set :\n" +
            "설정 옵션, 자세한 설명은 '접두사 + help set'" +
            "```";

    private static String setText = "```" +
            "set (prefix / skiptime) :\n\n" +
            "set prefix ('1글자 접두사') :\n" +
            "접두사 변경. 기본 값 = '-'" +
            "\n\n" +
            "set skiptime (default / 초) :\n" +
            "'ss' 커맨드에 사용할 skiptime 변경. 기본 값(default) = 10초" +
            "```";

    public static String getHelpText() {
        return helpText;
    }

    public static String getSetText() {
        return setText;
    }
}
