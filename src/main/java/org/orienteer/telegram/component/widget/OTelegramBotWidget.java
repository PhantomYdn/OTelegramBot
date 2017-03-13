package org.orienteer.telegram.component.widget;

import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.security.ORule;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.orienteer.core.CustomAttribute;
import org.orienteer.core.component.FAIcon;
import org.orienteer.core.component.FAIconType;
import org.orienteer.core.component.command.EditSchemaCommand;
import org.orienteer.core.component.command.SaveSchemaCommand;
import org.orienteer.core.component.meta.OClassMetaPanel;
import org.orienteer.core.component.property.BooleanEditPanel;
import org.orienteer.core.component.property.BooleanViewPanel;
import org.orienteer.core.component.property.DisplayMode;
import org.orienteer.core.component.structuretable.OrienteerStructureTable;
import org.orienteer.core.widget.AbstractModeAwareWidget;
import org.orienteer.core.widget.Widget;
import org.orienteer.telegram.module.OTelegramCustomAttributes;
import org.orienteer.telegram.module.OTelegramModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ydn.wicket.wicketorientdb.components.TransactionlessForm;
import ru.ydn.wicket.wicketorientdb.security.OSecurityHelper;
import ru.ydn.wicket.wicketorientdb.security.OrientPermission;

import java.util.ArrayList;
import java.util.List;



/**
 * @author Vitaliy Gonchar
 */
@Widget(domain="class", tab="telegram", id="telegram-list", autoEnable=true)
public class OTelegramBotWidget extends AbstractModeAwareWidget<OClass> {
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LoggerFactory.getLogger(OTelegramBotWidget.class);

	private OrienteerStructureTable<OClass, String> structureTable;

	private List<String> propertiesList = new ArrayList<>();
	
	public OTelegramBotWidget(String id, IModel<OClass> model, IModel<ODocument> widgetDocumentModel) {
		super(id, model, widgetDocumentModel);
		Form<OClass> form = new TransactionlessForm<OClass>("form");
		propertiesList.add(OTelegramCustomAttributes.TELEGRAM_SEARCH.get().getName());
		propertiesList.add(OTelegramCustomAttributes.TELEGRAM_DOCUMENTS_LIST.get().getName());
		propertiesList.add(OTelegramCustomAttributes.TELEGRAM_CLASS_DESCRIPTION.get().getName());
		propertiesList.add(OTelegramCustomAttributes.TELEGRAM_SEARCH_QUERY.get().getName());
		structureTable = new OrienteerStructureTable<OClass, String>("attributes", model, propertiesList) {

			@Override
			protected Component getValueComponent(String id, IModel<String> rowModel) {
				return new OClassMetaPanel<Object>(id, getModeModel(), getModel(), rowModel) {
					@Override
					protected Object getValue(OClass entity, String critery) {
						CustomAttribute customAttribute = CustomAttribute.get(critery);
						LOG.debug("customAttribute = " + customAttribute.getName() + " default value: " + customAttribute.getDefaultValue());
						return customAttribute.getValue(entity);
					}

					@Override
					protected Component resolveComponent(String id, DisplayMode mode, String critery) {
						if (DisplayMode.EDIT.equals(mode) && !OSecurityHelper.isAllowed(ORule.ResourceGeneric.SCHEMA, null, OrientPermission.UPDATE)) {
							mode = DisplayMode.VIEW;
						}
						if (DisplayMode.VIEW.equals(mode)) {
							if (critery.equals(OTelegramCustomAttributes.TELEGRAM_SEARCH_QUERY.get().getName())) {
								return new Label(id, getModel());
							} else {
								return new BooleanViewPanel(id, Model.<Boolean>of(getModel()));
							}
						} else if (DisplayMode.EDIT.equals(mode)){
							if (critery.equals(OTelegramCustomAttributes.TELEGRAM_SEARCH_QUERY.get().getName())) {
								return new TextField(id, getModel());
							} else return new BooleanEditPanel(id, Model.<Boolean>of(getModel()));
						}
						return null;
					}

					@Override
					protected void setValue(OClass entity, String critery, Object value) {
						CustomAttribute customAttribute = CustomAttribute.get(critery);
						customAttribute.setValue(entity, value);
					}
				};
			}
		};

		structureTable.addCommand(new EditSchemaCommand<OClass>(structureTable, getModeModel()));
		structureTable.addCommand(new SaveSchemaCommand<OClass>(structureTable, getModeModel()));
		form.add(structureTable);
		add(form);
	}

    @Override
    protected FAIcon newIcon(String id) {
        return new FAIcon(id, FAIconType.list);
    }

    @Override
    protected IModel<String> getDefaultTitleModel() {
        return new ResourceModel("telegram.configuration.title");
    }
    
    @Override
	protected String getWidgetStyleClass() {
		return "strict";
	}
}
