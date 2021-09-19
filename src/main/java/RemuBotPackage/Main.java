package RemuBotPackage;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;



import javax.security.auth.login.LoginException;
import java.net.SocketException;
import java.nio.channels.ClosedByInterruptException;

public class Main extends ListenerAdapter {



    public static void main(String[] args) throws LoginException, SocketException , ClosedByInterruptException {
        JDA jda = JDABuilder.createDefault("ODg2MjYxNzYxNTUwOTgzMTgw.YTzBlQ.MvMtZDllRyVLbJAtBczyGEgI1eE")
                .addEventListeners(new EVListener())
                .build();

        jda.setAutoReconnect(true);
        jda.getPresence().setActivity(Activity.playing("-help"));



    }


}
