package com.shop.service;

import com.shop.dao.ActivityLogDao;
import com.shop.model.ActivityLog;
import java.util.List;
import java.time.LocalDateTime;

public class ActivityLogService {
    private final ActivityLogDao logDao;

    public ActivityLogService() {
        this.logDao = new ActivityLogDao();
    }

    public List<ActivityLog> getAllLogs() {
        return logDao.getAllLogs();
    }

    public void logAction(String action, String entity, Integer entityId, String detail) {
        ActivityLog logEntry = new ActivityLog(action, entity, entityId, detail, LocalDateTime.now());
        logDao.insertLog(logEntry);
    }
}
