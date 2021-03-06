package com.jmpesp.halgnu.listeners;

import com.jmpesp.halgnu.models.MemberModel;
import com.jmpesp.halgnu.tasks.TwitterTask;
import com.jmpesp.halgnu.util.CommandHelper;
import com.jmpesp.halgnu.managers.ConfigManager;
import com.jmpesp.halgnu.util.PermissionHelper;
import org.apache.commons.lang3.StringUtils;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;

public class TwitterListener extends ListenerAdapter {

    private String m_command = ".tweet";

    private int m_nickAbbrivLength = 7;
    private int m_twitterMsgLimit = 140;

    private List<MemberModel.MemberStatus> neededPermissions =
            new ArrayList<MemberModel.MemberStatus>(Arrays.asList(
                    MemberModel.MemberStatus.OG,
                    MemberModel.MemberStatus.ADMIN,
                    MemberModel.MemberStatus.MEMBER
            ));

    private ConfigurationBuilder m_configBuilder;
    private TwitterFactory m_twitterFactory;
    private Twitter m_twitter;

    private Timer m_timer;

    public static void sendHelpMsg(GenericMessageEvent event) {
        event.getBot().sendIRC().message(event.getUser().getNick(), ".tweet <tweet> - Used to send out a tweet under group account");
    }
    
    public TwitterListener() {
        m_timer = new Timer();
        m_configBuilder = new ConfigurationBuilder();
        m_configBuilder.setDebugEnabled(true)
                .setOAuthConsumerKey(ConfigManager.getInstance().getTwitterConsumerKey())
                .setOAuthConsumerSecret(ConfigManager.getInstance().getTwitterComsumerSecret())
                .setOAuthAccessToken(ConfigManager.getInstance().getTwitterAccessToken())
                .setOAuthAccessTokenSecret(ConfigManager.getInstance().getTwitterAccessSecret());
        m_twitterFactory = new TwitterFactory(m_configBuilder.build());
        m_twitter = m_twitterFactory.getInstance();

        m_timer.scheduleAtFixedRate(new TwitterTask(), 2*60*1000, 2*60*1000);
    }

    @Override
    public void onGenericMessage(final GenericMessageEvent event) throws Exception {

        if (event.getMessage().startsWith(m_command)) {
            if(PermissionHelper.HasPermissionFromList(neededPermissions, event.getUser().getNick())) {
                if (CommandHelper.checkForAmountOfArgs(event.getMessage(), 1)) {
                    String completeMsg = CommandHelper.removeCommandFromString(event.getMessage()).trim() +
                            " - " + StringUtils.abbreviate(event.getUser().getNick().trim(), m_nickAbbrivLength);

                    if(completeMsg.length() <= m_twitterMsgLimit) {
                        if (tweetMessage(completeMsg)){
                            event.respond("Tweeted");
                        } else {
                            event.respond("Error occurred when sending tweet.");
                        }
                    } else {
                        event.respond("Error: Tweet is " + (completeMsg.length()-m_twitterMsgLimit) + " characters to long.");
                    }
                } else {
                    event.respond("Ex: " + m_command + "");
                }
            } else {
                event.respond("Permission denied");
            }
        }
    }

    private boolean tweetMessage(String message) {
        try {
            m_twitter.updateStatus(message);
            return true;
        } catch (TwitterException e) {
            e.printStackTrace();
            return false;
        }
    }
}
