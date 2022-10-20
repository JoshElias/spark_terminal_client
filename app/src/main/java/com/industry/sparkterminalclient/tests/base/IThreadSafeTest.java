package com.industry.sparkterminalclient.tests.base;

/**
 * Created by Two on 12/4/2014.
 */
public interface IThreadSafeTest {
    public void testExecutionSingle() throws Exception;
    public void testExecutionSeries() throws Exception;
    public void testExecutionBulk() throws Exception;
}
