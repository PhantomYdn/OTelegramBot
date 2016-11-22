package org.orienteer.telegram.bot.response;

import org.orienteer.telegram.bot.BotMessage;
import org.orienteer.telegram.bot.UserSession;
import org.orienteer.telegram.bot.link.LinkFactory;
import org.orienteer.telegram.bot.search.Search;
import org.orienteer.telegram.bot.search.SearchFactory;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author Vitaliy Gonchar
 */
public class ResponseFactory {
    private final Message message;
    private UserSession userSession;
    private BotMessage botMessage;

    public ResponseFactory(Message message, UserSession userSession) {
        this.message = message;
        this.userSession = userSession;
        if (this.userSession == null) {
            this.userSession = new UserSession();
            this.userSession.setBotState(getBotState(message.getText()));
        }
        this.botMessage = this.userSession.getBotMessage();
    }

    public Response getResponse() {
        BotState state = getBotState(message.getText());
        state = state == BotState.BACK ? userSession.getPreviousBotState() : state;
        List<SendMessage> responseList = new ArrayList<>(2);
        Response response = null;
        switch (state) {
            case START:
                userSession.setBotState(BotState.NEW_CLASS_SEARCH);
                userSession.setPreviousBotState(BotState.START);
                responseList.add(ResponseMessage.getStartMenu(message, botMessage));
                break;
            case CLASS_SEARCH:
                userSession.setTargetClass(message.getText().substring(botMessage.CLASS_BUT.length()));
                userSession.setBotState(BotState.SEARCH_IN_CLASS_GLOBAL);
                userSession.setPreviousBotState(BotState.START);
                responseList.add(ResponseMessage.getBackMenu(message, String.format(botMessage.CLASS_SEARCH_MSG, "/" + userSession.getTargetClass()), botMessage));
                break;
            case NEXT_RESULT:
                responseList.add(ResponseMessage.getTextMessage(message, userSession.getNextResult()));
                responseList.add(ResponseMessage.getNextPreviousMenu(message, userSession.hasNextResult(), userSession.hasPreviousResult(), botMessage));
                break;
            case PREVIOUS_RESULT:
                responseList.add(ResponseMessage.getTextMessage(message, userSession.getPreviousResult()));
                responseList.add(ResponseMessage.getNextPreviousMenu(message, userSession.hasNextResult(), userSession.hasPreviousResult(), botMessage));
                break;
            case GO_TO_DOCUMENT_SHORT_DESCRIPTION:
                responseList.add(ResponseMessage.getTextMessage(message, LinkFactory.getLink(message.getText(), false, botMessage).goTo()));
                break;
            case GO_TO_DOCUMENT_ALL_DESCRIPTION:
                responseList.add(ResponseMessage.getTextMessage(message, LinkFactory.getLink(message.getText(), true, botMessage).goTo()));
                break;
            case GO_TO_CLASS:
                responseList.add(ResponseMessage.getTextMessage(message, LinkFactory.getLink(message.getText(), botMessage).goTo()));
                break;
            case CHANGE_LANGUAGE:
                userSession.setBotMessage(changeLanguage(message));
                userSession.setPreviousBotState(BotState.START);
                userSession.setBotState(BotState.START);
                responseList.add(ResponseMessage.getTextMessage(message, botMessage.LANGUAGE_CHANGED));
                responseList.add(ResponseMessage.getStartMenu(message, botMessage));
                break;
            case LANGUAGE:
                responseList.add(ResponseMessage.getLanguageMenu(message, botMessage));
                break;
            case ABOUT:
                responseList.add(ResponseMessage.getTextMessage(message, botMessage.ABOUT_MSG));
                break;
            default:
                response = handleSearchRequest(message, userSession);
        }

        return response != null ? response : new Response(responseList, userSession);
    }

    private Response handleSearchRequest(Message message, UserSession userSession) {
        List<SendMessage> responseList = new ArrayList<>(2);
        List<String> result = null;
        Search search;
        switch (userSession.getBotState()) {
            case SEARCH_IN_CLASS_GLOBAL:
                search = SearchFactory.getSearch(message.getText(), userSession.getTargetClass(), botMessage);
                result = search.execute();
                break;
            case NEW_CLASS_SEARCH:
                search = SearchFactory.getSearch(message.getText(), botMessage);
                result = search.execute();
                break;
        }
        if (result != null) {
            userSession.setResultOfSearch(result);
            if (result.size() > 1) {
                responseList.add(ResponseMessage.getTextMessage(message, userSession.getNextResult()));
                responseList.add(ResponseMessage.getNextPreviousMenu(message, userSession.hasNextResult(), userSession.hasPreviousResult(), botMessage));
            } else {
                responseList.add(ResponseMessage.getTextMessage(message, botMessage.START_SEARCH_MSG));
                responseList.add(ResponseMessage.getTextMessage(message, userSession.getNextResult()));

            }
        } else responseList.add(ResponseMessage.getTextMessage(message, botMessage.SEARCH_RESULT_FAILED_MSG));

        return new Response(responseList, userSession);
    }

    private BotMessage changeLanguage(Message message) {
        String lang = message.getText().substring(botMessage.LANGUAGE_BUT.length());
        if (lang.equals(botMessage.ENGLISH)) {
            return new BotMessage("en");
        } else if (lang.equals(botMessage.RUSSIAN)) {
            return new BotMessage("ru");
        } else  {
            return new BotMessage("uk");
        }
    }

    private BotState getBotState(String text) {
        BotState state = BotState.ERROR;
        for (BotState search : BotState.values()) {
            if (search.getCommand().equals(text)) {
                state = search;
                break;
            }
        }
        if (state == BotState.ERROR) {
            if (text.startsWith(BotState.GO_TO_CLASS.getCommand()) && text.endsWith("_details")) {
                return BotState.GO_TO_DOCUMENT_ALL_DESCRIPTION;
            }
            if (text.startsWith(botMessage.LANGUAGE_BUT)) {
                return BotState.CHANGE_LANGUAGE;
            }
            if (text.startsWith(BotState.GO_TO_CLASS.getCommand()) && text.contains("_")) {
                return BotState.GO_TO_DOCUMENT_SHORT_DESCRIPTION;
            } else if (text.startsWith(BotState.GO_TO_CLASS.getCommand())) {
                return BotState.GO_TO_CLASS;
            } else if (text.startsWith("/")) {
                return BotState.GO_TO_CLASS;
            } else if (text.startsWith(botMessage.CLASS_BUT)) {
                return BotState.CLASS_SEARCH;
            } else if (text.equals(botMessage.NEXT_RESULT_BUT)) {
                return BotState.NEXT_RESULT;
            } else if (text.endsWith(botMessage.PREVIOUS_RESULT_BUT)) {
                return BotState.PREVIOUS_RESULT;
            } else if (text.equals(botMessage.BACK)) {
                return BotState.BACK;
            }
        }
        return state;
    }
}
