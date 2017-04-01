package xyz.klinker.messenger.shared.service.jobs;

import xyz.klinker.messenger.shared.service.NotificationService;

public class RepeatNotificationJob extends NotificationService {

    public static final int REQUEST_CODE = 1224;
    // we will simply check an instance of this service to know whether or not to re-notify.
}
