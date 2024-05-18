package org.example;

public enum Status {
    DISCONNECTED("connect", false, true, true),
    CONNECTING("connecting...", true, true, false),
    CONNECTED("disconnect", true, true, false),
    DISCONNECTING("disconnecting...", false, false, false);

    final String buttonText;
    final boolean buttonSelected, buttonEnabled;
    final boolean allowModifyData;

    Status(String buttonText, boolean buttonSelected, boolean buttonEnabled, boolean allowModifyData) {
        this.buttonText = buttonText;
        this.buttonSelected = buttonSelected;
        this.buttonEnabled = buttonEnabled;
        this.allowModifyData = allowModifyData;
    }
}
