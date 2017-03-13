package org.orienteer.telegram.module;

import com.orientechnologies.orient.core.metadata.schema.OType;
import org.orienteer.core.CustomAttribute;

/**
 * @author Vitaliy Gonchar
 */
public enum OTelegramCustomAttributes {
    TELEGRAM_SEARCH("orienteer.telegramSearch", OType.BOOLEAN, false, false, false),
    TELEGRAM_DOCUMENTS_LIST("orienteer.telegramDocumentsList", OType.BOOLEAN, false, false, false),
    TELEGRAM_SEARCH_QUERY("orienteer.telegramSearchQuery", OType.STRING, null, true, false),
    TELEGRAM_CLASS_DESCRIPTION("orienteer.telegramClassDescription", OType.BOOLEAN, false, false, false);

    private final String name;
    private final OType type;
    private final Object defaultValue;
    private final boolean encode;
    private final boolean hiearchical;

    OTelegramCustomAttributes(String  name, OType type, Object defaultValue, boolean encode, boolean hiearchical) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
        this.encode = encode;
        this.hiearchical = hiearchical;
    }

    public CustomAttribute get() {
        CustomAttribute result = CustomAttribute.getIfExists(name);
        return result != null ? result : CustomAttribute.create(name, type, defaultValue, encode, hiearchical);
    }
}
