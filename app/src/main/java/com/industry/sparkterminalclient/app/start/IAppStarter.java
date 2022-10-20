package com.industry.sparkterminalclient.app.start;

import bolts.Task;

/**
 * Created by Two on 2/6/2015.
 */
public interface IAppStarter {
    public Task<Void> startApp(final String packageName);
}
