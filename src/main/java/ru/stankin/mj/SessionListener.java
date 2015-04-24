package ru.stankin.mj;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

@WebListener
public class SessionListener implements HttpSessionListener {

    private static final Logger logger = LogManager.getLogger(StudentsContainer.class);

    public void sessionCreated(HttpSessionEvent sessionEvent) {
        logger.debug("Session Created:: ID={}", sessionEvent.getSession().getId());
    }

    public void sessionDestroyed(HttpSessionEvent sessionEvent) {
        logger.debug("Session Destroyed:: ID={}", sessionEvent.getSession().getId());
    }

}