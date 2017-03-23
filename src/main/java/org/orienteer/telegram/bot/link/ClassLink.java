package org.orienteer.telegram.bot.link;

import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.iterator.ORecordIteratorClass;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.orienteer.telegram.bot.Cache;
import org.orienteer.telegram.bot.MessageKey;
import org.orienteer.telegram.bot.OTelegramBot;
import org.orienteer.telegram.bot.response.BotState;
import org.orienteer.telegram.module.OTelegramModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ydn.wicket.wicketorientdb.utils.DBClosure;

import java.util.*;

/**
 * @author Vitaliy Gonchar
 */
public class ClassLink {
    private final String className;
    private final Locale locale;
    private final String classLink;
    private static final Logger LOG = LoggerFactory.getLogger(ClassLink.class);

    public ClassLink(String classLink, Locale locale) {
        this.locale = locale;
        this.classLink = classLink;
        className = classLink.contains("@") ?
                classLink.substring(0, classLink.indexOf("@")).substring(BotState.GO_TO_CLASS.getCommand().length())
                : classLink.substring(BotState.GO_TO_CLASS.getCommand().length());
    }

    public Map<Integer, String> getResult() {
        return new DBClosure<Map<Integer, String>>() {
            @Override
            protected Map<Integer, String> execute(ODatabaseDocument oDatabaseDocument) {
                Map<Integer, String> result = new HashMap<Integer, String>();
                StringBuilder builder = new StringBuilder(
                        String.format(MessageKey.HTML_STRONG_TEXT.toString(), MessageKey.CLASS_DESCRIPTION_MSG.getString(locale)) + "\n\n");
                Map<String, OClass> classCache = Cache.getClassCache();
                if (!classCache.containsKey(className)) {
                    result = new HashMap<Integer, String>();
                    result.put(0, MessageKey.SEARCH_FAILED_CLASS_BY_NAME.getString(locale));
                    return result;
                }
                OClass oClass = classCache.get(className);
                builder.append(String.format(MessageKey.HTML_STRONG_TEXT.toString(), MessageKey.NAME.getString(locale) + " "));
                builder.append(oClass.getName());
                builder.append("\n");
                builder.append(String.format(MessageKey.HTML_STRONG_TEXT.toString(), MessageKey.SUPER_CLASSES.getString(locale) + " "));
                List<String> superClassNames = new ArrayList<>();
                for (OClass superClass : oClass.getSuperClasses()) {
                    if (classCache.containsKey(superClass.getName())) {
                        superClassNames.add("/" + superClass.getName() + " ");
                    }
                }
                if (superClassNames.size() > 0) {
                    for (String str : superClassNames) {
                        builder.append(str);
                    }
                } else builder.append(MessageKey.WITHOUT_SUPER_CLASSES.getString(locale));
                builder.append("\n");
                List<String> resultList = new ArrayList<>();
                if (OTelegramModule.TELEGRAM_CLASS_DESCRIPTION.getValue(oClass)) {
                    Collection<OProperty> properties = oClass.properties();
                    for (OProperty property : properties) {
                        resultList.add(String.format(MessageKey.HTML_STRONG_TEXT.toString(), property.getName())
                                + ": " + property.getDefaultValue() + " (" + MessageKey.DEFAULT_VALUE.getString(locale) + ")");
                    }
                    Collections.sort(resultList);
                    for (String string : resultList) {
                        builder.append(string);
                        builder.append("\n");
                    }
                }
                ORecordIteratorClass<ODocument> oDocuments = oDatabaseDocument.browseClass(oClass.getName());
                resultList = new ArrayList<>();
                if (OTelegramModule.TELEGRAM_DOCUMENTS_LIST.getValue(oClass)) {
                    builder.append("\n");
                    builder.append(String.format(MessageKey.HTML_STRONG_TEXT.toString(), MessageKey.CLASS_DOCUMENTS.getString(locale)));
                    builder.append("\n");
                    for (ODocument oDocument : oDocuments) {
                        String docId = BotState.GO_TO_CLASS.getCommand() + oDocument.getClassName()
                                + "_" + oDocument.getIdentity().getClusterId()
                                + "_" + oDocument.getIdentity().getClusterPosition();
                        String docName = OTelegramBot.getDocName(oDocument);
                        resultList.add(docName + " " + docId);
                    }
                    Collections.sort(resultList);
                    String head = builder.toString();
                    result.put(0, head + String.format(MessageKey.HTML_STRONG_TEXT.toString(), "1. ") + resultList.get(0) + "\n");
                    for (int i = 1; i < resultList.size(); i++) {
                        if (i % 10 == 0) {
                            result.put(i, head + String.format(MessageKey.HTML_STRONG_TEXT.toString(), (i + 1) + ". ") + resultList.get(i) + "\n");
                        } else {
                            result.put(i, String.format(MessageKey.HTML_STRONG_TEXT.toString(), (i + 1) + ". ") + resultList.get(i) + "\n");
                        }
                    }
                } else result.put(0, builder.toString());
                return result;
            }
        }.execute();
    }

}
