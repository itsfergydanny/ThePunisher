package com.dnyferguson.thepunisher.interfaces;

import java.sql.Timestamp;

public interface UserBannedCallback {
    public void denyLogin(String punisher, String reason);
    public void denyLogin(String punisher, String reason, Timestamp until);
}
