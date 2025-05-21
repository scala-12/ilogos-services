package com.ilogos.user.common.model;

import java.util.List;

import com.ilogos.user.user.usernameHistory.UsernameHistory;

public interface IWithUsernameHistory {
    List<UsernameHistory> getUsernameHistory();
}
