package dev.se1dhe.core.handlers.inline;


import dev.se1dhe.core.bots.AbstractTelegramBot;
import dev.se1dhe.core.handlers.ICallbackQueryHandler;
import dev.se1dhe.core.handlers.ICancelHandler;
import dev.se1dhe.core.handlers.ICommandHandler;
import dev.se1dhe.core.handlers.IMessageHandler;
import dev.se1dhe.core.handlers.inline.events.IInlineCallbackEvent;
import dev.se1dhe.core.handlers.inline.events.IInlineMessageEvent;
import dev.se1dhe.core.handlers.inline.events.InlineCallbackEvent;
import dev.se1dhe.core.handlers.inline.events.InlineMessageEvent;
import dev.se1dhe.core.util.BotUtil;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;


public abstract class AbstractInlineHandler implements ICommandHandler, IMessageHandler, ICallbackQueryHandler, ICancelHandler {
    private InlineMenu defaultMenu;

    /**
     * Calls init to register the default menu
     */
    public AbstractInlineHandler() {
        init();
    }

    /**
     * Registers the default menu
     */
    private void init() {
        final InlineContext ctx = new InlineContext();
        final InlineMenuBuilder builder = new InlineMenuBuilder(ctx);
        registerMenu(ctx, builder);
        defaultMenu = builder.build();
    }

    /**
     * Registers menu to this inline handler
     *
     * @param ctx     context
     * @param builder builder
     */
    public abstract void registerMenu(InlineContext ctx, InlineMenuBuilder builder);

    @Override
    public boolean onCallbackQuery(AbstractTelegramBot bot, Update update, CallbackQuery query) throws TelegramApiException {
        final InlineUserData userData = defaultMenu.getContext().getUserData(query.getFrom().getId());
        if (userData.getActiveMenu() == null) {
            return false;
        }

        final InlineMenu activeMenu = userData.getActiveMenu();
        for (InlineButton button : activeMenu.getButtons()) {
            if (button.getUUID().equals(query.getData())) {
                userData.setActiveButton(button);
                final IInlineCallbackEvent event = button.getOnQueryCallback();
                final AnswerCallbackQuery answerCallbackQuery = AnswerCallbackQuery.builder().callbackQueryId(query.getId()).build();
                if (event != null) {
                    if (event.onCallbackEvent(new InlineCallbackEvent(button.getContext(), button, bot, update, query))) {
                        final InlineMenu subMenu = button.getSubMenu();
                        if (subMenu != null) {
                            bot.execute(answerCallbackQuery);
                            userData.editCurrentMenu(bot, query.getMessage(), subMenu.getName() != null ? subMenu.getName() : "Sub menu", subMenu.getLayout(), subMenu);
                        }
                        return true;
                    }
                    return false;
                }

                final InlineMenu subMenu = button.getSubMenu();
                if (subMenu != null) {
                    bot.execute(answerCallbackQuery);
                    userData.editCurrentMenu(bot, query.getMessage(), subMenu.getName() != null ? subMenu.getName() : "Sub menu", subMenu.getLayout(), subMenu);
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public void onCancel(AbstractTelegramBot bot, Update update, Message message) {
        if (defaultMenu != null) {
            defaultMenu.getContext().clear(message.getFrom().getId());
        }
    }

    @Override
    public void onCommandMessage(AbstractTelegramBot bot, Update update, Message message, List<String> args) throws TelegramApiException {
        final InlineUserData userData = defaultMenu.getContext().getUserData(message.getFrom().getId());
        if (userData.getActiveMenu() == null) {
            userData.setActiveMenu(defaultMenu);
        }

        final InlineMenu activeMenu = userData.getActiveMenu();
        if (activeMenu != defaultMenu) {
            return;
        }

        userData.sendMenu(bot, message, activeMenu.getName() != null ? activeMenu.getName() : "Menu", activeMenu.getLayout(), activeMenu);
    }

    @Override
    public boolean onMessage(AbstractTelegramBot bot, Update update, Message message) throws TelegramApiException {
        if (defaultMenu == null) {
            return false;
        }

        final InlineUserData userData = defaultMenu.getContext().getUserData(message.getFrom().getId());
        final InlineButton activeButton = userData.getActiveButton();
        if (activeButton == null) {
            return false;
        }

        final IInlineMessageEvent event = activeButton.getInputMessage();
        if (event != null) {
            try {
                return event.onCallbackEvent(new InlineMessageEvent(defaultMenu.getContext(), activeButton, bot, update, message));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    /**
     * Creates generic default close button
     *
     * @param context the context
     * @return new inline button
     */
    public InlineButton defaultClose(InlineContext context) {
        //@formatter:off
		return new InlineButtonBuilder(context)
				.name("Close")
				.forceOnNewRow()
				.onQueryCallback(this::handleClose)
				.build();
		//@formatter:on
    }

    /**
     * Creates generic default back button
     *
     * @param context the context
     * @return new inline button
     */
    public InlineButton defaultBack(InlineContext context) {
        //@formatter:off
		return new InlineButtonBuilder(context)
				.name("Back")
				.forceOnNewRow()
				.onQueryCallback(this::handleBack)
				.build();
		//@formatter:on
    }

    /**
     * Creates default back button
     *
     * @param context    the context
     * @param targetMenu the target menu
     * @return new inline button
     */
    public InlineButton defaultBack(InlineContext context, InlineMenu targetMenu) {
        //@formatter:off
		return new InlineButtonBuilder(context)
				.name("Back")
				.forceOnNewRow()
				.onQueryCallback(event ->
				{
					final InlineUserData userData = event.getContext().getUserData(event.getQuery().getFrom().getId());
					userData.editCurrentMenu(event.getTelegramClient(), event.getQuery().getMessage(), defaultMenu.getName(), targetMenu.getLayout(), targetMenu);
					return true;
				})
				.build();
		//@formatter:on
    }

    /**
     * Handles close button
     *
     * @param event the callback event
     * @return true
     * @throws TelegramApiException in case of error
     */
    public boolean handleClose(InlineCallbackEvent event) throws TelegramApiException {
        event.getContext().clear(event.getQuery().getFrom().getId());
        if (event.getQuery().getMessage() instanceof Message msg) {
            BotUtil.editMessage(event.getTelegramClient(), msg, String.format("Menu closed, type in %s  to open the menu again.", getCommand()), false, null);
        }
        return true;
    }

    /**
     * Handles back button
     *
     * @param event the callback event
     * @return true
     * @throws TelegramApiException in case of error
     */
    public boolean handleBack(InlineCallbackEvent event) throws TelegramApiException {
        final InlineUserData userData = event.getContext().getUserData(event.getQuery().getFrom().getId());
        final InlineMenu targetMenu = userData.getActiveMenu().getParentMenu() != null ? userData.getActiveMenu().getParentMenu() : defaultMenu;
        userData.editCurrentMenu(event.getTelegramClient(), event.getQuery().getMessage(), defaultMenu.getName(), targetMenu.getLayout(), targetMenu);
        return true;
    }

    /**
     * @return the default menu
     */
    public InlineMenu getDefaultMenu() {
        return defaultMenu;
    }

    /**
     * Sets the default menu
     *
     * @param defaultMenu the default menu
     */
    public void setDefaultMenu(InlineMenu defaultMenu) {
        this.defaultMenu = Objects.requireNonNull(defaultMenu, "Default menu cannot be null!");
    }
}
