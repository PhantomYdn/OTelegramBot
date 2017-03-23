package org.orienteer.telegram.bot;

import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import org.orienteer.telegram.module.OTelegramModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ydn.wicket.wicketorientdb.utils.DBClosure;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Vitaliy Gonchar
 */
public abstract class Cache {
    private static Map<String, OClass> classCache;
    private static Map<String, String> queryCache;

    private static int schemaVersion;

    private static final Logger LOG = LoggerFactory.getLogger(Cache.class);

    public static synchronized void initCache() {
        new DBClosure() {
            @Override
            protected Object execute(ODatabaseDocument db) {
                int newVersion = db.getMetadata().getSchema().getVersion();
                if (schemaVersion != newVersion) {
                    createClassCache(db);
                    createQueryCache();
                    schemaVersion = newVersion;
                }
                return null;
            }
        }.execute();
    }

    private static void createClassCache(ODatabaseDocument db) {
        classCache = new HashMap<>();
        for (OClass oClass : db.getMetadata().getSchema().getClasses()) {
            if (OTelegramModule.TELEGRAM_SEARCH.getValue(oClass)) {
                classCache.put(oClass.getName(), oClass);
            }
        }
        LOG.debug("Class cache size: " + classCache.size());
    }

    private static void createQueryCache() {
        queryCache = new HashMap<>();
        for (OClass oClass : classCache.values()) {
            String query = OTelegramModule.TELEGRAM_SEARCH_QUERY.getValue(oClass);
            if (query == null){
                query = "SELECT FROM " + oClass.getName();
                OTelegramModule.TELEGRAM_SEARCH_QUERY.setValue(oClass, query);
            }
            queryCache.put(oClass.getName(), query);
        }
    }

    public static Map<String, String> getQueryCache() {
        initCache();
        return queryCache;
    }

    public static Map<String, OClass> getClassCache() {
        initCache();
        return classCache;
    }
}
