package com.dabakovich.service.utils;

import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dabak on 10.09.2017, 20:28.
 */
public class KeyboardBuilder {

    public static InlineKeyboardBuilder createInlineKeyboard() {
        return new InlineKeyboardBuilder();
    }

    public static ReplyKeyboardBuilder createReplyKeyboard() {
        return new ReplyKeyboardBuilder();
    }

    public static class InlineKeyboardBuilder {

        private List<List<InlineKeyboardButton>> buttonsGrid;
        private List<InlineKeyboardButton> buttonsRow;

        public InlineKeyboardMarkup build() {
            if (buttonsGrid == null) buttonsGrid = new ArrayList<>();
            if (buttonsRow != null) buttonsGrid.add(buttonsRow);

            return new InlineKeyboardMarkup().setKeyboard(buttonsGrid);
        }

        public InlineKeyboardBuilder addRow() {
            if (buttonsGrid == null) buttonsGrid = new ArrayList<>();
            if (buttonsRow != null) {
                buttonsGrid.add(buttonsRow);
                buttonsRow = null;
            }

            return this;
        }

        public InlineKeyboardBuilder addCallbackButton(String text, String data) {
            InlineKeyboardButton button = new InlineKeyboardButton()
                    .setText(text)
                    .setCallbackData(data);
            if (buttonsRow == null) buttonsRow = new ArrayList<>();
            buttonsRow.add(button);

            return this;
        }

        public InlineKeyboardBuilder addUrlButton(String text, String url) {
            InlineKeyboardButton button = new InlineKeyboardButton()
                    .setText(text)
                    .setUrl(url);
            if (buttonsRow == null) buttonsRow = new ArrayList<>();
            buttonsRow.add(button);

            return this;
        }
    }

    public static class ReplyKeyboardBuilder {

        private List<KeyboardRow> buttonsGrid;
        private boolean resizeKeyboard = false;
        private boolean oneTimeKeyboard = false;
        private boolean selective = false;
        private KeyboardRow buttonsRow;

        public ReplyKeyboardMarkup build() {
            if (buttonsGrid == null) buttonsGrid = new ArrayList<>();
            if (buttonsRow != null) buttonsGrid.add(buttonsRow);

            return new ReplyKeyboardMarkup()
                    .setKeyboard(buttonsGrid)
                    .setResizeKeyboard(resizeKeyboard)
                    .setOneTimeKeyboard(oneTimeKeyboard)
                    .setSelective(selective);
        }

        public ReplyKeyboardBuilder addRow() {
            if (buttonsGrid == null) buttonsGrid = new ArrayList<>();
            if (buttonsRow != null) {
                buttonsGrid.add(buttonsRow);
                buttonsRow = null;
            }

            return this;
        }

        public ReplyKeyboardBuilder addButton(String text) {
            KeyboardButton button = new KeyboardButton(text);
            if (buttonsRow == null) buttonsRow = new KeyboardRow();
            buttonsRow.add(button);
            return this;
        }

        public ReplyKeyboardBuilder addButton(String text,
                                              boolean requestContact,
                                              boolean requestLocation) {
            KeyboardButton button = new KeyboardButton(text);
            button.setRequestContact(requestContact);
            button.setRequestLocation(requestLocation);
            if (buttonsRow == null) buttonsRow = new KeyboardRow();
            buttonsRow.add(button);
            return this;
        }

        public ReplyKeyboardBuilder setResizeKeyboard(boolean resizeKeyboard) {
            this.resizeKeyboard = resizeKeyboard;
            return this;
        }

        public ReplyKeyboardBuilder setOneTimeKeyboard(boolean oneTimeKeyboard) {
            this.oneTimeKeyboard = oneTimeKeyboard;
            return this;
        }

        public ReplyKeyboardBuilder setSelective(boolean selective) {
            this.selective = selective;
            return this;
        }
    }
}
