package com.jy.eventbusshample.eventbus;

import android.os.Handler;
import android.os.Looper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 1. object -> 对应多个订阅方法 —> 每个订阅方法对应一个eventType(每个eventType存在于多个订阅方法中，但是每个订阅方法只可能对应一个eventType)
 *
 * 2.将
 * mSubscriptionsByEventType：key eventType, value List<Subscription> （Subscription 包装了Object 和 SubscriberMethod）(这个因为一个eventType可能对应多个订阅者和订阅方法)
 *
 * 3.根据object -> 找到eventType -> 根据eventType -> 从mSubscriptionsByEventType找到对应的类型集合 ->
 * mTypesBySubscriber
 */
public class EventBus {
    //根据订阅方法参数类型 将订阅订阅者和订阅方法保存下来
    private Map<Class<?>, CopyOnWriteArrayList<Subscription>> mSubscriptionsByEventType;
    //根据订阅者 将订阅方法参数类型存储起来
    private Map<Object, List<Class<?>>> mTypesBySubscriber;
    static volatile EventBus defaultInstance;

    private EventBus() {
        mSubscriptionsByEventType = new HashMap<>();
        mTypesBySubscriber = new HashMap<>();
    }
    /**
     * 单例
     * @return
     */
    public static EventBus getDefault() {
        if (defaultInstance == null) {
            synchronized (EventBus.class) {
                if (defaultInstance == null) {
                    defaultInstance = new EventBus();
                }
            }
        }
        return defaultInstance;
    }

    /**
     * 注册EventBus
     */
    public void register(Object object) {
        //根据对象获取所有有Subscriber注解的方法
        List<SubscriberMethod> subscriberMethods = new ArrayList<>();
        //1.获取所有的方法
        Method[] methods = object.getClass().getMethods();
        for (Method method : methods) {
            //2.获取所有的有Subscribe的注解
            Subscribe subscribe = method.getAnnotation(Subscribe.class);
            if (subscribe != null) {
                //将这个方法，方法的注解参数，方法的参数封装起来
                //获取方法的参数的返回值
                Class<?>[] parameterTypes = method.getParameterTypes();
                SubscriberMethod subscriberMethod = new SubscriberMethod(method,
                        parameterTypes[0], subscribe.threadMode(), subscribe.priority(), subscribe.sticky());
                subscriberMethods.add(subscriberMethod);
            }
        }

        //2.根据方法注解的事件类型， 将方法和订阅者存放起来
        for (SubscriberMethod subscriberMethod : subscriberMethods) {
            subscriber(object, subscriberMethod);
        }
    }

    /**
     *
     * @param object 订阅者
     * @param subscriberMethod 订阅方法
     */
    private void subscriber(Object object, SubscriberMethod subscriberMethod) {
        //获取订阅方法的注解类型
        Class<?> eventType = subscriberMethod.mEventType;
        CopyOnWriteArrayList<Subscription> subscriptions = mSubscriptionsByEventType.get(eventType);
        if (subscriptions == null) {
            subscriptions = new CopyOnWriteArrayList<>();
            mSubscriptionsByEventType.put(eventType, subscriptions);
        }

        //检查优先级

        //将订阅者和订阅方法进行组合成Subscription 存放到组合里
        Subscription subscription = new Subscription(object, subscriberMethod);
        subscriptions.add(subscription);

        //eventType 按照订阅者存放起来
        List<Class<?>> eventTypes = mTypesBySubscriber.get(object);
        if (eventTypes == null) {
            eventTypes = new ArrayList<>();
            mTypesBySubscriber.put(object, eventTypes);
        }

        if (!eventTypes.contains(eventType)) {
            eventTypes.add(eventType);
        }
    }

    /**
     * 取消注册EventBus
     */
    public void unRegister(Object object) {
        List<Class<?>> eventTypes = mTypesBySubscriber.get(object);
        if (eventTypes != null) {
            for (Class<?> eventType : eventTypes) {
                removeObject(object, eventType);
            }
        }
    }

    private void removeObject(Object object, Class<?> eventType) {
        List<Subscription> subscriptions = mSubscriptionsByEventType.get(eventType);
        if (subscriptions != null) {
            //进行删除
            int size = subscriptions.size();
            for (int i = 0; i < size; i++) {
                //获取到订阅者
                Subscription subscription = subscriptions.get(i);
                if (subscription.subscriber == object) {
                    subscriptions.remove(i);
                    i--;
                    size--;
                }
            }
        }
    }

    /**
     * 发布消息
     * @param event 事件类型
     */
    public void post(Object event) {
        //1.eventType
        Class<?> eventType = event.getClass();
        //从集合中获取这个事件的 subscription
        List<Subscription> subscriptions = mSubscriptionsByEventType.get(eventType);
        if (subscriptions != null) {
            for (Subscription subscription : subscriptions) {
                executeMethod(subscription, event);
            }
        }
    }

    /**
     *
     * @param subscription 订阅者 和 订阅方法
     * @param event 事件类型
     */
    private void executeMethod(final Subscription subscription, final Object event) {
        ThreadMode threadMode = subscription.subscriberMethod.mThreadMode;
        boolean isMainThread = Looper.getMainLooper() == Looper.myLooper();
        switch (threadMode) {
            case POSTING:
                invokeMethod(subscription, event);
                break;
            case MAIN:
                if (isMainThread) {
                    invokeMethod(subscription, event);
                } else {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                           invokeMethod(subscription, event);
                        }
                    });
                }
                break;
            case ASYNC:
                AsyncPoster.enqueue(subscription, event);
                break;
            case BACKGROUND:
                if (!isMainThread) {
                    AsyncPoster.enqueue(subscription,event);
                } else {
                    invokeMethod(subscription, event);
                }
                break;
        }
    }


    private void invokeMethod(Subscription subscription, Object event) {
        try {
            subscription.subscriberMethod.mMethod.invoke(subscription.subscriber, event);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
