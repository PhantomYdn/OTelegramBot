package org.orienteer.telegram.bot.util;

import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import org.orienteer.telegram.bot.OTelegramBot;

import java.util.List;
import java.util.Map;

/**
 * Search class by given word in classes cache
 */
public class OClassNameSearch extends AbstractSearch {

    private final String searchWord;

    /**
     * Constructor
     * @param bot {@link OTelegramBot} bot which need search
     * @param searchWord {@link String} which will search
     */
    public OClassNameSearch(OTelegramBot bot, String searchWord) {
        super(bot);
        this.searchWord = searchWord;
    }

    @Override
    public Map<Integer, String> search() {
        List<String> result = Lists.newArrayList();
        String headInfo = null;
        for (OClass oClass : classCache.values()) {
            if (isWordInLine(searchWord, oClass.getName())) {
                String searchClass = createSearchResultString(searchWord, oClass.getName()) + " "
                        + BotState.GO_TO_CLASS.getCommand() + oClass.getName() + "\n";
                result.add(searchClass);
            }
        }
        if (!result.isEmpty()) {
            headInfo =  "\n" + Markdown.BOLD.toString(MessageKey.SEARCH_CLASS_RESULT.toLocaleString(bot)) + "\n"
                    + Markdown.BOLD.toString(1 + ".  ");
        }
        return newSearchResult(result, headInfo);
    }
}
