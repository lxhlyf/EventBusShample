package com.jy.eventbusshample.eventbus;

import java.lang.reflect.Method;

public class SubscriberMethod {

    final Method mMethod;
    final Class<?> mEventType;
    final ThreadMode mThreadMode;
    final int mPriority;
    final boolean mSticky;

    /** Used for efficient comparison */
    String methodString;

    public SubscriberMethod(Method method, Class<?> parameterType, ThreadMode threadMode, int priority, boolean sticky) {
        this.mMethod = method;
        this.mEventType = parameterType;
        this.mThreadMode = threadMode;
        this.mPriority = priority;
        this.mSticky = sticky;
    }


}
