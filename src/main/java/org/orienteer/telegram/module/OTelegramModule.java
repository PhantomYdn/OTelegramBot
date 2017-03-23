package org.orienteer.telegram.module;

import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.orienteer.core.CustomAttribute;
import org.orienteer.core.OrienteerWebApplication;
import org.orienteer.core.module.AbstractOrienteerModule;
import org.orienteer.core.module.IOrienteerModule;
import org.orienteer.core.util.OSchemaHelper;
import org.orienteer.telegram.bot.OTelegramBot;
import org.orienteer.telegram.bot.handler.LongPolligHandlerConfig;
import org.orienteer.telegram.bot.handler.OTelegramLongPollingHandler;
import org.orienteer.telegram.bot.handler.WebHookHandlerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.updatesreceivers.BotSession;

/**
 * @author Vitaliy Gonchar
 */
public class OTelegramModule extends AbstractOrienteerModule {

	private static final Logger LOG = LoggerFactory.getLogger(OTelegramModule.class);

	private static final String NAME                                         = "telegram";
	private static final String OCLASS_NAME                                  = "OTelegramBotTest";
	private static final String OPROPERTY_USERNAME                           = "username";
	private static final String OPROPERTY_TOKEN                              = "token";
	private static final String OPROPERTY_USER_SESSION                       = "user_session";
	private static final String OPROPERTY_WEB_HOOK_ENABLE                    = "web_hook_enable";
	private static final String OPROPERTY_WEB_HOOK_HOST                      = "web_hook_host";
	private static final String OPROPERTY_WEB_HOOK_PORT                      = "web_hook_port";
	private static final String OPROPERTY_WEB_HOOK_PATH_TO_CERTIFICATE_KEY   = "path_to_certificate_public_key";
	private static final String OPROPERTY_WEB_HOOK_PATH_TO_CERTIFICATE_STORE = "path_to_certificate_store";
	private static final String OPROPERTY_WEB_HOOK_CERTIFICATE_PASSWORD      = "certificate_password";

	public static final CustomAttribute TELEGRAM_SEARCH = CustomAttribute.getOrCreate("orienteer.telegramSearch", OType.BOOLEAN, false, false, false);
    public static final CustomAttribute TELEGRAM_DOCUMENTS_LIST = CustomAttribute.getOrCreate("orienteer.telegramDocumentsList", OType.BOOLEAN, false, false, false);
	public static final CustomAttribute TELEGRAM_SEARCH_QUERY = CustomAttribute.getOrCreate("orienteer.telegramSearchQuery", OType.STRING, null, true, false);
	public static final CustomAttribute TELEGRAM_CLASS_DESCRIPTION = CustomAttribute.getOrCreate("orienteer.telegramClassDescription", OType.BOOLEAN, false, false, false);

    private BotSession botSession;

	protected OTelegramModule() {
		super(NAME, 1);
	}
	
	@Override
	public ODocument onInstall(OrienteerWebApplication app, ODatabaseDocument db) {
		OSchemaHelper helper = OSchemaHelper.bind(db);
		helper.oClass(OCLASS_NAME, OMODULE_CLASS)
				.oProperty(OPROPERTY_USERNAME, OType.STRING).notNull()
				.oProperty(OPROPERTY_TOKEN, OType.STRING).notNull()
				.oProperty(OPROPERTY_WEB_HOOK_ENABLE, OType.BOOLEAN).defaultValue("false")
				.oProperty(OPROPERTY_WEB_HOOK_HOST, OType.STRING).defaultValue("Your web hook host").notNull()
				.oProperty(OPROPERTY_WEB_HOOK_PORT, OType.INTEGER).defaultValue("443").notNull()
				.oProperty(OPROPERTY_WEB_HOOK_PATH_TO_CERTIFICATE_KEY, OType.STRING)
				.oProperty(OPROPERTY_WEB_HOOK_PATH_TO_CERTIFICATE_STORE, OType.STRING)
				.oProperty(OPROPERTY_WEB_HOOK_CERTIFICATE_PASSWORD, OType.STRING)
				.oProperty(OPROPERTY_USER_SESSION, OType.LONG).defaultValue("30").notNull()
				.oProperty(IOrienteerModule.OMODULE_ACTIVATE, OType.BOOLEAN).defaultValue("false");

		return new ODocument(helper.getOClass());
	}

	@Override
	public void onInitialize(OrienteerWebApplication app, ODatabaseDocument db, ODocument moduleDoc) {
		app.registerWidgets("org.orienteer.telegram.component.widget");
		LOG.debug("moduleDoc: " + moduleDoc.toString());

		TelegramBotsApi telegramBotsApi;
		try {
			if (moduleDoc.field(IOrienteerModule.OMODULE_ACTIVATE)) {
				if (moduleDoc.field(OPROPERTY_WEB_HOOK_ENABLE)) {
					WebHookHandlerConfig config = readWebHookBotConfig(moduleDoc);
					telegramBotsApi = new TelegramBotsApi(
							config.pathToCertificateStore,
							config.certificateStorePassword,
							config.externalWebHookUrl,
							config.internalWebHookUrl,
							config.pathToCertificatePublicKey);
					telegramBotsApi.registerBot(OTelegramBot.getWebHookBot(config));
				} else {
					telegramBotsApi = new TelegramBotsApi();
					LongPolligHandlerConfig longPolligHandlerConfig = readLongPollingBotConfig(moduleDoc);
					OTelegramLongPollingHandler longPollingBot = OTelegramBot.getLongPollingBot(longPolligHandlerConfig);
					botSession = telegramBotsApi.registerBot(longPollingBot);
				}
			}

		} catch (TelegramApiRequestException e) {
			LOG.error("Cannot register bot");
			if (LOG.isDebugEnabled()) e.printStackTrace();
		}
	}

	private LongPolligHandlerConfig readLongPollingBotConfig(ODocument doc) {
		String username;
		String token;
		long userSession;
		username = doc.field(OPROPERTY_USERNAME, OType.STRING);
		token = doc.field(OPROPERTY_TOKEN, OType.STRING);
		userSession = doc.field(OPROPERTY_USER_SESSION, OType.LONG);

		return new LongPolligHandlerConfig(username, token, userSession);
	}

	private WebHookHandlerConfig readWebHookBotConfig(ODocument doc) {
		String username;
		String token;
		String webHookHost;
		String pathToCertificatePublicKey;
		String pathToCertificateStore;
		String certificateStorePassword;
		long userSession;
		long port;

		username = doc.field(OPROPERTY_USERNAME, OType.STRING);
		token = doc.field(OPROPERTY_TOKEN, OType.STRING);
		userSession = doc.field(OPROPERTY_USER_SESSION, OType.LONG);
		webHookHost = doc.field(OPROPERTY_WEB_HOOK_HOST, OType.STRING);
		pathToCertificatePublicKey = doc.field(OPROPERTY_WEB_HOOK_PATH_TO_CERTIFICATE_KEY, OType.STRING);
		pathToCertificateStore = doc.field(OPROPERTY_WEB_HOOK_PATH_TO_CERTIFICATE_STORE, OType.STRING);
		certificateStorePassword = doc.field(OPROPERTY_WEB_HOOK_CERTIFICATE_PASSWORD, OType.STRING);
		port = doc.field(OPROPERTY_WEB_HOOK_PORT, OType.LONG);

		return new WebHookHandlerConfig(username, token, webHookHost, port, userSession, pathToCertificatePublicKey, pathToCertificateStore, certificateStorePassword);
	}

	@Override
	public void onDestroy(OrienteerWebApplication app, ODatabaseDocument db, ODocument moduleDoc) {
		if (botSession != null) {
			botSession.close();
		}
	}

}
