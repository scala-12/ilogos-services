package com.ilogos.user.common.model;

import java.util.List;

import com.ilogos.user.user.emailHistory.EmailHistory;

public interface IWithEmailHistory {
    List<EmailHistory> getEmailHistory();
}
