package RemuBotPackage;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;



import javax.security.auth.login.LoginException;
import java.net.SocketException;
import java.nio.channels.ClosedByInterruptException;

public class Main extends ListenerAdapter {

    public static JDA jda;


    public static void main(String[] args) throws LoginException, SocketException , ClosedByInterruptException {
        jda = JDABuilder.createDefault("")
                .addEventListeners(new EVListener())
                .build();

        jda.setAutoReconnect(true);
        jda.getPresence().setActivity(Activity.playing("-help"));



    }


}